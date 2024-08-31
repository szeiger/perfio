package perfio

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra._

import java.io._
import java.nio.{ByteBuffer, ByteOrder}
import java.util.concurrent.TimeUnit

@BenchmarkMode(Array(Mode.AverageTime))
@Fork(value = 1, jvmArgsAppend = Array("-Xmx12g", "-Xss32M", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC", "--enable-native-access=ALL-UNNAMED", "--add-modules", "jdk.incubator.vector"))
@Threads(1)
@Warmup(iterations = 7, time = 1)
@Measurement(iterations = 7, time = 1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
class BufferedInputNumBenchmark {

  private[this] var testData: Array[Byte] = _
  val count = 20000000

  @Setup(Level.Trial)
  def buildTestData(): Unit = {
    val out = new ByteArrayOutputStream()
    val dout = new DataOutputStream(out)
    for(i <- 0 until count) {
      dout.writeByte(i)
      dout.writeInt(i+100)
      dout.writeLong(i+101)
    }
    testData = out.toByteArray
  }

  private[this] def run(bh: Blackhole, bin: BufferedInput): Unit = {
    for(i <- 0 until count) {
      bh.consume(bin.int8())
      bh.consume(bin.int32())
      bh.consume(bin.int64())
    }
    bin.close()
  }

  @Benchmark
  def array_DataInputStream(bh: Blackhole): Unit = {
    val din = new DataInputStream(new ByteArrayInputStream(testData))
    for(i <- 0 until count) {
      bh.consume(din.readByte())
      bh.consume(din.readInt())
      bh.consume(din.readLong())
    }
    din.close()
  }

  @Benchmark
  def array_ByteBuffer(bh: Blackhole): Unit = {
    val buf = ByteBuffer.wrap(testData)
    buf.order(ByteOrder.BIG_ENDIAN)
    for(i <- 0 until count) {
      bh.consume(buf.get())
      bh.consume(buf.getInt())
      bh.consume(buf.getLong())
    }
  }

  @Benchmark
  def array_BufferedInput(bh: Blackhole): Unit = run(bh, BufferedInput(new ByteArrayInputStream(testData)))

  @Benchmark
  def array_BufferedInput_fromArray(bh: Blackhole): Unit = run(bh, BufferedInput.fromArray(testData))
}
