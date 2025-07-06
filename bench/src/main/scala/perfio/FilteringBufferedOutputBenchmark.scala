package perfio

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.*

import java.nio.file.Paths
import java.util.concurrent.{CompletableFuture, TimeUnit}

@BenchmarkMode(Array(Mode.AverageTime))
@Fork(value = 1, jvmArgsAppend = Array("-Xmx12g", "-Xss32M", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC",
  "--enable-native-access=ALL-UNNAMED", "--add-modules", "jdk.incubator.vector"
))
@Threads(1)
@Warmup(iterations = 15, time = 1)
@Measurement(iterations = 10, time = 1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
class FilteringBufferedOutputBenchmark extends BenchUtil:
  import BenchmarkOutputFilters.*

  @Param(Array("num", "chunks"))
  var dataSet: String = null
  final lazy val data = BenchmarkDataSet.forName(dataSet)
  import data.*

  //@Param(Array("array", "file"))
  @Param(Array("file"))
  var output: String = null
  var fileOut = false

  @Param(Array("32768"))
  var blockSize: Int = 0

  @Setup
  def setup: Unit =
    fileOut = output == "file"

  private def run(bh: Blackhole)(f: BufferedOutput => BufferedOutput) =
    if(fileOut) runFile(bh)(f) else runArray(bh)(f)

  private def runArray(bh: Blackhole)(f: BufferedOutput => BufferedOutput) =
    val out = BufferedOutput.growing(byteSize, blockSize)
    writeTo(f(out))
    bh.consume(out.buffer)
    bh.consume(out.length)

  private def runFile(bh: Blackhole)(f: BufferedOutput => BufferedOutput) =
    val out = BufferedOutput.ofFile(Paths.get("/dev/null"), blockSize)
    writeTo(f(out))
    out.close()

//  @Benchmark
//  def syncInPlace(bh: Blackhole): Unit = runArray(bh)(new XorBlockBufferedOutput(_))
//
//  @Benchmark
//  def sync(bh: Blackhole): Unit = runArray(bh)(new XorBlockSwappingBufferedOutput(_))
//
//  @Benchmark
//  def partialFlushing(bh: Blackhole): Unit = runArray(bh)(new XorFlushingBufferedOutput(_, true))
//
//  @Benchmark
//  def flushing(bh: Blackhole): Unit = runArray(bh)(new XorFlushingBufferedOutput(_, false))
//
//  @Benchmark
//  def simpleParallelInPlace(bh: Blackhole): Unit = runArray(bh)(new SimpleAsyncXorBlockBufferedOutput(_))
//
//  @Benchmark
//  def simpleAsyncInPlace(bh: Blackhole): Unit = runArray(bh)(new SimpleAsyncSequentialXorBlockBufferedOutput(_))
//
//  @Benchmark
//  def simpleParallel(bh: Blackhole): Unit = runArray(bh)(new SimpleAsyncSwappingXorBlockBufferedOutput(_))
//
//  @Benchmark
//  def simpleAsync(bh: Blackhole): Unit = runArray(bh)(new SimpleAsyncSequentialSwappingXorBlockBufferedOutput(_))

  @Benchmark
  def parallel(bh: Blackhole): Unit = runArray(bh)(new AsyncXorBlockBufferedOutput(_, false, -1))

  @Benchmark
  def async(bh: Blackhole): Unit = runArray(bh)(new AsyncXorBlockBufferedOutput(_, true, 0))
