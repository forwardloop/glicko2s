package forwardloop.glicko2s

import org.scalacheck.{ Gen, Properties }
import org.scalacheck.Prop.forAll

object Glicko2PropSpec extends Properties("Glicko2") {

  val glicko1Gen: Gen[Glicko1] = for {
    rating <- Gen.choose(500.0, 2500.0)
    ratingDeviation <- Gen.choose(0.0, 500.0)
    ratingVolatility <- Gen.choose(0.0, 0.1)
  } yield (Glicko1(rating, ratingDeviation, ratingVolatility))

  val idlePeriodsGen = Gen.choose(1, 1000)

  property("rating deviation increases for player who has not competed") = forAll(glicko1Gen, idlePeriodsGen) {
    (g1, ip) =>
      val g2 = g1.toGlicko2()
      val idleG2 = g2.ratingDeviationForIdle(ip)
      g2.rating == idleG2.rating &&
        g2.ratingVolatility == idleG2.ratingVolatility &&
        g2.ratingDeviation < idleG2.ratingDeviation
  }

  property("a loss to equal rated opponent decreases player's rating") = forAll(glicko1Gen, idlePeriodsGen) {
    (g1, ip) =>
      val g2 = g1.toGlicko2()
      val opponent = g1.copy().toGlicko2()
      val r = g2.calculateNewRating(List((opponent, Loss)))
      r.rating < g2.rating
  }

  property("a win with equal rated opponent increases player's rating") = forAll(glicko1Gen, idlePeriodsGen) {
    (g1, ip) =>
      val g2 = g1.toGlicko2()
      val opponent = g1.copy().toGlicko2()
      val r = g2.calculateNewRating(List((opponent, Win)))
      r.rating > g2.rating
  }
}