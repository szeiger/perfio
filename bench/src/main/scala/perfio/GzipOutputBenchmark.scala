package perfio

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.*

import java.io.BufferedOutputStream
import java.util.concurrent.{ForkJoinPool, TimeUnit}
import java.util.zip.{Deflater, GZIPOutputStream}

@BenchmarkMode(Array(Mode.AverageTime))
@Fork(value = 1, jvmArgsAppend = Array("-Xmx12g", "-Xss32M", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC",
  "--enable-native-access=ALL-UNNAMED", "--add-modules", "jdk.incubator.vector"
))
@Threads(1)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 10, time = 1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
class GzipOutputBenchmark extends BenchUtil:

  @Param(Array("numSmall", "chunks", "chunksSlow50", "chunksVerySlow", "randomChunks"))
  var dataSet: String = null

  //@Param(Array("1024", "32768"))
  @Param(Array("32768"))
  var blockSize: Int = 0

  final lazy val data = BenchmarkDataSet.forName(dataSet)
  import data.*

  println()
  println("availableProcessors: "+Runtime.getRuntime().availableProcessors())
  println("Parallelism: "+ForkJoinPool.getCommonPoolParallelism)
  println()

  @Benchmark
  def noop(bh: Blackhole): Unit =
    val out = BufferedOutput.growing(byteSize, blockSize)
    writeTo(out)
    bh.consume(out.buffer)
    bh.consume(out.length)

  @Benchmark
  def gzipOutputStream(bh: Blackhole): Unit =
    val bout = new MyByteArrayOutputStream(byteSize)
    val gout = new GZIPOutputStream(bout)
    writeTo(gout)
    bh.consume(bout.getSize)
    bh.consume(bout.getBuffer)

  @Benchmark
  def gzipBufferedOutputStream(bh: Blackhole): Unit =
    val bout = new MyByteArrayOutputStream(byteSize)
    val gout = new BufferedOutputStream(new GZIPOutputStream(bout))
    writeTo(gout)
    bh.consume(bout.getSize)
    bh.consume(bout.getBuffer)

  @Benchmark
  def sync(bh: Blackhole): Unit =
    val out = BufferedOutput.growing(byteSize, blockSize)
    val gout = GzipBufferedOutput.sync(out)
    writeTo(gout)
    bh.consume(out.buffer)
    bh.consume(out.length)

  @Benchmark
  def async(bh: Blackhole): Unit =
    val out = BufferedOutput.growing(byteSize, blockSize)
    val gout = GzipBufferedOutput.async(out)
    writeTo(gout)
    bh.consume(out.buffer)
    bh.consume(out.length)

  @Benchmark
  def asyncUnlimited(bh: Blackhole): Unit =
    val out = BufferedOutput.growing(byteSize, blockSize)
    val gout = GzipBufferedOutput.async(out, 0, 65536, 0, true, null, Deflater.DEFAULT_COMPRESSION)
    writeTo(gout)
    bh.consume(out.buffer)
    bh.consume(out.length)

  @Benchmark
  def parallel(bh: Blackhole): Unit =
    val out = BufferedOutput.growing(byteSize, blockSize)
    val gout = GzipBufferedOutput.parallel(out)
    writeTo(gout)
    bh.consume(out.buffer)
    bh.consume(out.length)

  @Benchmark
  def parallelSinglePart(bh: Blackhole): Unit =
    val out = BufferedOutput.growing(byteSize, blockSize)
    val gout = GzipBufferedOutput.parallel(out, -1, Int.MaxValue, Int.MaxValue, false, null, Deflater.DEFAULT_COMPRESSION)
    writeTo(gout)
    bh.consume(out.buffer)
    bh.consume(out.length)

  @Benchmark
  def parallelNoPart(bh: Blackhole): Unit =
    val out = BufferedOutput.growing(byteSize, blockSize)
    val gout = GzipBufferedOutput.parallel(out, -1, 0, 0, false, null, Deflater.DEFAULT_COMPRESSION)
    writeTo(gout)
    bh.consume(out.buffer)
    bh.consume(out.length)
