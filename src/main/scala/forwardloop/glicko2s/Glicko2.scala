package forwardloop.glicko2s

import java.lang.Math.{ PI, abs, exp, log, sqrt, pow }

trait EloResult { def value: Double }
case object Loss extends EloResult { val value = 0.0 }
case object Draw extends EloResult { val value = 0.5 }
case object Win extends EloResult { val value = 1.0 }

object Glicko2 {

  /** Conversion between Glicko-1 and Glicko-2 systems */
  final val Glicko2Conversion = 173.7178

  /**
   * Set between 0.3 and 1.2. Smaller values prevent volatility from changing large amounts,
   * which in turn prevents enormous changes in ratings based on very improbable results. The system should be
   * tested to decide which value results in greatest predictive accuracy
   */
  final val Tau = 0.5

  /**
   * Defaults assigned to new/unrelated players. `NewPlayerRatingG1` is an average club player rating
   */
  final val NewPlayerRatingG1 = 1500.0
  final val NewPlayerRatingDeviationG1 = 350.0
  final val NewPlayerVolatilityG1 = 0.06

  /* Helper functions */

  def g(ratingDeviation: Double): Double = 1.0 / sqrt(1.0 + 3 * pow2(ratingDeviation) / pow2(PI))

  def E(rating: Double, opponentRating: Double, opponentRatingDeviation: Double): Double =
    1.0 / (1.0 + exp(-g(opponentRatingDeviation) * (rating - opponentRating)))

  private def pow2(op: Double) = pow(op, 2)
}

import Glicko2._

/**
 * The rating scale for Glicko-2 is different from that of the original Glicko-1 system.
 * Ratings are normally presented in ELO/Glicko-1 scale, but the formulas operate to the Glicko-2 scale hence
 * conversions are necessary.
 *
 * @param rating a player's rating in Glicko1 scale. `NewPlayerRatingG1 = 1500.0` is the default rating often
 *               assigned to new players not previously rated
 * @param ratingDeviation confidence in accuracy of a player's rating
 * @param ratingVolatility degree of expected fluctuation in a player's rating. High when a player has erratic
 *                         performances, low if performs at a consistent level
 */
case class Glicko1(
    rating: Double,
    ratingDeviation: Double,
    ratingVolatility: Double) {

  def toGlicko2() = Glicko2(
    (rating - NewPlayerRatingG1) / Glicko2Conversion,
    ratingDeviation / Glicko2Conversion,
    ratingVolatility
  )

  override def toString() =
    "rating: %1.0f, deviation: %1.2f, volatility: %1.6f".format(rating, ratingDeviation, ratingVolatility)
}

case class Glicko2(
    rating: Double = 0.0,
    ratingDeviation: Double = Glicko2.NewPlayerRatingDeviationG1 / Glicko2.Glicko2Conversion,
    ratingVolatility: Double = Glicko2.NewPlayerVolatilityG1) {

  def toGlicko1() = Glicko1(
    rating * Glicko2Conversion + NewPlayerRatingG1,
    ratingDeviation * Glicko2Conversion,
    ratingVolatility
  )

  /**
   * Computes estimated variance of the player’s rating based on game outcomes
   */
  def estimatedVariance(implicit opponents: Seq[(Glicko2, EloResult)]): Double = {
    val sum = opponents.foldLeft(0.0) { (acc, resultWithOpponent) =>
      val (opponent, _) = resultWithOpponent

      acc + {
        pow2(g(opponent.ratingDeviation)) *
          E(rating, opponent.rating, opponent.ratingDeviation) *
          (1 - E(rating, opponent.rating, opponent.ratingDeviation))
      }
    }
    1.0 / sum
  }

  /**
   * Computes the quantity ∆, the estimated improvement in rating by comparing the
   * pre-period rating to the performance rating based only on game outcomes
   */
  def estimatedImprovement(implicit opponents: Seq[(Glicko2, EloResult)]) = {
    val sum = opponents.foldLeft(0.0) { (acc, resultWithOpponent) =>
      val (opponentGlicko2, result) = resultWithOpponent
      acc + g(opponentGlicko2.ratingDeviation) *
        (result.value - E(rating, opponentGlicko2.rating, opponentGlicko2.ratingDeviation))
    }
    estimatedVariance(opponents) * sum
  }

  def calculateNewRating(implicit opponents: Seq[(Glicko2, EloResult)]): Glicko2 = {

    val ε = 0.000001 // convergence tolerance
    val v = estimatedVariance
    val ∆ = estimatedImprovement
    val φ = ratingDeviation
    val σ = ratingVolatility
    val τ = Glicko2.Tau

    /**
     * Based on the so-called “Illinois algorithm”, the algorithm takes advantage of the knowledge that
     * the desired value of σ can be sandwiched at the start of the algorithm by the initial choices of A and B
     */
    val newVolatility = {
      val a = log(pow2(σ))

      def f(x: Double): Double =
        (exp(x) * (pow2(∆) - pow2(φ) - v - exp(x))) /
          (2.0 * pow2(pow2(φ) + v + exp(x))) -
          (x - a) / pow2(τ)

      var A = a
      var B =
        if (pow2(∆) > pow2(φ) + v)
          log(pow2(∆) - pow2(φ) - v)
        else {
          var k = 1
          while (f(a - k * τ) < 0) k += 1
          a - k * τ
        }

      /* A and B chosen to bracket ln(pow2(σ'), remainder of the algorithm iteratively narrows this bracket */
      var fA = f(A)
      var fB = f(B)
      while (abs(B - A) > ε) {
        val C = A + (A - B) * fA / (fB - fA)
        val fC = f(C)
        if (fC * fB < 0) {
          A = B
          fA = fB
        } else fA = fA / 2
        B = C
        fB = fC
      }

      exp(A / 2)
    }

    val newRatingDeviation = {
      val preRatingPeriod_φ = sqrt(pow2(φ) + pow2(newVolatility))
      1.0 / sqrt(1.0 / pow2(preRatingPeriod_φ) + 1.0 / v)
    }

    val newRating = {
      val sum = opponents.foldLeft(0.0)((acc, resultWithOpponent) => {
        val (opponent, result) = resultWithOpponent

        acc +
          g(opponent.ratingDeviation) *
          (result.value - E(rating, opponent.rating, opponent.ratingDeviation))
      })
      rating + pow2(newRatingDeviation) * sum
    }

    Glicko2(newRating, newRatingDeviation, newVolatility)
  }

  /**
   * If a player does not compete during the rating period their rating and volatility parameters
   * remain the same but the rating deviation increases.
   *
   * The Glicko-2 system works best when the number of games in a rating period is moderate to large,
   * i.e. at least 10-15 games per player in a rating period.
   *
   *  @param idleRatingPeriods number of rating periods a player has not competed
   */
  def ratingDeviationForIdle(idleRatingPeriods: Int): Glicko2 = {

    val idleRatingDeviation = (0 until idleRatingPeriods).foldLeft(ratingDeviation) { (rd, _) =>
      sqrt(pow2(rd) + pow2(ratingVolatility))
    }

    Glicko2(rating, idleRatingDeviation, ratingVolatility)
  }
}
