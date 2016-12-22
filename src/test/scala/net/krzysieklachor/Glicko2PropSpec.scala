package net.krzysieklachor

import org.scalacheck._
import org.scalacheck.Prop.forAll

object Glicko2PropSpec extends Properties("String"){

val smallInteger = Gen.choose(0,100)

  val rating = Gen.choose(500.0,2500.0)
  val ratingDeviation = Gen.choose(0.0,500.0)
  val ratingVolatility = Gen.choose(0.0, 0.1)

  property("convertBetween1And2") = forAll(rating, ratingDeviation, ratingVolatility) { (r, rd, rv) =>
    Glicko1(r, rd, rv) == Glicko1(r, rd, rv).toGlicko2().toGlicko1()
  }
}