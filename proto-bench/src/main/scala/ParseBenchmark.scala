package perfio

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra._

import java.util.concurrent.TimeUnit
import com.google.protobuf.compiler.{PluginProtos => GPluginProtos}
import perfio.protoapi.{PluginProtos => PPluginProtos}

import java.io.{BufferedInputStream, FileInputStream}
import java.nio.ByteOrder
import java.nio.file.{Files, Path}

@BenchmarkMode(Array(Mode.AverageTime))
@Fork(value = 1, jvmArgsAppend = Array("-Xmx12g", "-Xss32M", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC", "--enable-native-access=ALL-UNNAMED", "--add-modules", "jdk.incubator.vector"))
@Threads(1)
@Warmup(iterations = 7, time = 1)
@Measurement(iterations = 7, time = 1)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
class ParseBenchmark {

  private[this] var testData: Array[Byte] = _
  private[this] val testDataFile = Path.of("src/main/resources/CodeGeneratorRequest-pack.bin")

  @Setup(Level.Trial)
  def buildTestData(): Unit = {
    testData = Files.readAllBytes(testDataFile)
    assert(testData.length == 159366, s"testData.length ${testData.length}")
  }

  @Benchmark
  def array_google(bh: Blackhole): Unit = {
    val p = GPluginProtos.CodeGeneratorRequest.parseFrom(testData)
    assert(p.getFileToGenerateList.size() == 2)
    assert(p.getSourceFileDescriptorsList.size() == 2)
    bh.consume(p)
  }

  @Benchmark
  def array_perfio(bh: Blackhole): Unit = {
    val p = PPluginProtos.CodeGeneratorRequest.parseFrom(BufferedInput.ofArray(testData).order(ByteOrder.LITTLE_ENDIAN))
    assert(p.getFileToGenerateList.size() == 2)
    assert(p.getSourceFileDescriptorsList.size() == 2)
    bh.consume(p)
  }

  @Benchmark
  def file_google(bh: Blackhole): Unit = {
    val in = new BufferedInputStream(new FileInputStream(testDataFile.toFile))
    val p = GPluginProtos.CodeGeneratorRequest.parseFrom(in)
    assert(p.getFileToGenerateList.size() == 2)
    assert(p.getSourceFileDescriptorsList.size() == 2)
    bh.consume(p)
    in.close()
  }

  @Benchmark
  def file_perfio(bh: Blackhole): Unit = {
    val in = BufferedInput.of(new FileInputStream(testDataFile.toFile)).order(ByteOrder.LITTLE_ENDIAN)
    val p = PPluginProtos.CodeGeneratorRequest.parseFrom(in)
    assert(p.getFileToGenerateList.size() == 2)
    assert(p.getSourceFileDescriptorsList.size() == 2)
    bh.consume(p)
    in.close()
  }

  @Benchmark
  def file_perfio_mapped(bh: Blackhole): Unit = {
    val p = PPluginProtos.CodeGeneratorRequest.parseFrom(BufferedInput.ofMappedFile(testDataFile).order(ByteOrder.LITTLE_ENDIAN))
    assert(p.getFileToGenerateList.size() == 2)
    assert(p.getSourceFileDescriptorsList.size() == 2)
    bh.consume(p)
  }
}
