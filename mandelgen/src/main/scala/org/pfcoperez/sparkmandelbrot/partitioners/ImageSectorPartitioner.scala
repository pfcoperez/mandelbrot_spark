package org.pfcoperez.sparkmandelbrot.partitioners

import org.apache.spark.Partitioner
import org.pfcoperez.geometry.Primitives2D.{PixelFrame, Pixel, sector}

class ImageSectorPartitioner(sectorSize: (Int, Int), pixelFrame: PixelFrame) extends Partitioner {

  override def numPartitions: Int = {
    val sectorArea: Long = sectorSize.productIterator.reduce[Any] {
      case (a: Int, b: Int) => a.toLong*b.toLong
    }.asInstanceOf[Long]
    (pixelFrame.area/sectorArea).toInt
  }

  override def getPartition(key: Any): Int = key match {
    case p: Pixel =>
      val asLongPairSectorSize = (sectorSize._1.toLong, sectorSize._2.toLong)
      (sector(p, asLongPairSectorSize)(pixelFrame) % (Int.MaxValue.toLong+1L)).toInt
  }

}
