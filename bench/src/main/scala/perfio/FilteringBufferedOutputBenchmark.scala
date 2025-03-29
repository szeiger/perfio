package perfio

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.*

import java.nio.file.Paths
import java.util.concurrent.TimeUnit

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

  @Param(Array("num", "chunks"))
  var dataSet: String = null
  final lazy val data = BenchmarkDataSet.forName(dataSet)
  import data.*

  @Benchmark
  def array_syncBlock(bh: Blackhole): Unit =
    val out = BufferedOutput.growing(byteSize)
    val gout = new XorBlockBufferedOutput(out, 85)
    writeTo(gout)
    bh.consume(out.buffer)
    bh.consume(out.length)

//  @Benchmark
//  def array_partialFlushing(bh: Blackhole): Unit =
//    val out = BufferedOutput.growing(byteSize)
//    val gout = new XorFlushingBufferedOutput(out, 85, true)
//    writeTo(gout)
//    bh.consume(out.buffer)
//    bh.consume(out.length)
//
//  @Benchmark
//  def array_fullFlushing(bh: Blackhole): Unit =
//    val out = BufferedOutput.growing(byteSize)
//    val gout = new XorFlushingBufferedOutput(out, 85, false)
//    writeTo(gout)
//    bh.consume(out.buffer)
//    bh.consume(out.length)

  @Benchmark
  def file_syncBlock(bh: Blackhole): Unit =
    val out = BufferedOutput.ofFile(Paths.get("/dev/null"))
    val gout = new XorBlockBufferedOutput(out, 85)
    writeTo(gout)
    out.close()

//  @Benchmark
//  def file_partialFlushing(bh: Blackhole): Unit =
//    val out = BufferedOutput.ofFile(Paths.get("/dev/null"))
//    val gout = new XorFlushingBufferedOutput(out, 85, true)
//    writeTo(gout)
//    out.close()
//
//  @Benchmark
//  def file_fullFlushing(bh: Blackhole): Unit =
//    val out = BufferedOutput.ofFile(Paths.get("/dev/null"))
//    val gout = new XorFlushingBufferedOutput(out, 85, false)
//    writeTo(gout)
//    out.close()

  final class XorBlockBufferedOutput(parent: BufferedOutput, mask: Byte) extends FilteringBufferedOutput(parent, false):
    def finalizeBlock(b: BufferedOutput, blocking: Boolean): Boolean =
      b.state = BufferedOutput.STATE_CLOSED
      var i = b.start
      while i < b.pos do
        b.buf(i) = (b.buf(i) ^ mask).toByte
        i += 1
      false

  final class XorFlushingBufferedOutput(parent: BufferedOutput, mask: Byte, partialFlush: Boolean) extends FilteringBufferedOutput(parent, partialFlush):
    def finalizeBlock(b: BufferedOutput, blocking: Boolean): Boolean =
      if(b.state == BufferedOutput.STATE_PROCESSING) b.state = BufferedOutput.STATE_CLOSED
      val blen = b.pos - b.start
      val p = parent.fwd(blen)
      var i = 0
      while i < blen do
        parent.buf(p + i) = (b.buf(b.start + i) ^ mask).toByte
        i += 1
      true
