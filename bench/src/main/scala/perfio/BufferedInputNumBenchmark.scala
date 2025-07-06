package perfio

import com.esotericsoftware.kryo.io.{ByteBufferInput, Input}
import com.esotericsoftware.kryo.unsafe.{UnsafeByteBufferInput, UnsafeInput}
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.*

import java.io.*
import java.lang.foreign.MemorySegment
import java.nio.{ByteBuffer, ByteOrder}
import java.util.concurrent.TimeUnit

@BenchmarkMode(Array(Mode.AverageTime))
@Fork(value = 1, jvmArgsAppend = Array("-Xmx12g", "-Xss32M", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC",
  "--enable-native-access=ALL-UNNAMED", "--add-modules", "jdk.incubator.vector",
  "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED" // for KryoUnsafe
))
@Threads(1)
@Warmup(iterations = 7, time = 1)
@Measurement(iterations = 7, time = 1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
class BufferedInputNumBenchmark extends BenchUtil:

  private var testData: Array[Byte] = null
  private var testDataGlobal: MemorySegment = null
  private var diskTestData: File = null

  val count = 20000000

  @Param(Array("false", "true"))
  var mixedWarmUp: Boolean = true
  
  @Setup(Level.Trial)
  def buildTestData(): Unit =
    val out = new ByteArrayOutputStream()
    val dout = new DataOutputStream(out)
    var i = 0
    while(i < count)
      dout.writeByte(i)
      dout.writeInt(i+100)
      dout.writeLong(i+101)
      i += 1
    testData = out.toByteArray
    testDataGlobal = toGlobal(testData)
    diskTestData = writeFileIfMissing("medium", testData)
    if(mixedWarmUp) runWarmup: bh =>
      var i = 0
      while(i < 5)
        run(bh, BufferedInput.ofArray(testData))
        run(bh, BufferedInput.ofMappedFile(diskTestData.toPath))
        i += 1

  private def run(bh: Blackhole, bin: BufferedInput): Unit =
    var i = 0
    while(i < count)
      bh.consume(bin.int8())
      bh.consume(bin.int32())
      bh.consume(bin.int64())
      i += 1
    bin.close()

  // JDK: always big endian
  def run(bh: Blackhole, din: DataInputStream): Unit =
    var i = 0
    while(i < count)
      bh.consume(din.readByte())
      bh.consume(din.readInt())
      bh.consume(din.readLong())
      i += 1
    din.close()

  // Kryo: little endian (safe) or native endian (unsafe)
  def run(bh: Blackhole, din: Input): Unit =
    var i = 0
    while(i < count)
      bh.consume(din.readByte())
      bh.consume(din.readInt())
      bh.consume(din.readLong())
      i += 1
    din.close()

  def run(bh: Blackhole, buf: ByteBuffer): Unit =
    buf.order(ByteOrder.BIG_ENDIAN)
    var i = 0
    while(i < count)
      bh.consume(buf.get())
      bh.consume(buf.getInt())
      bh.consume(buf.getLong())
      i += 1

  @Benchmark
  def array_DataInputStream(bh: Blackhole): Unit = run(bh, new DataInputStream(new ByteArrayInputStream(testData)))
  @Benchmark
  def array_Kryo(bh: Blackhole): Unit = run(bh, new Input(testData))
  @Benchmark
  def array_KryoUnsafe(bh: Blackhole): Unit = run(bh, new UnsafeInput(testData))
  @Benchmark
  def array_KryoBB(bh: Blackhole): Unit = run(bh, new ByteBufferInput(testData))
  @Benchmark
  def array_KryoBBUnsafe(bh: Blackhole): Unit = run(bh, new UnsafeByteBufferInput(testData))
  @Benchmark
  def array_ByteBuffer(bh: Blackhole): Unit = run(bh, ByteBuffer.wrap(testData))
  @Benchmark
  def array_BufferedInput(bh: Blackhole): Unit = run(bh, BufferedInput.of(new ByteArrayInputStream(testData)))
  @Benchmark
  def array_BufferedInput_fromArray(bh: Blackhole): Unit = run(bh, BufferedInput.ofArray(testData))

  @Benchmark
  def direct_BufferedInput_mapped(bh: Blackhole): Unit = run(bh, BufferedInput.ofMemorySegment(testDataGlobal))

  @Benchmark
  def mediumFile_DataInputStream(bh: Blackhole): Unit = run(bh, new DataInputStream(new BufferedInputStream(new FileInputStream(diskTestData))))
  @Benchmark
  def mediumFile_BufferedInput(bh: Blackhole): Unit = run(bh, BufferedInput.of(new FileInputStream(diskTestData)))
  @Benchmark
  def mediumFile_BufferedInput_mapped(bh: Blackhole): Unit = run(bh, BufferedInput.ofMappedFile(diskTestData.toPath))
