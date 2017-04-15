package org.pfcoperez.sparkmandelbrot.imagetools.impl

import java.io.File

import org.pfcoperez.geometry.Primitives2D.Pixel
import org.pfcoperez.sparkmandelbrot.imagetools.ImageCanvas

class BufferedImageCanvas(
                           val w: Int,
                           val h: Int
                         )(val description: String) extends ImageCanvas {

  import java.awt.image.BufferedImage

  private val buffer: BufferedImage =
    new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)

  private val initialState = buffer.getData

  override def drawPoint(p: Pixel, color: Int): Unit = {
    val (x, y) = p.tuple
    buffer.setRGB((x % w).toInt, (y % h).toInt, color)
  }

  override def clear(): Unit = buffer.setData(initialState)

  override def render(output: File): Unit = {
    import javax.imageio.IIOImage
    import javax.imageio.ImageIO
    import javax.imageio.ImageWriteParam
    import javax.imageio.plugins.jpeg.JPEGImageWriteParam
    import javax.imageio.stream.FileImageOutputStream

    val writer = ImageIO.getImageWritersByFormatName("png").next
    writer.setOutput(new FileImageOutputStream(output))

    val encoderParams = new JPEGImageWriteParam(null)
    encoderParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT)
    //encoderParams.setCompressionQuality(1f)

    writer.write(null, new IIOImage(buffer, null, null), encoderParams)
  }

}
