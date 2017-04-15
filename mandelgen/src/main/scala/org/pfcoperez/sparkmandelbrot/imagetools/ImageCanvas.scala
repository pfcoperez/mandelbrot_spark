package org.pfcoperez.sparkmandelbrot.imagetools

import java.io.File

import org.pfcoperez.geometry.Primitives2D.Pixel

trait ImageCanvas {
  val w: Int
  val h: Int
  val description: String

  def drawPoint(p: Pixel)(r: Byte, g: Byte, b: Byte): Unit
  def clear(): Unit
  def render(output: File): Unit
}
