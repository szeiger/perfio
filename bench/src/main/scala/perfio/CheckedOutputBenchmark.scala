package perfio

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.*

import java.util.concurrent.TimeUnit
import java.util.zip.{CRC32, CheckedOutputStream}

@BenchmarkMode(Array(Mode.AverageTime))
@Fork(value = 1, jvmArgsAppend = Array("-Xmx12g", "-Xss32M", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC",
  "--enable-native-access=ALL-UNNAMED", "--add-modules", "jdk.incubator.vector"
))
@Threads(1)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 10, time = 1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
class CheckedOutputBenchmark extends BenchUtil:

  @Param(Array("chunks", "num"))
  var dataSet: String = null

  @Param(Array("32768"))
  var blockSize: Int = 0

  final lazy val data = BenchmarkDataSet.forName(dataSet)
  import data.*

  @Benchmark
  def noop(bh: Blackhole): Unit =
    val out = BufferedOutput.growing(byteSize, blockSize)
    writeTo(out)
    bh.consume(out.buffer)
    bh.consume(out.length)

  @Benchmark
  def javaCRC32(bh: Blackhole): Unit =
    val bout = new MyByteArrayOutputStream(byteSize)
    val crc = new CRC32
    val cout = new CheckedOutputStream(bout, crc)
    writeTo(cout)
    bh.consume(bout.getSize)
    bh.consume(bout.getBuffer)
    bh.consume(crc.getValue)

  @Benchmark
  def perfioCRC32(bh: Blackhole): Unit =
    val out = BufferedOutput.growing(byteSize, blockSize)
    val crc = new CRC32
    val cout = new CheckedBufferedOutput(out, crc)
    writeTo(cout)
    bh.consume(out.buffer)
    bh.consume(out.length)
    bh.consume(crc.getValue)
