import sbt._
import Keys._

object Common {

  val settings: Seq[Def.Setting[_]] = Seq(
    scalaVersion := "2.11.8"
  )

}
