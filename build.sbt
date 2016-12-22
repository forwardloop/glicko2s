name := """glicko2"""

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2-core" % "3.8.5" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.4" % "test"
)