package perfio

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra._

import java.io._
import java.nio.file.Paths
import java.nio.{ByteBuffer, ByteOrder}
import java.util.concurrent.TimeUnit

@BenchmarkMode(Array(Mode.AverageTime))
@Fork(value = 1, jvmArgsAppend = Array("-Xmx12g", "-Xss32M", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC", "--enable-native-access=ALL-UNNAMED", "--add-modules", "jdk.incubator.vector"))
@Threads(1)
@Warmup(iterations = 15, time = 1)
@Measurement(iterations = 15, time = 1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
class BufferedOutputUncheckedBenchmark extends BenchUtil {

  val count = 20000000
  val byteSize = count * 8

  @Benchmark
  def simple8(bh: Blackhole): Unit = {
    val out = BufferedOutput.growing(byteSize)
    var i = 0
    while(i < count) {
      out.int8(i.toByte)
      out.int8((i+1).toByte)
      out.int8((i+2).toByte)
      out.int8((i+3).toByte)
      out.int8((i+4).toByte)
      out.int8((i+5).toByte)
      out.int8((i+6).toByte)
      out.int8((i+7).toByte)
      i += 1
    }
    out.close()
    bh.consume(out.buffer)
    bh.consume(out.length)
  }

  @Benchmark
  def internal8(bh: Blackhole): Unit = {
    val out = BufferedOutput.growing(byteSize)
    var i = 0
    while(i < count) {
      val p = out.fwd(8)
      out.buf(p) = i.toByte
      out.buf(p+1) = (i+1).toByte
      out.buf(p+2) = (i+2).toByte
      out.buf(p+3) = (i+3).toByte
      out.buf(p+4) = (i+4).toByte
      out.buf(p+5) = (i+5).toByte
      out.buf(p+6) = (i+6).toByte
      out.buf(p+7) = (i+7).toByte
      i += 1
    }
    out.close()
    bh.consume(out.buffer)
    bh.consume(out.length)
  }

  @Benchmark
  def simple16(bh: Blackhole): Unit = {
    val out = BufferedOutput.growing(byteSize)
    var i = 0
    while(i < count) {
      out.int16(i.toShort)
      out.int16((i+1).toShort)
      out.int16((i+2).toShort)
      out.int16((i+3).toShort)
      i += 1
    }
    out.close()
    bh.consume(out.buffer)
    bh.consume(out.length)
  }

  @Benchmark
  def internal16(bh: Blackhole): Unit = {
    val out = BufferedOutput.growing(byteSize)
    var i = 0
    while(i < count) {
      val p = out.fwd(8)
      BufferUtil.BA_SHORT_BIG.set(out.buf, p, i.toShort)
      BufferUtil.BA_SHORT_BIG.set(out.buf, p+2, (i+1).toShort)
      BufferUtil.BA_SHORT_BIG.set(out.buf, p+4, (i+2).toShort)
      BufferUtil.BA_SHORT_BIG.set(out.buf, p+6, (i+3).toShort)
      i += 1
    }
    out.close()
    bh.consume(out.buffer)
    bh.consume(out.length)
  }
}
