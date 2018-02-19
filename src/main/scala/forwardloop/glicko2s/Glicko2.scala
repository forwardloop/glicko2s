package forwardloop.glicko2s

import java.lang.Math.{ PI, abs, exp, log, sqrt }

sealed trait Result { def value: Double }
case object Loss extends Result { val value = 0.0 }
case object Draw extends Result { val value = 0.5 }
case object Win extends Result { val value = 1.0 }

object Glicko2 {

  /** Conversion between Glicko-1 and Glicko-2 systems */
  final val Glicko2Conversion = 173.7178

  /**
   * Set between 0.3 and 1.2. Smaller values prevent volatility from changing large amounts,
   * which in turn prevents enormous changes in ratings based on very improbable results. The system should be
   * tested to decide which value results in greatest predictive accuracy.
   */
  final val Tau = 0.5

  final val NewPlayerRatingG1 = 1500.0
  final val NewPlayerRatingDeviationG1 = 350.0
  final val NewPlayerVolatilityG1 = 0.06

  def g(ratingDeviation: Double): Double = 1.0 / sqrt(1.0 + 3 * pow2(ratingDeviation) / pow2(PI))

  def E(rating: Double, opponentRating: Double, opponentRatingDeviation: Double): Double =
    1.0 / (1.0 + exp(-g(opponentRatingDeviation) * (rating - opponentRating)))

  private def pow2(op: Double): Double = op * op
}

case class Glicko1(
    rating: Double,
    ratingDeviation: Double,
    ratingVolatility: Double) {

  def toGlicko2() = Glicko2(
    (rating - 1500) / Glicko2.Glicko2Conversion,
    ratingDeviation / Glicko2.Glicko2Conversion,
    ratingVolatility
  )

  override def toString() =
    "rating: %1.0f, deviation: %1.2f, volatility: %1.6f".format(rating, ratingDeviation, ratingVolatility)
}

case class Glicko2(
    rating: Double = (Glicko2.NewPlayerRatingG1 - 1500) / Glicko2.Glicko2Conversion,
    ratingDeviation: Double = Glicko2.NewPlayerRatingDeviationG1 / Glicko2.Glicko2Conversion,
    ratingVolatility: Double = Glicko2.NewPlayerVolatilityG1) {

  import Glicko2._

  def toGlicko1() = Glicko1(
    rating * Glicko2.Glicko2Conversion + 1500,
    ratingDeviation * Glicko2.Glicko2Conversion,
    ratingVolatility
  )

  /**
   * Computes estimated variance of the player’s rating based on game outcomes
   */
  def estimatedVariance(opponents: Seq[(Glicko2, Result)]): Double = {
    val sum = opponents.foldLeft(0.0) { (acc, resultWithOpponent) =>
      val (opponentGlicko2, result) = resultWithOpponent

      acc + {
        pow2(g(opponentGlicko2.ratingDeviation)) *
          E(rating, opponentGlicko2.rating, opponentGlicko2.ratingDeviation) *
          (1 - E(rating, opponentGlicko2.rating, opponentGlicko2.ratingDeviation))
      }
    }
    1.0 / sum
  }

  /**
   * Computes the quantity ∆, the estimated improvement in rating by comparing the
   * pre-period rating to the performance rating based only on game outcomes
   */
  def estimatedImprovement(opponents: Seq[(Glicko2, Result)]) = {
    val sum = opponents.foldLeft(0.0) { (acc, resultWithOpponent) =>
      val (opponentGlicko2, result) = resultWithOpponent
      acc + g(opponentGlicko2.ratingDeviation) *
        (result.value - E(rating, opponentGlicko2.rating, opponentGlicko2.ratingDeviation))
    }
    estimatedVariance(opponents) * sum
  }

  def calculateNewRating(opponents: Seq[(Glicko2, Result)]): Glicko2 = {

    val convergenceTolerance = 0.000001 // convergence tolerance, epsilon

    /**
     * Based on the so-called “Illinois algorithm”, the algorithm takes advantage of the knowledge that
     * the desired value of σ can be sandwiched at the start of the algorithm by the initial choices of A and B
     */
    def newVolatility(): Double = {

      def a(): Double = log(pow2(ratingVolatility))

      def f(x: Double): Double = {
        (exp(x) * (pow2(estimatedImprovement(opponents)) - pow2(ratingDeviation) - estimatedVariance(opponents) - exp(x))) /
          (2.0 * pow2(pow2(ratingDeviation) + estimatedVariance(opponents) + exp(x))) -
          (x - a) / pow2(Glicko2.Tau)
      }

      var A: Double = a
      var B: Double =
        if (pow2(estimatedImprovement(opponents)) > pow2(ratingDeviation)) {
          log(pow2(estimatedImprovement(opponents)) - pow2(ratingDeviation) - estimatedVariance(opponents))
        } else {
          var k = 1
          while (f(a - k * sqrt(pow2(Glicko2.Tau))) < 0) {
            k += 1
          }
          a - k * sqrt(pow2(Glicko2.Tau))
        }

      var fA = f(A)
      var fB = f(B)
      while (abs(B - A) > convergenceTolerance) {
        val C: Double = A + (A - B) * fA / (fB - fA)
        val fC = f(C)
        if (fC * fB < 0) {
          A = B
          fA = fB
        } else {
          fA = fA / 2
        }
        B = C
        fB = fC
      }

      exp(A / 2)
    }

    /** Update rating deviation to new pre-rating period value (decay rating deviation) */
    def preRatingPeriodRatingDeviation(): Double =
      sqrt(pow2(this.ratingDeviation) + pow2(newVolatility))

    def newRatingDeviation(): Double =
      1.0 / sqrt(1.0 / pow2(preRatingPeriodRatingDeviation) + 1.0 / estimatedVariance(opponents))

    def newRating: Double = {

      val sum = opponents.foldLeft(0.0)((acc, resultWithOpponent) => {
        val (opponentGlicko2, result) = resultWithOpponent

        acc +
          g(opponentGlicko2.ratingDeviation) *
          (result.value - E(rating, opponentGlicko2.rating, opponentGlicko2.ratingDeviation))
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
   *  @param n number of rating periods to decay
   */
  def decayRatingDeviation(n: Int): Glicko2 = {

    val decayedRatingDeviation = (0 until n).foldLeft(ratingDeviation) { (a, b) =>
      sqrt(pow2(a) + pow2(ratingVolatility))
    }

    Glicko2(rating, decayedRatingDeviation, ratingVolatility)
  }
}