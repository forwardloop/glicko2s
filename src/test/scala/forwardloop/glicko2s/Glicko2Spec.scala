package forwardloop.glicko2s

import org.specs2.mutable.Specification
import scala.math.BigDecimal.RoundingMode

class Glicko2Spec extends Specification {

  final val player = g2(1500.0, 200.0, 0.06)
  final val opponent1 = g2(1400.0, 30.0, 0.06)
  final val opponent2 = g2(1550.0, 100.0, 0.06)
  final val opponent3 = g2(1700.0, 300.0, 0.06)

  final val opponents = Seq(opponent1, opponent2, opponent3)

  final val results = Seq(
    (opponent1, Win),
    (opponent2, Loss),
    (opponent3, Loss)
  )

  def g2(ratingG1: Double, ratingDeviationG1: Double, volatilityG1: Double) =
    Glicko1(ratingG1, ratingDeviationG1, volatilityG1).toGlicko2()

  s"In Glicko-2 scale, player ($player) rating and RD" should {
    "be 0 and 1.1513" in {
      player.rating === 0.0
      rd4(player.ratingDeviation) === 1.1513
    }
  }

  "In Glicko-2 scale, opponent1 rating and RD" should {
    "be −0.5756 and 0.1727" in {
      rd4(opponent1.rating) === -0.5756
      rd4(opponent1.ratingDeviation) === 0.1727
    }
  }

  "In Glicko-2 scale, opponent2 rating and RD" should {
    "be 0.2878 and 0.5756" in {
      rd4(opponent2.rating) === 0.2878
      rd4(opponent2.ratingDeviation) === 0.5756
    }
  }

  "In Glicko-2 scale, opponent3 rating and RD" should {
    "be 1.1513 and 1.7269" in {
      rd4(opponent3.rating) === 1.1513
      rd4(opponent3.ratingDeviation) === 1.7269
    }
  }

  "g() helper function" should {
    "return expected values for opponents 1, 2 and 3" in {
      opponents
        .map(opp => Glicko2.g(opp.ratingDeviation))
        .map(rd4(_)) === Seq(0.9955, 0.9531, 0.7242)
    }
  }

  "E() helper function" should {
    "return expected values for opponents 1, 2 and 3" in {
      opponents
        .map(opp => Glicko2.E(player.rating, opp.rating, opp.ratingDeviation))
        .map(rd3(_)) === Seq(0.639, 0.432, 0.303)
    }
  }

  "Computing the quantity v (the estimated variance)" should {
    "return 1.7785" in {
      rd4(player.estimatedVariance(results)) === 1.7790
    }
  }

  "Computing the quantity ∆ (the estimated improvement in rating)" should {
    "return −0.4834" in {
      val expected = -0.4839
      val result = player.estimatedImprovement(results)
      rd4(result) === expected
    }
  }

  "Calculate a new player's rating after three matches with three players" should {
    "give expected result" in {
      g2(1500.0, 200.0, 0.06)
        .calculateNewRating(results)
        .toGlicko1().toString must be equalTo "rating: 1464, deviation: 151.52, volatility: 0.059996"
    }
  }

  "Compute rating deviation for idle player" should {
    "not increase deviation for 0 periods" in {
      rd3(
        player
          .ratingDeviationForIdle(0)
          .toGlicko1()
          .ratingDeviation
      ) === 200.0
    }
    "increase deviation for 1 period" in {
      rd3(
        player
          .ratingDeviationForIdle(1)
          .toGlicko1()
          .ratingDeviation
      ) === 200.271
    }
    "increase deviation for 2 periods" in {
      rd3(
        player
          .ratingDeviationForIdle(2)
          .toGlicko1()
          .ratingDeviation
      ) === 200.542
    }
  }

  private def round(d: Double, scale: Int) = BigDecimal.valueOf(d).setScale(scale, RoundingMode.HALF_UP)
  private def rd3(d: Double) = round(d, 3)
  private def rd4(d: Double) = round(d, 4)
}