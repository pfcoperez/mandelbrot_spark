package org.pfcoperez.sparkmandelbrot.imagetools.impl

import java.io.File
import javax.imageio.ImageIO

import org.pfcoperez.geometry.Primitives2D
import org.pfcoperez.sparkmandelbrot.imagetools.ImageCanvas

class BufferedImageCanvas(
                           val w: Int,
                           val h: Int
                         )(val description: String) extends ImageCanvas {

  import java.awt.image.BufferedImage

  private val buffer: BufferedImage =
    new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)

  private val initialState = buffer.getData

  override def drawPoint(p: Primitives2D.Pixel)(r: Byte, g: Byte, b: Byte): Unit = {
    val (x, y) = p.tuple
    val color = 0x10000*r+0x100*g+b
    buffer.setRGB((x % w).toInt, (y % h).toInt, color)
  }

  override def clear(): Unit = buffer.setData(initialState)

  override def render(output: File): Unit =
    ImageIO.write(buffer, "PNG", output)

}
