package perfio

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra._

import java.util.concurrent.TimeUnit
import com.google.protobuf.compiler.{PluginProtos => GPluginProtos}
import perfio.protoapi.{PluginProtos => PPluginProtos}

import java.io.{BufferedOutputStream, ByteArrayOutputStream, FileOutputStream}
import java.nio.ByteOrder
import java.nio.file.{Files, Path}

@BenchmarkMode(Array(Mode.AverageTime))
@Fork(value = 1, jvmArgsAppend = Array("-Xmx12g", "-Xss32M", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC", "--enable-native-access=ALL-UNNAMED", "--add-modules", "jdk.incubator.vector"))
@Threads(1)
@Warmup(iterations = 7, time = 1)
@Measurement(iterations = 7, time = 1)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
class WriteBenchmark:

  private var testData: Array[Byte] = null
  private val testDataFile = Path.of("src/main/resources/CodeGeneratorRequest-pack.bin")
  private var gReq: GPluginProtos.CodeGeneratorRequest = null
  private var pReq: PPluginProtos.CodeGeneratorRequest = null

  @Setup(Level.Trial)
  def buildTestData(): Unit =
    testData = Files.readAllBytes(testDataFile)
    assert(testData.length == 159366, s"testData.length ${testData.length}")
    gReq = GPluginProtos.CodeGeneratorRequest.parseFrom(testData)
    pReq = PPluginProtos.CodeGeneratorRequest.parseFrom(BufferedInput.ofArray(testData).order(ByteOrder.LITTLE_ENDIAN))

  @Benchmark
  def array_google(bh: Blackhole): Unit =
    val baos = new ByteArrayOutputStream()
    gReq.writeTo(baos)
    baos.close()
    bh.consume(baos.size())

  @Benchmark
  def array_perfio(bh: Blackhole): Unit =
    val bo = BufferedOutput.growing(4096)
    pReq.writeTo(bo)
    bo.close()
    bh.consume(bo.length())

  @Benchmark
  def file_google(bh: Blackhole): Unit =
    val out = new BufferedOutputStream(new FileOutputStream("/dev/null"))
    gReq.writeTo(out)
    out.close()

  @Benchmark
  def file_perfio(bh: Blackhole): Unit =
    val bo = BufferedOutput.ofFile(Path.of("/dev/null"), 8192)
    pReq.writeTo(bo)
    bo.close()
