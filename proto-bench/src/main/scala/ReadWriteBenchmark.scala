package perfio;

import com.google.protobuf.compiler.PluginProtos as GPluginProtos
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.*
import perfio.protoapi.PluginProtos as PPluginProtos

import java.io.{BufferedInputStream, BufferedOutputStream, ByteArrayOutputStream, FileInputStream, FileOutputStream}
import java.nio.ByteOrder
import java.nio.file.{Files, Path}
import java.util.concurrent.TimeUnit

@BenchmarkMode(Array(Mode.AverageTime))
@Fork(value = 1, jvmArgsAppend = Array("-Xmx12g", "-Xss32M", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC", "--enable-native-access=ALL-UNNAMED", "--add-modules", "jdk.incubator.vector"))
@Threads(1)
@Warmup(iterations = 7, time = 1)
@Measurement(iterations = 7, time = 1)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
class ReadWriteBenchmark:

  private var testData: Array[Byte] = null
  private val testDataFile = Path.of("src/main/resources/CodeGeneratorRequest-pack.bin")

  @Setup(Level.Trial)
  def buildTestData(): Unit =
    testData = Files.readAllBytes(testDataFile)
    assert(testData.length == 159366, s"testData.length ${testData.length}")

  @Benchmark
  def array_google(bh: Blackhole): Unit =
    val p = GPluginProtos.CodeGeneratorRequest.parseFrom(testData)
    val out = new ByteArrayOutputStream()
    p.writeTo(out)
    out.close()
    bh.consume(out.size())

  @Benchmark
  def array_perfio(bh: Blackhole): Unit =
    val p = PPluginProtos.CodeGeneratorRequest.parseFrom(BufferedInput.ofArray(testData).order(ByteOrder.LITTLE_ENDIAN))
    val bo = BufferedOutput.growing(4096)
    p.writeTo(bo)
    bo.close()
    bh.consume(bo.length())

  @Benchmark
  def file_google(bh: Blackhole): Unit =
    val in = new BufferedInputStream(new FileInputStream(testDataFile.toFile))
    val p = GPluginProtos.CodeGeneratorRequest.parseFrom(in)
    in.close()
    val out = new BufferedOutputStream(new FileOutputStream("/dev/null"))
    p.writeTo(out)
    out.close()

  @Benchmark
  def file_perfio(bh: Blackhole): Unit =
    val in = BufferedInput.of(new FileInputStream(testDataFile.toFile)).order(ByteOrder.LITTLE_ENDIAN)
    val p = PPluginProtos.CodeGeneratorRequest.parseFrom(in)
    in.close()
    val bo = BufferedOutput.ofFile(Path.of("/dev/null"), 8192)
    p.writeTo(bo)
    bo.close()

  @Benchmark
  def file_perfio_mapped(bh: Blackhole): Unit =
    val in = BufferedInput.ofMappedFile(testDataFile).order(ByteOrder.LITTLE_ENDIAN)
    val p = PPluginProtos.CodeGeneratorRequest.parseFrom(in)
    in.close()
    val bo = BufferedOutput.ofFile(Path.of("/dev/null"), 8192)
    p.writeTo(bo)
    bo.close()
