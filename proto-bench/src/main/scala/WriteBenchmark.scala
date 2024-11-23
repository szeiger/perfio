package perfio

import com.google.protobuf.{CodedOutputStream, GeneratedMessage as GGeneratedMessage, Parser as GParser}
import perfio.proto.runtime.GeneratedMessage as PGeneratedMessage
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.*

import java.util.concurrent.TimeUnit
import com.google.protobuf.compiler.PluginProtos as GPluginProtos
import perfio.protoapi.PluginProtos as PPluginProtos

import java.io.{BufferedOutputStream, ByteArrayOutputStream, FileOutputStream, OutputStream}
import java.lang.invoke.{MethodHandles, MethodType}
import java.nio.ByteOrder
import java.nio.file.{Files, Path}

import scala.jdk.CollectionConverters._

@BenchmarkMode(Array(Mode.AverageTime))
@Fork(value = 1, jvmArgsAppend = Array("-Xmx12g", "-Xss32M", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC", "--enable-native-access=ALL-UNNAMED", "--add-modules", "jdk.incubator.vector"))
@Threads(1)
@Warmup(iterations = 15, time = 1)
@Measurement(iterations = 10, time = 1)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
class WriteBenchmark:

  private val testDataFile = Path.of("src/main/resources/CodeGeneratorRequest-pack.bin")
  private var testData: Array[Byte] = null
  private var gMessage: GGeneratedMessage = null
  private var pMessage: PGeneratedMessage = null

  private def p2g(p: PGeneratedMessage, gp: GParser[? <: GGeneratedMessage]): GGeneratedMessage = {
    val out = BufferedOutput.growing()
    p.writeTo(out)
    out.close()
    gp.parseFrom(out.toInputStream)
  }

  @Setup(Level.Trial)
  def buildTrialData(): Unit =
    testData = Files.readAllBytes(testDataFile)
    assert(testData.length == 159366, s"testData.length ${testData.length}")
    val m = PPluginProtos.CodeGeneratorRequest.parseFrom(BufferedInput.ofArray(testData).order(ByteOrder.LITTLE_ENDIAN))
    val pfs = m.getProtoFileList.asScala.toArray
    for i <- 1 to 10 do
      pfs.foreach(m.addProtoFile)
    val out = BufferedOutput.growing()
    m.writeTo(out)
    out.close()
    testData = out.copyToByteArray()

  @Setup(Level.Invocation)
  def buildInvocationData(): Unit =
    gMessage = GPluginProtos.CodeGeneratorRequest.parseFrom(testData)
    pMessage = PPluginProtos.CodeGeneratorRequest.parseFrom(BufferedInput.ofArray(testData).order(ByteOrder.LITTLE_ENDIAN))

  private def writeGoogle(out: OutputStream): Unit =
    val c = CodedOutputStream.newInstance(out, 4096)
    gMessage.writeTo(c)
    c.flush()
    out.close()

  private def writePerfio(out: BufferedOutput): Unit =
    var i = 0
    pMessage.writeTo(out)
    out.close()

//  @Benchmark
//  def array_google(bh: Blackhole): Unit =
//    val baos = new ByteArrayOutputStream()
//    writeGoogle(baos)
//    bh.consume(baos.size())

  @Benchmark
  def array_perfio(bh: Blackhole): Unit =
    val bo = BufferedOutput.growing(4096)
    writePerfio(bo)
    bh.consume(bo.length())

//  @Benchmark
//  def file_google(bh: Blackhole): Unit =
//    val out = new BufferedOutputStream(new FileOutputStream("/dev/null"))
//    writeGoogle(out)
//    out.close()
//
//  @Benchmark
//  def file_perfio(bh: Blackhole): Unit =
//    val bo = BufferedOutput.ofFile(Path.of("/dev/null"), 8192)
//    writePerfio(bo)
//    bo.close()
