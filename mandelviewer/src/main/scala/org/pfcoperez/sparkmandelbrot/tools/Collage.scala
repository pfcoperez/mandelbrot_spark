package org.pfcoperez.sparkmandelbrot.tools

import org.pfcoperez.geometry.Primitives2D.Pixel

import java.io.{File, PrintWriter}
import scala.util.Try

object Collage extends App {

  val imagesDir = new File(args(0)) //TODO: Use a better arguments parser
  val sizeRatio = Try { //TODO: Avoid using exceptions as flow control
    args(1).toDouble
  } getOrElse 1.0

  val index = new ImageIndex(imagesDir)

  val nSectorsRow = index.nSectors._1

  val htmlStr: String = {

    val (sw, sh) = index.sectorSize

    val tileW: Int = sw*sizeRatio toInt
    val tileH: Int = sh*sizeRatio toInt

    val htmlTable = {
      ("" /: index.sectorsPositions) {
        case (acc, (sector, Pixel(x, y))) =>
          val fileName = "./" + index.sectionMatrix(x)(y)._2.getName
          val rowHeader = if(sector % nSectorsRow == 0) "</tr><tr>" else ""

          val cell = s"""
             |<td>
             |  <a href="$fileName">
             |    <img src="$fileName" width="${tileW}px" height="${tileH}px">
             |  </a>
             |</td>
             |""" stripMargin

          acc + rowHeader + cell
          
      } drop(5)
    } + "</tr>"

    s"""
       |<html>
       | <body>
       |   <table border="0">
       |    $htmlTable
       |   </table>
       | </body>
       |</html>
    """.stripMargin
  }

  val outputFile = new File(imagesDir.getPath + File.separator + "index.html")

  val pw = new PrintWriter(outputFile)
  pw.write(htmlStr)
  pw.close()

}
