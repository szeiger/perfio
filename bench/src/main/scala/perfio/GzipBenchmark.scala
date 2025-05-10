package perfio

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.*

import java.util.concurrent.TimeUnit
import java.util.zip.GZIPOutputStream

@BenchmarkMode(Array(Mode.AverageTime))
@Fork(value = 1, jvmArgsAppend = Array("-Xmx12g", "-Xss32M", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC",
  "--enable-native-access=ALL-UNNAMED", "--add-modules", "jdk.incubator.vector"
))
@Threads(1)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 10, time = 1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
class GzipBenchmark extends BenchUtil:

  //@Param(Array("numSmall", "chunks", "chunksSlow", "randomChunks"))
  @Param(Array("chunks"))
  var dataSet: String = null
  final lazy val data = BenchmarkDataSet.forName(dataSet)
  import data.*

  @Benchmark
  def noopBufferedOutput(bh: Blackhole): Unit =
    val out = BufferedOutput.growing(byteSize)
    writeTo(out)
    bh.consume(out.buffer)
    bh.consume(out.length)

//  @Benchmark
//  def gzipOutputStream(bh: Blackhole): Unit =
//    val bout = new MyByteArrayOutputStream(byteSize)
//    val gout = new GZIPOutputStream(bout)
//    writeTo(gout)
//    bh.consume(bout.getSize)
//    bh.consume(bout.getBuffer)
//
//  @Benchmark
//  def gzipBufferedOutput(bh: Blackhole): Unit =
//    val out = BufferedOutput.growing(byteSize)
//    val gout = new GzipBufferedOutput(out)
//    writeTo(gout)
//    bh.consume(out.buffer)
//    bh.consume(out.length)

  @Benchmark
  def asyncGzipBufferedOutput(bh: Blackhole): Unit =
    val out = BufferedOutput.growing(byteSize)
    val gout = new AsyncGzipBufferedOutput(out)
    writeTo(gout)
    bh.consume(out.buffer)
    bh.consume(out.length)

  @Benchmark
  def asyncGzipBufferedOutputUnlimited(bh: Blackhole): Unit =
    val out = BufferedOutput.growing(byteSize)
    val gout = new AsyncGzipBufferedOutput(out, 0)
    writeTo(gout)
    bh.consume(out.buffer)
    bh.consume(out.length)

  @Benchmark
  def parallelGzipBufferedOutput(bh: Blackhole): Unit =
    val out = BufferedOutput.growing(byteSize)
    val gout = new ParallelGzipBufferedOutput(out)
    writeTo(gout)
    bh.consume(out.buffer)
    bh.consume(out.length)

  @Benchmark
  def parallelGzipBufferedOutputSinglePart(bh: Blackhole): Unit =
    val out = BufferedOutput.growing(byteSize)
    val gout = new ParallelGzipBufferedOutput(out, -1, Int.MaxValue)
    writeTo(gout)
    bh.consume(out.buffer)
    bh.consume(out.length)

  @Benchmark
  def parallelGzipBufferedOutputNoPart(bh: Blackhole): Unit =
    val out = BufferedOutput.growing(byteSize)
    val gout = new ParallelGzipBufferedOutput(out, -1, 0)
    writeTo(gout)
    bh.consume(out.buffer)
    bh.consume(out.length)
