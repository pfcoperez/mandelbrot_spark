name := "Distributed Mandelbrot Set"

version := "0.1"
organization := "org.pfcoperez"
scalaVersion := "2.11.8"

lazy val root = (project in file("."))
  .aggregate(
    math,
    generator,
    viewer
  ).settings(Common.settings: _*)

lazy val generator = (project in file("./mandelgen"))
  .settings(Common.settings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % "2.1.0" % "provided",
      "com.github.scopt" %% "scopt" % Versions.scopt
    )
  )
  .dependsOn(math)

lazy val viewer = (project in file("./mandelviewer"))
  .settings(Common.settings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "com.github.scopt" %% "scopt" % Versions.scopt
    )
  )
  .dependsOn(math)

lazy val math = (project in file("./math"))
  .settings(Common.settings: _*)