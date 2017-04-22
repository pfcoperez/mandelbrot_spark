package org.pfcoperez.sparkmandelbrot

import java.io.File

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import org.pfcoperez.geometry.Primitives2D.PixelFrame
import org.pfcoperez.sparkmandelbrot.partitioners.ImageSectorPartitioner
import org.pfcoperez.geometry.Primitives2D.Implicits._
import org.pfcoperez.geometry.Primitives2D._
import org.pfcoperez.sparkmandelbrot.imagetools.ImageCanvas
import org.pfcoperez.sparkmandelbrot.imagetools.impl.BufferedImageCanvas

object Colors {

  val colorPalette: Array[Int] = Seq(
    "#226666",
    "#669999",
    "#407F7F",
    "#0D4D4D",
    "#003333",
    "#2E4272",
    "#7887AB",
    "#4F628E",
    "#162955",
    "#061539",
    "#2D882D",
    "#88CC88",
    "#55AA55",
    "#116611",
    "#004400"
  ).map(colorStr => Integer.parseInt(colorStr.tail, 16)).toArray

  def colorFor(n: Int): Int = colorPalette(n % colorPalette.size)

}

object GeneratorDriver extends App {

  case class GeneratorConfig(
                              mandelbrotRange: RealFrame = RealFrame(
                                Point(-2.5, -1.0), Point(1.0, 1.0) //Whole set by default
                              ),
                              maxIterations: Int = 1000,
                              resolution: (Int, Int) = (1890, 1080),
                              sectorSize: (Int, Int) = (1890, 1080),
                              outputDir: File = new File("/tmp"),
                              providedSparkMaster: Option[String] = None
                            )

  val appName = "MandelbrotSetGen"
  val argumentsParser = new scopt.OptionParser[GeneratorConfig](appName) {
    head(appName)

    import scopt.Read
    import scopt.Read._

    implicit def pointRead[T](implicit reader: Read[T], numEvidence: Numeric[T]): Read[(T, T)] = new Read[(T, T)] {
      val arity = 2
      val reads = { (str: String) =>
        require(str.startsWith("(") && str.endsWith(")"), "Required format: (v1,v2)")
        val Seq(x, y) = str.tail.init.split(',').map(reader.reads(_))
        x -> y
      }
    }

    val pointLabel = "(x,y)"
    val sizeLabel = "(w,h)"

    opt[(Double, Double)]("from") action { (x, c) =>
      c.copy(mandelbrotRange = c.mandelbrotRange.copy(upperLeft = x))
    } text pointLabel

    opt[(Double, Double)]("to") action { (x, c) =>
      c.copy(mandelbrotRange = c.mandelbrotRange.copy(bottomRight = x))
    } text pointLabel

    opt[Int]('i', "maxIterations") action { (x, c) =>
      c.copy(maxIterations = x)
    } validate { it =>
      if (it <= 0) failure("Maximum number of iterations should be >0")
      else success
    }

    opt[(Int, Int)]('s', "size") action { (x, c) =>
      c.copy(resolution = x, sectorSize = x)
    } text sizeLabel

    opt[(Int, Int)]("sectorsize") action { (x, c) =>
      c.copy(sectorSize = x)
    } text sizeLabel

    opt[String]("master") action { (x, c) =>
      c.copy(providedSparkMaster = Some(x))
    }

    arg[File]("output") action { (x, c) =>
      c.copy(outputDir = x)
    } validate { of =>
      if(of.isDirectory && of.canWrite) success
      else failure("Output directory should exist")
    }

    checkConfig {
      case GeneratorConfig(RealFrame(from, to), _, (w, h), (sw, sh), _, _) =>
        if(from >= to) failure("Empty exploration area!")
        else if(w < 0 || h < 0) failure("Invalid size")
        else if(sw < 0 || sh < 0) failure("Invalid sector size")
        else if(sw > w || sh > h) failure("Sectors can't be bigger than the global resolution")
        else success
    }

  }

  val generatorSettings = argumentsParser.parse(args, GeneratorConfig())

  generatorSettings foreach { config =>

    println {
      s"""
         |Starting Mandelbrot Set exploration
         |-----------------------------------
         |$config
         |
      """.stripMargin
    }

    import config._

    val conf = new SparkConf().setAppName(appName)
    val context = new SparkContext(
      providedSparkMaster.map(master => conf.setMaster(master)) getOrElse conf
    )

    type Position = (Int, Int)
    type Sector = Int
    type SectorizedPosition = (Position, Sector)

    def sectorsRDD(
                    dimensions: (Int, Int),
                    sectorDimensions: (Int, Int)
                  ): RDD[SectorizedPosition] = {
      import context._

      val (w, h) = dimensions
      val asLongPairSectorSize = sectorDimensions._1.toLong -> sectorDimensions._2.toLong
      val (sw, sh) = asLongPairSectorSize

      val frame = PixelFrame(0L -> 0L, (w-1L, h-1L))

      (range(0, w-1, sw) cartesian range(0, h-1, sh)) map {
        case (x: Long, y: Long) =>
          (x.toInt, y.toInt) -> sector(x -> y, asLongPairSectorSize)(frame).toInt
      } partitionBy {
        new ImageSectorPartitioner(sectorDimensions, frame)
      }
    }

    sectorsRDD(resolution, sectorSize) foreachPartition {
      points: Iterator[(Position, Sector)] =>

        import org.pfcoperez.iterativegen.MandelbrotSet.numericExploration

        val (w, h) = resolution
        val (sw, sh) = sectorSize
        val ((sx, sy), sector) = points.next()

        val partName = s"sector${sector}at${sx}x$sy"

        implicit val scale: Scale = Scale(
          mandelbrotRange,
          PixelFrame(0L -> 0L, (w-1L, h-1L))
        )

        val renderer: ImageCanvas = new BufferedImageCanvas(sw, sh)(partName)

        for(x <-sx until (sx+sw); y <- sy until (sy+sh)) {

          val point: Point = Pixel(x.toLong, y.toLong)

          val (st, nIterations) = numericExploration(point.tuple, maxIterations)

          val color: Int = st map { _ => 0 } getOrElse Colors.colorFor(nIterations)

          renderer.drawPoint(x.toLong -> y.toLong, color)
        }

        renderer.render(new File(outputDir.getPath + File.separator + s"$partName.png"))
    }

  }

  System.exit(generatorSettings.map(_ => 0).getOrElse(-1))

}
