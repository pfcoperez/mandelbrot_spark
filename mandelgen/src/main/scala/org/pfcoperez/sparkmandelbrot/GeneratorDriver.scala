package org.pfcoperez.sparkmandelbrot

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import org.pfcoperez.geometry.Primitives2D.PixelFrame
import org.pfcoperez.sparkmandelbrot.partitioners.ImageSectorPartitioner
import org.pfcoperez.geometry.Primitives2D.Implicits._
import org.pfcoperez.geometry.Primitives2D._

object GeneratorDriver extends App {

  val maxIterations = 1000
  val setDimensions = (2048, 2048)
  val appName = "MandelbrotSetGen"
  val master = "local[2]"

  val conf = new SparkConf().setAppName(appName).setMaster(master)
  val context = new SparkContext(conf)

  def partitionedSpaceRDD(dimensions: (Int, Int)): RDD[(Int, Int)] = {
    import context._

    val (w, h) = dimensions
    (range(0, w-1L) cartesian range(0, h-1L)) partitionBy {
      new ImageSectorPartitioner(setDimensions, PixelFrame(0L -> 0L, (w-1L, h-1L)))
    }
  }

  def mandelbrotSet(space: RDD[(Int, Int)])(
    maxIterations: Int,
    dimensions: (Int, Int)
  ): RDD[((Int, Int), (Option[(Double, Double)], Int))] = {

    import org.pfcoperez.iterativegen.MandelbrotSet.numericExploration

    val (w, h) = dimensions

    implicit val scale: Scale = Scale(
      RealFrame(-2.5 -> 1.0, -1.0 -> 1.0),
      PixelFrame(0L -> 0L, (w-1L, h-1L))
    )

    space map { case (x, y) =>
      val point: Point = Pixel(x, y)
      (x, y) -> numericExploration(point.tuple, maxIterations)
    }

  }

  import com.datastax.spark.connector._
  import org.apache.spark.sql.cassandra._


  mandelbrotSet(partitionedSpaceRDD(setDimensions))(maxIterations, setDimensions)

}
