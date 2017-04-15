package org.pfcoperez.sparkmandelbrot.imagetools

import java.io.File

import org.pfcoperez.geometry.Primitives2D.Pixel

trait ImageCanvas {
  val w: Int
  val h: Int
  val description: String

  def drawPoint(p: Pixel, color: Int): Unit

  def drawPoint(p: Pixel, rgb: (Byte, Byte,Byte)): Unit = {
    val (r, g, b) = rgb
    drawPoint(p, 0x10000*r+0x100*g+b)
  }

  def clear(): Unit
  def render(output: File): Unit
}
