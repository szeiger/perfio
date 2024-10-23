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
class BufferedInputNumBenchmark extends BenchUtil {

  private[this] var testData: Array[Byte] = _
  private[this] var diskTestDataMedium: File = _

  val count = 20000000

  @Setup(Level.Trial)
  def buildTestData(): Unit = {
    val out = new ByteArrayOutputStream()
    val dout = new DataOutputStream(out)
    var i = 0
    while(i < count) {
      dout.writeByte(i)
      dout.writeInt(i+100)
      dout.writeLong(i+101)
      i += 1
    }
    testData = out.toByteArray
    diskTestDataMedium = writeFileIfMissing("medium", testData)
  }

  private[this] def run(bh: Blackhole, bin: BufferedInput): Unit = {
    var i = 0
    while(i < count) {
      bh.consume(bin.int8())
      bh.consume(bin.int32())
      bh.consume(bin.int64())
      i += 1
    }
    bin.close()
  }

  def run(bh: Blackhole, din: DataInputStream): Unit = {
    var i = 0
    while(i < count) {
      bh.consume(din.readByte())
      bh.consume(din.readInt())
      bh.consume(din.readLong())
      i += 1
    }
    din.close()
  }

  def run(bh: Blackhole, buf: ByteBuffer): Unit = {
    buf.order(ByteOrder.BIG_ENDIAN)
    var i = 0
    while(i < count) {
      bh.consume(buf.get())
      bh.consume(buf.getInt())
      bh.consume(buf.getLong())
      i += 1
    }
  }

  @Benchmark
  def array_DataInputStream(bh: Blackhole): Unit = run(bh, new DataInputStream(new ByteArrayInputStream(testData)))
  @Benchmark
  def array_ByteBuffer(bh: Blackhole): Unit = run(bh, ByteBuffer.wrap(testData))
  @Benchmark
  def array_BufferedInput(bh: Blackhole): Unit = run(bh, BufferedInput.of(new ByteArrayInputStream(testData)))
  @Benchmark
  def array_BufferedInput_fromArray(bh: Blackhole): Unit = run(bh, BufferedInput.ofArray(testData))

  @Benchmark
  def mediumFile_DataInputStream(bh: Blackhole): Unit = run(bh, new DataInputStream(new BufferedInputStream(new FileInputStream(diskTestDataMedium))))
  @Benchmark
  def mediumFile_BufferedInput(bh: Blackhole): Unit = run(bh, BufferedInput.of(new FileInputStream(diskTestDataMedium)))
  @Benchmark
  def mediumFile_BufferedInput_mapped(bh: Blackhole): Unit = run(bh, BufferedInput.ofMappedFile(diskTestDataMedium.toPath))
}
