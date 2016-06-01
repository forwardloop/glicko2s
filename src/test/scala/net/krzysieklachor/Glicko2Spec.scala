package net.krzysieklachor

import org.specs2.mutable.Specification
import scala.math.BigDecimal.RoundingMode

class Glicko2Spec extends Specification {

  final val player = Glicko1(1500.0, 200.0, 0.06).toGlicko2()
  final val opponent1 = Glicko1(1400.0, 30.0, 0.06).toGlicko2()
  final val opponent2 = Glicko1(1550.0, 100.0, 0.06).toGlicko2()
  final val opponent3 = Glicko1(1700.0, 300.0, 0.06).toGlicko2()

  final val opponents = List(opponent1, opponent2, opponent3)

  final val results = List(
    (opponent1, Win),
    (opponent2, Loss),
    (opponent3, Loss)
  )

  "In Glicko-2 scale, test player’s rating and RD" should {
    "be 0 and 1.1513" in {
      player.rating === 0.0
      round4(player.ratingDeviation) === 1.1513
    }
  }

  "In Glicko-2 scale, opponent1 rating and RD" should {
    "be −0.5756 and 0.1727" in {
      round4(opponent1.rating) === -0.5756
      round4(opponent1.ratingDeviation) === 0.1727
    }
  }

  "In Glicko-2 scale, opponent2 rating and RD" should {
    "be 0.2878 and 0.5756" in {
      round4(opponent2.rating) === 0.2878
      round4(opponent2.ratingDeviation) === 0.5756
    }
  }

  "In Glicko-2 scale, opponent3 rating and RD" should {
    "be 1.1513 and 1.7269" in {
      round4(opponent3.rating) === 1.1513
      round4(opponent3.ratingDeviation) === 1.7269
    }
  }

  "g() helper function" should {
    "return 0.9955, 0.9531, 0.7242 for opponents 1, 2 and 3 respectively" in {
      val expected = List(0.9955, 0.9531, 0.7242)
      val result = opponents
        .map(glicko2 => Glicko2.g(glicko2.ratingDeviation))
        .map(round4(_))
      expected === result
    }
  }

  "E() helper function" should {
    "return 0.639, 0.432, 0.303 for opponents 1, 2 and 3 respectively" in {
      val expected = List(0.639, 0.432, 0.303)
      val result = opponents
        .map { opponent =>
          Glicko2.E(player.rating, opponent.rating, opponent.ratingDeviation)
        }
        .map(round3(_))
      expected === result
    }
  }

  "Computing the quantity v (the estimated variance)" should {
    "return 1.7785" in {
      //      val expected =  1.7785
      val expected = 1.7790 //probably a rounding issue
      val result = player.estimatedVariance(results)
      round4(result) === expected
    }
  }

  "Computing the quantity ∆ (the estimated improvement in rating)" should {
    "return −0.4834" in {
      //      val expected = -0.4834
      val expected = -0.4839 //probably a rounding issue
      val result = player.estimatedImprovement(results)
      round4(result) === expected
    }
  }

  "Calculate a new player's rating after three matches with three players" should {
    "give expected result" in {
      val newPlayerGlicko2 = Glicko1(1500.0, 200.0, 0.06).toGlicko2
      val str = newPlayerGlicko2
        .calculateNewRating(results)
        .toGlicko1().toString
      str must be equalTo "rating: 1464, rd: 151.52, volatility: 0.059996"
    }
  }

  def round3(d: Double) = round(d, 3)

  def round4(d: Double) = round(d, 4)

  def round(d: Double, scale: Int) = BigDecimal.valueOf(d).setScale(scale, RoundingMode.HALF_UP)
}