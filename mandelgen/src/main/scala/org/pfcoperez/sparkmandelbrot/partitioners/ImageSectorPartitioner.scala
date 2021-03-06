package org.pfcoperez.sparkmandelbrot.partitioners

import org.apache.spark.Partitioner
import org.pfcoperez.geometry.Primitives2D.{PixelFrame, Pixel, sector}

class ImageSectorPartitioner(val sectorSize: (Int, Int), val pixelFrame: PixelFrame) extends Partitioner {

  override def numPartitions: Int = {
    val sectorArea: Long = sectorSize.productIterator.reduce[Any] {
      case (a: Int, b: Int) => a.toLong*b.toLong
    }.asInstanceOf[Long]
    (pixelFrame.area.toDouble/sectorArea.toDouble).ceil.toInt
  }

  override def getPartition(key: Any): Int = key match {
    case (x: Int, y: Int) =>
      val asLongPairSectorSize = (sectorSize._1.toLong, sectorSize._2.toLong)
      val p = Pixel(x, y)
      sector(p, asLongPairSectorSize)(pixelFrame).toInt
  }

  override def equals(obj: scala.Any): Boolean = obj match {
    case that: ImageSectorPartitioner =>
      that.sectorSize == sectorSize && that.pixelFrame == pixelFrame
    case _ => false
  }
}
