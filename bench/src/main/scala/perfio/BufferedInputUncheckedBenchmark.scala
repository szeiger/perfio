package perfio

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra._

import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

@BenchmarkMode(Array(Mode.AverageTime))
@Fork(value = 1, jvmArgsAppend = Array("-Xmx12g", "-Xss32M", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC", "--enable-native-access=ALL-UNNAMED", "--add-modules", "jdk.incubator.vector"))
@Threads(1)
@Warmup(iterations = 15, time = 1)
@Measurement(iterations = 15, time = 1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
class BufferedInputUncheckedBenchmark extends BenchUtil {

  val count = 20000000
  val byteSize = count * 8
  val data = new Array[Byte](byteSize)

  @Setup
  def setup(): Unit = {
    for(i <- data.indices) data(i) = (i % 255).toByte
  }

  @Benchmark
  def simple8(bh: Blackhole): Unit = {
    val in = BufferedInput.ofArray(data)
    var i = 0
    while(i < count) {
      bh.consume(in.int8() | in.int8() | in.int8() | in.int8() | in.int8() | in.int8() | in.int8() | in.int8())
      i += 1
    }
    in.close()
  }

  @Benchmark
  def internal8(bh: Blackhole): Unit = {
    val in = BufferedInput.ofArray(data).asInstanceOf[HeapBufferedInput]
    var i = 0
    while(i < count) {
      val p = in.fwd(8)
      bh.consume(in.buf(p) | in.buf(p+1) | in.buf(p+2) | in.buf(p+3) | in.buf(p+4) | in.buf(p+5) | in.buf(p+6) | in.buf(p+7))
      i += 1
    }
    in.close()
  }

  @Benchmark
  def simple16(bh: Blackhole): Unit = {
    val in = BufferedInput.ofArray(data)
    var i = 0
    while(i < count) {
      bh.consume(in.int16() | in.int16() | in.int16() | in.int16())
      i += 1
    }
    in.close()
  }

  @Benchmark
  def internal16(bh: Blackhole): Unit = {
    val in = BufferedInput.ofArray(data).asInstanceOf[HeapBufferedInput]
    var i = 0
    while(i < count) {
      val p = in.fwd(8)
      bh.consume((BufferUtil.BA_SHORT_BIG.get(in.buf, p): Short) | (BufferUtil.BA_SHORT_BIG.get(in.buf, p+2): Short) | (BufferUtil.BA_SHORT_BIG.get(in.buf, p+4): Short) | (BufferUtil.BA_SHORT_BIG.get(in.buf, p+6): Short))
      i += 1
    }
    in.close()
  }
}
