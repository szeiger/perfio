package perfio

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra._

import java.util.concurrent.TimeUnit
import com.google.protobuf.compiler.{PluginProtos => GPluginProtos}
import perfio.protoapi.{PluginProtos => PPluginProtos}

import java.io.{BufferedInputStream, ByteArrayOutputStream, FileInputStream}
import java.nio.ByteOrder
import java.nio.file.{Files, Path}

@BenchmarkMode(Array(Mode.AverageTime))
@Fork(value = 1, jvmArgsAppend = Array("-Xmx12g", "-Xss32M", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC", "--enable-native-access=ALL-UNNAMED", "--add-modules", "jdk.incubator.vector"))
@Threads(1)
@Warmup(iterations = 7, time = 1)
@Measurement(iterations = 7, time = 1)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
class WriteBenchmark {

  private[this] var testData: Array[Byte] = _
  private[this] val testDataFile = Path.of("src/main/resources/CodeGeneratorRequest-pack.bin")
  private[this] var gReq: GPluginProtos.CodeGeneratorRequest = _
  private[this] var pReq: PPluginProtos.CodeGeneratorRequest = _

  @Setup(Level.Trial)
  def buildTestData(): Unit = {
    testData = Files.readAllBytes(testDataFile)
    assert(testData.length == 159366, s"testData.length ${testData.length}")
    gReq = GPluginProtos.CodeGeneratorRequest.parseFrom(testData)
    pReq = PPluginProtos.CodeGeneratorRequest.parseFrom(BufferedInput.ofArray(testData).order(ByteOrder.LITTLE_ENDIAN))
  }

  @Benchmark
  def array_google(bh: Blackhole): Unit = {
    val baos = new ByteArrayOutputStream()
    gReq.writeTo(baos)
    baos.close()
    bh.consume(baos.size())
  }

  @Benchmark
  def array_perfio(bh: Blackhole): Unit = {
    val bo = BufferedOutput.growing(32)
    pReq.writeTo(bo)
    bo.close()
    bh.consume(bo.length())
  }
}
