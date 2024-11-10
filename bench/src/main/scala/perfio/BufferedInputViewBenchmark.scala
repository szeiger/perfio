package perfio

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.*

import java.io.*
import java.util.concurrent.TimeUnit

import com.google.common.io.ByteStreams

@BenchmarkMode(Array(Mode.AverageTime))
@Fork(value = 1, jvmArgsAppend = Array("-Xmx12g", "-Xss32M", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC", "--enable-native-access=ALL-UNNAMED", "--add-modules", "jdk.incubator.vector"))
@Threads(1)
@Warmup(iterations = 7, time = 1)
@Measurement(iterations = 7, time = 1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
class BufferedInputViewBenchmark extends BenchUtil:

  private var testData: Array[Byte] = null
  private var diskTestDataMedium: File = null
  val count = 20000000

  @Setup(Level.Trial)
  def buildTestData(): Unit =
    val out = new ByteArrayOutputStream()
    val dout = new DataOutputStream(out)
    var i = 0
    while i < count do
      dout.writeByte(i)
      dout.writeInt(i+100)
      dout.writeLong(i+101)
      i += 1
    testData = out.toByteArray
    diskTestDataMedium = writeFileIfMissing("medium", testData)

  private def runDirect(bh: Blackhole, bin: BufferedInput): Unit =
    var i = 0
    val end = count/10
    while i < end do
      var j = 0
      while j < 10 do
        bh.consume(bin.int8())
        bh.consume(bin.int32())
        bh.consume(bin.int64())
        j += 1
      i += 1
    bin.close()

  private def runCounting(bh: Blackhole, bin: BufferedInput): Unit =
    var i = 0
    val end = count/10
    while i < end do
      val end = bin.totalBytesRead + 130
      while bin.totalBytesRead < end do
        bh.consume(bin.int8())
        bh.consume(bin.int32())
        bh.consume(bin.int64())
      i += 1
    bin.close()

  private def runView(bh: Blackhole, bin: BufferedInput): Unit =
    var i = 0
    val end = count/10
    while i < end do
      val v = bin.delimitedView(130)
      while v.hasMore do
        bh.consume(v.int8())
        bh.consume(v.int32())
        bh.consume(v.int64())
      v.close()
      i += 1
    bin.close()

  private def runDataInputStream(bh: Blackhole, in: InputStream): Unit =
    val din = new DataInputStream(in)
    var i = 0
    val end = count/10
    while i < end do
      val v = new DataInputStream(ByteStreams.limit(in, 130))
      try
        while true do
          bh.consume(v.readByte())
          bh.consume(v.readInt())
          bh.consume(v.readLong())
      catch case _: EOFException => ()
      i += 1
    din.close()

  @Benchmark
  def array_LimitedInputStream(bh: Blackhole): Unit = runDataInputStream(bh, new ByteArrayInputStream(testData))
  @Benchmark
  def array_fromInputStream_direct(bh: Blackhole): Unit = runDirect(bh, BufferedInput.of(new ByteArrayInputStream(testData)))
  @Benchmark
  def array_fromInputStream_counting(bh: Blackhole): Unit = runCounting(bh, BufferedInput.of(new ByteArrayInputStream(testData)))
  @Benchmark
  def array_fromInputStream_view(bh: Blackhole): Unit = runView(bh, BufferedInput.of(new ByteArrayInputStream(testData)))
  @Benchmark
  def array_fromArray_direct(bh: Blackhole): Unit = runDirect(bh, BufferedInput.ofArray(testData))
  @Benchmark
  def array_fromArray_counting(bh: Blackhole): Unit = runCounting(bh, BufferedInput.ofArray(testData))
  @Benchmark
  def array_fromArray_view(bh: Blackhole): Unit = runView(bh, BufferedInput.ofArray(testData))

  @Benchmark
  def mediumFile_LimitedInputStream(bh: Blackhole): Unit = runDataInputStream(bh, new BufferedInputStream(new FileInputStream(diskTestDataMedium)))
  @Benchmark
  def mediumFile_fromInputStream_direct(bh: Blackhole): Unit = runDirect(bh, BufferedInput.of(new BufferedInputStream(new FileInputStream(diskTestDataMedium))))
  @Benchmark
  def mediumFile_fromInputStream_counting(bh: Blackhole): Unit = runCounting(bh, BufferedInput.of(new BufferedInputStream(new FileInputStream(diskTestDataMedium))))
  @Benchmark
  def mediumFile_fromInputStream_view(bh: Blackhole): Unit = runView(bh, BufferedInput.of(new BufferedInputStream(new FileInputStream(diskTestDataMedium))))
  @Benchmark
  def mediumFile_mapped_direct(bh: Blackhole): Unit = runDirect(bh, BufferedInput.ofMappedFile(diskTestDataMedium.toPath))
  @Benchmark
  def mediumFile_mapped_counting(bh: Blackhole): Unit = runCounting(bh, BufferedInput.ofMappedFile(diskTestDataMedium.toPath))
  @Benchmark
  def mediumFile_mapped_view(bh: Blackhole): Unit = runView(bh, BufferedInput.ofMappedFile(diskTestDataMedium.toPath))
