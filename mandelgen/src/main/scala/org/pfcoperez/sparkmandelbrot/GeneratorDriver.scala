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

object GeneratorParameters {
  val maxIterations = 3000
  val setDimensions = (16384, 16384)
  val sectorDimensions = (4096, 4096)

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

}

object GeneratorDriver extends App {

  val appName = "MandelbrotSetGen"
  val master = "local[4]"

  import GeneratorParameters._

  val conf = new SparkConf().setAppName(appName).setMaster(master)
  val context = new SparkContext(conf)

  type Position = (Int, Int)
  type Sector = Int
  type SectorizedPosition = (Position, Sector)

  def partitionedSpaceRDD(
                           dimensions: (Int, Int),
                           sectorDimensions: (Int, Int)
                         ): RDD[SectorizedPosition] = {
    import context._

    val (w, h) = dimensions
    val asLongPairSectorSize = sectorDimensions._1.toLong -> sectorDimensions._2.toLong

    val frame = PixelFrame(0L -> 0L, (w-1L, h-1L))

    (range(0, w-1) cartesian range(0, h-1)) map {
      case (x: Long, y: Long) =>
        (x.toInt, y.toInt) -> sector(x -> y, asLongPairSectorSize)(frame).toInt
    } partitionBy {
      new ImageSectorPartitioner(sectorDimensions, frame)
    }
  }

  case class MandelbrotFeature(
                                sector: Sector,
                                state: Option[(Double, Double)],
                                nIterations: Int
                              )

  def mandelbrotSet(space: RDD[SectorizedPosition])(
    maxIterations: Int,
    dimensions: (Int, Int)
  ): RDD[(Position, MandelbrotFeature)] = {

    import org.pfcoperez.iterativegen.MandelbrotSet.numericExploration

    val (w, h) = dimensions

    implicit val scale: Scale = Scale(
      RealFrame(-0.7350 ->  0.1100, -0.7460 -> 0.1200),
      PixelFrame(0L -> 0L, (w-1L, h-1L))
    )

    space map { case ((x, y), sector) =>
      val point: Point = Pixel(x, y)
      val (st, niterations) = numericExploration(point.tuple, maxIterations)
      (x, y) -> MandelbrotFeature(sector, st, niterations)
    }

  }

  val result = mandelbrotSet(partitionedSpaceRDD(setDimensions, sectorDimensions))(maxIterations, setDimensions)

  result.foreachPartition {
    points: Iterator[(Position, MandelbrotFeature)] =>
      val (w, s) = GeneratorParameters.sectorDimensions

      val ((sx, sy), MandelbrotFeature(sector, _, _)) = points.next()

      val partName = s"sector${sector}at${sx}x$sy"

      val renderer: ImageCanvas = new BufferedImageCanvas(w, s)(partName)
      points foreach {
        case ((x, y), MandelbrotFeature(_, st, nIterations)) =>
          val color: Int =
            st map { _ => 0
            } getOrElse {
              colorPalette(nIterations % colorPalette.size)
            }
          renderer.drawPoint(x.toLong -> y.toLong, color)
      }
      renderer.render(new File(s"/tmp/fractalparts/$partName.png"))
  }


}
