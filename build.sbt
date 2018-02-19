name := """glicko2s"""

version := "0.9.2"

scalaVersion := "2.11.12"

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2-core" % "3.8.5" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.4" % "test",
  "junit" % "junit" % "4.12" % "test"
)

// POM settings for Sonatype
homepage := Some(url("https://github.com/forwardloop/glicko2s"))
scmInfo := Some(ScmInfo(url("https://github.com/forwardloop/glicko2s"),
                            "git@github.com:forwardloop/glicko2s.git"))
developers := List(Developer("forwardloop",
                             "Kris",
                             "support@squashpoints.com",
                             url("https://github.com/forwardloop")))
licenses += ("MIT", url("https://opensource.org/licenses/MIT"))
publishMavenStyle := true
sonatypeProfileName := "com.github.forwardloop"
organization := "com.github.forwardloop"

// Sonatype repo settings
publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)
