package org.pfcoperez.geometry

object Primitives2D {

  trait Vector[T] { val x: T; val y: T; def tuple: (T,T) = x -> y }

  case class Pixel(x: Long, y: Long) extends Vector[Long]
  case class Point(x: Double, y: Double) extends Vector[Double]

  trait Frame[T] {
    val upperLeft: Vector[T]
    val bottomRight: Vector[T]

    def contains(v: Vector[T])(implicit numericEvidence: Numeric[T]): Boolean = {
      import numericEvidence.mkOrderingOps

      val (xmin, xmax) = xRange
      val (ymin, ymax) = yRange

      xmin <= v.x && v.x <= xmax && ymin <= v.y && v.y <= ymax
    }

    def xRange: (T, T) = upperLeft.x -> bottomRight.x
    def yRange: (T, T) = upperLeft.y -> bottomRight.y

    def area(implicit numericEvidence: Numeric[T]): T = {
      import numericEvidence.mkNumericOps

      val (xmin, xmax) = xRange
      val (ymin, ymax) = yRange

      (ymax-ymin)*(xmax-xmin)
    }

  }

  case class PixelFrame(upperLeft: Pixel, bottomRight: Pixel) extends Frame[Long]
  case class RealFrame(upperLeft: Point, bottomRight: Point) extends Frame[Double]

  case class Scale(realFrame: RealFrame, pixelFrame: PixelFrame)

  object Implicits {

    implicit def tuple2pixel(t: (Long, Long)): Pixel = {
      val (x, y) = t
      Pixel(x, y)
    }

    implicit def tuple2point(t: (Double, Double)): Point = {
      val (x, y) = t
      Point(x, y)
    }

    implicit def real2pixel(p: Point)(implicit scale: Scale): Pixel = {
      import scale._
      import p._

      require(realFrame contains p, s"$p out of $realFrame")

      val (pxMin, pxMax) = pixelFrame.xRange
      val (pyMin, pyMax) = pixelFrame.yRange

      val (xMin, xMax) = realFrame.xRange
      val (yMin, yMax) = realFrame.yRange

      val px = pxMin + ((pxMax-pxMin)*(x-xMin)/(xMax-xMin)).toLong
      val py = pyMin + ((pyMax-pyMin)*(y-yMin)/(yMax-yMin)).toLong

      Pixel(px, py)
    }

    implicit def pixel2real(p: Pixel)(implicit scale: Scale): Point = {
      import scale._
      import p.{x => px, y => py}

      require(pixelFrame contains p, s"$p out of $pixelFrame")

      val (pxMin, pxMax) = pixelFrame.xRange
      val (pyMin, pyMax) = pixelFrame.yRange

      val (xMin, xMax) = realFrame.xRange
      val (yMin, yMax) = realFrame.yRange

      val x = xMin + (xMax-xMin)*((px.toDouble-pxMin)/(pxMax-pxMin))
      val y = yMin + (yMax-yMin)*((py.toDouble-pyMin)/(pyMax-pyMin))

      Point(x, y)
    }

  }

  def sector(p: Pixel, sectorSize: (Long, Long))(frame: PixelFrame): Long = {
    val w: Long = {
      val (xMin, xMax) = frame.xRange
      xMax - xMin + 1
    }

    val (sw, sh) = sectorSize
    val (x, y) = p.tuple

    y/sh * w/sw + x/sw

  }

}
