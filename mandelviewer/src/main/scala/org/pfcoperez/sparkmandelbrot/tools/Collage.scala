package org.pfcoperez.sparkmandelbrot.tools

import org.pfcoperez.geometry.Primitives2D.Pixel

import java.io.{File, PrintWriter}

object Collage extends App {

  case class CollageConfig(imagesDir: File, sizeRatio: Double = 1.0)

  val appName = "Collage"
  val argumentsParser = new scopt.OptionParser[CollageConfig](appName) {
    head(appName)

    arg[File]("<dir>").text("Directory containing the image tiles") action { (f, config) =>
      config.copy(imagesDir = f)
    }

    opt[Double]('z', "zoom").text("Zoom ratio for the output image") action { (zoom, config) =>
      config.copy(sizeRatio = zoom)
    }

  }

  argumentsParser.parse(args, CollageConfig(new File("."))) foreach { cfg =>
    import cfg._

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
                          |<td width="$tileW" height="$tileH">
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

}
