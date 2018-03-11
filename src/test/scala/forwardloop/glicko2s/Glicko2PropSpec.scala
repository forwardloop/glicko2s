package forwardloop.glicko2s

import org.scalacheck.{ Gen, Properties }
import org.scalacheck.Prop.forAll

object Glicko2PropSpec extends Properties("Glicko2") {

  def glicko1Gen: Gen[Glicko1] = for {
    rating <- Gen.choose(0.0, 3000.0)
    ratingDeviation <- Gen.choose(0.0, 1000.0)
    ratingVolatility <- Gen.choose(0.0, 0.1)
  } yield Glicko1(rating, ratingDeviation, ratingVolatility)

  val idlePeriodsGen = Gen.choose(1, 1000)

  property("rating deviation increases for player who has not competed") = forAll(glicko1Gen, idlePeriodsGen) {
    (g1, ip) =>
      val g2 = g1.toGlicko2()
      val idleG2 = g2.ratingDeviationForIdle(ip)
      g2.rating == idleG2.rating &&
        g2.ratingVolatility == idleG2.ratingVolatility &&
        g2.ratingDeviation < idleG2.ratingDeviation
  }

  property("a loss to any opponent decreases player's rating") = forAll(glicko1Gen, glicko1Gen) {
    (playerG1, opponentG1) =>
      val playerG2 = playerG1.toGlicko2()
      val opponentG2 = opponentG1.toGlicko2()
      val r = playerG2.calculateNewRating(Seq((opponentG2, Loss)))
      r.rating < playerG2.rating
  }

  property("a win with any opponent increases player's rating") = forAll(glicko1Gen, glicko1Gen) {
    (playerG1, opponentG1) =>
      val playerG2 = playerG1.toGlicko2()
      val opponentG2 = opponentG1.toGlicko2()
      val r = playerG2.calculateNewRating(Seq((opponentG2, Win)))
      r.rating > playerG2.rating
  }

  property("a draw with equal rated opponent does not change player's rating") = forAll(glicko1Gen) {
    g1 =>
      val g2 = g1.toGlicko2()
      val opponentG2 = g1.copy().toGlicko2()
      val r = g2.calculateNewRating(Seq((opponentG2, Draw)))
      r.rating == g2.rating
  }

  property("a draw with lower rated opponent decreases player's rating") = forAll(glicko1Gen) {
    g1 =>
      val g2 = g1.toGlicko2()
      val opponentG2 = g1.copy(rating = g1.rating - 1).toGlicko2()
      val r = g2.calculateNewRating(Seq((opponentG2, Draw)))
      r.rating < g2.rating
  }

  property("a draw with higher rated opponent increases player's rating") = forAll(glicko1Gen) {
    g1 =>
      val g2 = g1.toGlicko2()
      val opponentG2 = g1.copy(rating = g1.rating + 1).toGlicko2()
      val r = g2.calculateNewRating(Seq((opponentG2, Draw)))
      r.rating > g2.rating
  }
}