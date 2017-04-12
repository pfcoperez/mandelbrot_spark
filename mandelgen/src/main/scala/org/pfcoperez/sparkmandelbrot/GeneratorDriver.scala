package org.pfcoperez.sparkmandelbrot

import org.apache.spark.{SparkConf, SparkContext}

object GeneratorDriver extends App {

  val appName = "MandelbrotSetGen"
  val master = "local[2]"

  val conf = new SparkConf().setAppName(appName).setMaster(master)
  new SparkContext(conf)

}
