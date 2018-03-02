package forwardloop.glicko2s

import org.scalacheck.{ Gen, Properties }
import org.scalacheck.Prop.forAll

object Glicko2PropSpec extends Properties("Glicko2") {

  val rating = Gen.choose(500.0, 2500.0)
  val ratingDeviation = Gen.choose(0.0, 500.0)
  val ratingVolatility = Gen.choose(0.0, 0.1)
  val idlePeriods = Gen.choose(1, 1000)

  property("increaseDeviationForIdle") = forAll(rating, ratingDeviation, ratingVolatility, idlePeriods) {
    (r, rd, rv, ip) =>
      val g2 = Glicko1(r, rd, rv).toGlicko2()
      val idleG2 = g2.ratingDeviationForIdle(ip)
      g2.rating == idleG2.rating &&
        g2.ratingVolatility == idleG2.ratingVolatility &&
        g2.ratingDeviation < idleG2.ratingDeviation
  }
}