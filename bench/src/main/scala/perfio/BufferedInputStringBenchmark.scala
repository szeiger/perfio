package perfio

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.*

import java.io.*
import java.util.concurrent.TimeUnit

@BenchmarkMode(Array(Mode.AverageTime))
@Fork(value = 1, jvmArgsAppend = Array("-Xmx12g", "-Xss32M", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC", "--enable-native-access=ALL-UNNAMED", "--add-modules", "jdk.incubator.vector"))
@Threads(1)
@Warmup(iterations = 7, time = 1)
@Measurement(iterations = 7, time = 1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
class BufferedInputStringBenchmark extends BenchUtil:

  private var testData: Array[Byte] = null
  private var diskTestData: File = null

  val count = 10000000

  @Param(Array("false", "true"))
  var mixedWarmUp: Boolean = true

  @Setup(Level.Trial)
  def buildTestData(): Unit =
    val out = new ByteArrayOutputStream()
    val dout = new DataOutputStream(out)
    var i = 0
    while(i < count)
      dout.writeUTF("abcdefghijklmnopqrstuvwxyz")
      i += 1
    testData = out.toByteArray
    diskTestData = writeFileIfMissing("strings", testData)
    if(mixedWarmUp) runWarmup: bh =>
      var i = 0
      while(i < 5)
        run(bh, BufferedInput.ofArray(testData))
        run(bh, BufferedInput.ofMappedFile(diskTestData.toPath))
        i += 1

  private def run(bh: Blackhole, bin: BufferedInput): Unit =
    var i = 0
    while(i < count)
      val len = bin.uint16()
      bh.consume(bin.string(len))
      i += 1
    bin.close()
  
  private def run(bh: Blackhole, din: DataInputStream): Unit =
    var i = 0
    while(i < count)
      bh.consume(din.readUTF())
      i += 1
    din.close()

  @Benchmark
  def array_DataInputStream(bh: Blackhole): Unit =
    run(bh, new DataInputStream(new ByteArrayInputStream(testData)))

  @Benchmark
  def array_BufferedInput(bh: Blackhole): Unit =
    run(bh, BufferedInput.of(new ByteArrayInputStream(testData)))

  @Benchmark
  def array_BufferedInput_fromArray(bh: Blackhole): Unit =
    run(bh, BufferedInput.ofArray(testData))

  @Benchmark
  def file_BufferedInput(bh: Blackhole): Unit =
    run(bh, BufferedInput.of(new FileInputStream(diskTestData)))

  @Benchmark
  def file_BufferedInput_mapped(bh: Blackhole): Unit =
    run(bh, BufferedInput.ofMappedFile(diskTestData.toPath))
