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
  val maxIterations = 1000
  val setDimensions = (4096, 4096)
  val sectorDimensions = (2048, 2048)
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
      RealFrame(-2.5 -> -1.0, 1.0 -> 1.0),
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
      val partName = s"it${points.hashCode()}"

      val renderer: ImageCanvas = new BufferedImageCanvas(w, s)(partName)
      points foreach {
        case ((x, y), MandelbrotFeature(_, st, nIterations)) =>
          val (r: Byte, g: Byte, b: Byte) =
            st.map(_ => (0xff.toByte, 0xff.toByte, 0xff.toByte)).getOrElse((0.toByte, 0.toByte, 0.toByte))
          renderer.drawPoint(x.toLong -> y.toLong)(r, g, b)
      }
      renderer.render(new File(s"/tmp/fractalparts/$partName.png"))
  }


}
