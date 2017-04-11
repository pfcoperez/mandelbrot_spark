name := "Distributed Mandelbrot Set"

version := "0.1"
organization := "org.pfcoperez"
scalaVersion := "2.11.8"

lazy val root = (project in file("."))
  .aggregate(
    generator,
    viewer
  ).settings(Common.settings: _*)

lazy val generator = (project in file("./mandelgen"))
  .settings(Common.settings: _*)

lazy val viewer = (project in file("./mandelviewer"))
  .settings(Common.settings: _*)

