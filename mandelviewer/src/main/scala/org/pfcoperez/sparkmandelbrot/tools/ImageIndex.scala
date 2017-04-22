package org.pfcoperez.sparkmandelbrot.tools

import org.pfcoperez.geometry.Primitives2D.Pixel

import scala.util.Try
import java.io.File

object ImageIndex {
  case class Section(position: Pixel, sector: Int)
}

class ImageIndex(directory: File) {
  require(directory.exists && directory.isDirectory, s"${directory.getName} should exist and be a directory")

  import ImageIndex._

  private def fileName2Section(fileName: String): Try[Section] = Try {
    val SectionRegex = """sector([0-9]+)at([0-9]+)x([0-9]+)\.png""".r
    fileName match {
      case SectionRegex(sectorStr, xStr, yStr) =>
        Section(Pixel(xStr.toLong, yStr.toLong), sectorStr.toInt)
    }
  }

  val sectionMatrix =
    (Map.empty[Long, Map[Long, (Section, File)]] /: directory.listFiles) {
      case (matrix, f) =>
        fileName2Section(f.getName) map { case s @ Section(Pixel(x, y), _) =>
          val column = matrix.getOrElse(x, Map.empty) + (y -> (s, f))
          matrix + (x -> column)
        } getOrElse matrix
    }

  val nSectors =  sectionMatrix.size -> sectionMatrix(0).size

  val sectorsPositions: Seq[(Int, Pixel)] = {
    for {
      (_, row) <- sectionMatrix
      (_, (Section(pos, sector), _)) <- row
    } yield sector -> pos
  }.toSeq.sortBy(_._1)

  val (sectorSize: (Int, Int), imageSize: (Int, Int)) = {

    val sectorSize @ (sw, sh) = {
      val p0: Pixel = sectorsPositions.head._2
      val (_, p1: Pixel) = sectorsPositions find {
        case (_, Pixel(0, _)) | (_, Pixel(_, 0)) => false
        case _ => true
      } head

      (p1.x - p0.x).toInt -> (p1.y - p0.y).toInt
    }

    val size = (sw*nSectors._1, sh*nSectors._2)

    sectorSize -> size

  }

}
