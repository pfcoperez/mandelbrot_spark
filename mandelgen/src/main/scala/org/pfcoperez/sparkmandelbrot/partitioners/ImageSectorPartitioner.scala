package org.pfcoperez.sparkmandelbrot.partitioners

import org.apache.spark.Partitioner
import org.pfcoperez.geometry.Primitives2D.{PixelFrame, Pixel, sector}

class ImageSectorPartitioner(sectorSize: (Long, Long), pixelFrame: PixelFrame) extends Partitioner {

  override def numPartitions: Int = {
    val sectorArea: Long = sectorSize.productIterator.reduce[Any] {
      case (a: Long, b: Long) => a*b
    }.asInstanceOf[Long]
    (pixelFrame.area/sectorArea).toInt
  }

  override def getPartition(key: Any): Int = key match {
    case p: Pixel =>
      (sector(p, sectorSize)(pixelFrame) % (Int.MaxValue.toLong+1L)).toInt
  }

}
