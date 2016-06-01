package net.krzysieklachor

import java.lang.Math.{ PI, abs, exp, log, sqrt }

import net.krzysieklachor.Glicko2._

sealed trait Result { def value: Double }
case object Loss extends Result { val value = 0.0 }
case object Draw extends Result { val value = 0.5 }
case object Win extends Result { val value = 1.0 }

object Glicko2 {

  final val Glicko2Conversion = 173.7178

  /*
   * Should it be configurable per competition? Probably not. Should set to 6 months?
   * The Glicko-2 system works
   * best when the number of games in a rating period is moderate to large, say an average of at
   * least 10-15 games per player in a rating period
   */

  /*
   * 0.3 to 1.2. Smaller values prevent volatility from changing large amounts, which in turn
   * prevents enormous changes in ratings based on very improbable results.
   * The system should be tested to decide which value results in greatest predictive
   * accuracy.
   */
  final val Tau = 0.5

  final val UnratedPlayerRatingG1 = 1500.0
  final val UnratedPlayerRatingDeviationG1 = 350.0
  final val UnratedPlayerVolatilityG1 = 0.06

  /* helper function */
  def g(ratingDeviation: Double): Double = 1.0 / sqrt(1.0 + 3 * pow2(ratingDeviation) / pow2(PI))

  /* helper function */
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

  override def toString(): String =
    "rating: %1.0f, rd: %1.2f, volatility: %1.6f".format(rating, ratingDeviation, ratingVolatility)
}

case class Glicko2(
    rating: Double = (UnratedPlayerRatingG1 - 1500) / Glicko2Conversion,
    ratingDeviation: Double = UnratedPlayerRatingDeviationG1 / Glicko2Conversion,
    ratingVolatility: Double = UnratedPlayerVolatilityG1) {

  def toGlicko1() = Glicko1(
    rating * Glicko2.Glicko2Conversion + 1500,
    ratingDeviation * Glicko2.Glicko2Conversion,
    ratingVolatility
  )

  /**
   * Computes the quantity v. This is the estimated variance of the team’s/player’s
   * rating based only on game outcomes
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

    // step5 - calculate new volatility
    val convergenceTolerance = 0.000001 // convergence tolerance, epsilon

    /*
     * Based on the so-called “Illinois algorithm,” a variant of the regula falsi (false position) procedure.
     * The algorithm takes advantage of the knowledge that the desired value of σ can be sandwiched
     * at the start of the algorithm by the initial choices of A and B
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

    /* step6 - update rating deviation to new pre-rating period value (decay RD) */
    def preRatingPeriodRatingDeviation(): Double = {
      sqrt(pow2(this.ratingDeviation) + pow2(newVolatility))
    }

    // step7a - calculate new RD
    def newRD(): Double = {
      1.0 /
        sqrt(1.0 / pow2(preRatingPeriodRatingDeviation) + 1.0 / estimatedVariance(opponents))
    }

    // step7b - calculate new rating
    def newRating: Double = {

      val sum = opponents.foldLeft(0.0)((acc, resultWithOpponent) => {
        val (opponentGlicko2, result) = resultWithOpponent

        acc +
          g(opponentGlicko2.ratingDeviation) *
          (result.value - E(rating, opponentGlicko2.rating, opponentGlicko2.ratingDeviation))
      })
      rating + pow2(newRD) * sum
    }

    Glicko2(newRating, newRD, newVolatility)
  }

  /**
   * If a player does not compete during the rating period, then only Step 6 applies. In
   * this case, the player’s rating and volatility parameters remain the same, but the rating deviation increases
   * according to
   *
   *  @param n  number of rating periods to decay
   *            //todo test
   */
  def decayRatingDeviation(n: Int): Glicko2 = {

    val decayedRatingDeviation = (0 until n).foldLeft(ratingDeviation)((a, b) =>
      sqrt(pow2(a) + pow2(ratingVolatility))
    )

    Glicko2(rating, decayedRatingDeviation, ratingVolatility)
  }

}