# Mandelbrot Set Spark generator

An Apache Spark application (driver) aimed to generate huge
[Mandelbrot's Set](https://en.wikipedia.org/wiki/Mandelbrot_set) representation by segmenting and distributing the set complex range.

It implements the simplest form the escape algorithm and the plumbing
needed to distribute it in Spark tasks in a fashion that allows to
confine the generation of whole 2D sectors of its representations to single
machines.

The root project includes:

 *  **The generator (mandelgen)**: A Spark driver which is able to generate whole sectors representation (in loseless PNG format).
 *  **A simple image composer and image tools (viewer)**: Which allows visualizing the generated representation by composing the sectors into a single HTML images table mosaic.
 *  **Geometry primitives (math)**: Used by the generator and the mosaic composer to build and scale images.

[![Example](http://i.imgur.com/jCNOT7L.png)](http://orionsword.no-ip.org/demos/wholefractal/)

## Images generator (mandelgen)

It is designed as as parametrized Spark application which should started through [Spark's submit](http://spark.apache.org/docs/latest/submitting-applications.html) mechanism.

Its input parameters are used to determine:

* The **exploration area**: The complex range of the set to be explored and represented.
* The **output total size**: The aggregated size of each section of the generated representation (in pixels).
* The **sector size**: Each section size (in pixels).
* The maximum **number of iterations** for each point in the escape algorithm.
* The **output** directory where each executor will dump the resulting image.

The whole image generation is distributed in sections (aka **sectors**) to be computed as Spark partitions.
These sectors are the result of a grid decomposition of the selected exploration area.

The final representation is thus provided via PNG images which get stored by each partition executor in the **output** directory.
The output directory can be local or remote as long as it can be accessed as a file system folder from the executor.
It could be, for example, a local directory or a NFS mount entry.

## Launching and exploration/generation task

As mentioned above, the current design requires the generation app to be launched using spark submit.
The exploration parameters and output directory are provided through the application arguments.

As a distributed Spark application, to the generator it has to be packaged within an unber jar containing its code as well as its dependencies:

```bash
git clone git@github.com:pfcoperez/mandelbrot_spark.git
cd mandelbrot_spark

sbt "project generator" assembly
```

This will generate the following artifact:

_generator-assembly-0.1-SNAPSHOT.jar_

... which has to accessible for the worker nodes in a path which should be established in the Spark submit parametrization. e.g:

```bash
cp mandelgen/target/scala-2.11/generator-assembly-0.1-SNAPSHOT.jar /mnt/nfs/generator.jar
```

Then, we can start exploring the beautiful Mandelbrot's universe in a distributed fashion:

```bash
/LOCAL_SPARK_PATH/bin/spark-submit \
    --class org.pfcoperez.sparkmandelbrot.GeneratorDriver \
    --master spark://ganymede:7077 \ # Spark master selection: Standalone, yarn, mesos... even local!
    --deploy-mode cluster \ # Choose "cluster" to run the driver within a Spark worker node...
    # ... or "client" if you want to keep your driver running locally.
    /mnt/nfs/generator.jar \ # Location of the application.
    --from "(-2.5,-1)" --to "(1,1)" \ # Exploration range
    -i 3500 \ # Maximum number of iterations of the escape algorithm
    -s "(18900,10800)" \ # Mosaic total size (w,h) in pixels
    --sectorsize "(1890,1080)" \ # Tile/Sector size
    /mnt/nfs/results/wholefractal # Folder where the representation of each tile will be stored.
```

The command above will start out Spark application within the cluster, thus allowing us to halt our laptop, go to home, drink some beers and check the results of out computation the day after its launch.
( as long as your Spark cluster isn't your laptop ;-) )

# Sectors and partitions, the magic resides in the partitioner...

The fact that this generator dumps its results in tiles, called sectors, matching a given grid is the secret behind being able to generate images within the same machine and, therefore, behind providing a shuffle free distributed algorithm.
As the application uses a custom partitioner for K-V RDDs where K is `(Long, Long)` and represents a pixel in the whole resulting image, it is possible to use `foreachPartition` as  way of consolidating contiguous areas into a single output file.

```scala
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
```

Therefore, **each sector resides in a single partition** so the application will create as many partitions as sectors are.
Namely, as many partitions as:

```
// w = Whole image width
// h = Whole image height

// sw = Sector width
// sh = Sector height

val nPartitions = (w*h)/(sw*sh)

```

