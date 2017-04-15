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

  sectorsRDD(setDimensions, sectorDimensions) foreachPartition {
    points: Iterator[(Position, Sector)] =>

      import org.pfcoperez.iterativegen.MandelbrotSet.numericExploration

      val (w, h) = setDimensions
      val (sw, sh) = sectorDimensions
      val ((sx, sy), sector) = points.next()

      val partName = s"sector${sector}at${sx}x$sy"

      implicit val scale: Scale = Scale(
        RealFrame(-0.7350 ->  0.1100, -0.7460 -> 0.1200),
        PixelFrame(0L -> 0L, (w-1L, h-1L))
      )

      val renderer: ImageCanvas = new BufferedImageCanvas(sw, sh)(partName)

      for(x <-sx until (sx+sw); y <- sy until (sy+sh)) {

        val point: Point = Pixel(x.toLong, y.toLong)

        val (st, nIterations) = numericExploration(point.tuple, GeneratorParameters.maxIterations)

        val color: Int = st map { _ => 0 } getOrElse {
          colorPalette(nIterations % colorPalette.size)
        }

        renderer.drawPoint(x.toLong -> y.toLong, color)
      }

      renderer.render(new File(s"/tmp/fractalparts/$partName.png"))
  }


}
