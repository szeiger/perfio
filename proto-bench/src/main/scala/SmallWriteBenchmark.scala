package perfio

import com.google.protobuf.compiler.PluginProtos as GPluginProtos
import com.google.protobuf.{CodedOutputStream, GeneratedMessage as GGeneratedMessage, Parser as GParser}
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.*
import perfio.proto.runtime.GeneratedMessage as PGeneratedMessage
import perfio.protoapi.PluginProtos as PPluginProtos

import java.io.{BufferedOutputStream, ByteArrayOutputStream, FileOutputStream, OutputStream}
import java.lang.invoke.{MethodHandles, MethodType}
import java.nio.file.Path
import java.util.concurrent.TimeUnit

@BenchmarkMode(Array(Mode.AverageTime))
@Fork(value = 1, jvmArgsAppend = Array("-Xmx12g", "-Xss32M", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC", "--enable-native-access=ALL-UNNAMED", "--add-modules", "jdk.incubator.vector"))
@Threads(1)
@Warmup(iterations = 15, time = 1)
@Measurement(iterations = 10, time = 1)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
class SmallWriteBenchmark:

  @Param(Array("version1000"))
  var testCase: String = null

  private var gMessage: GGeneratedMessage = null
  private var pMessage: PGeneratedMessage = null
  private var count = 1;

  private def p2g(p: PGeneratedMessage, gp: GParser[? <: GGeneratedMessage]): GGeneratedMessage = {
    val out = BufferedOutput.growing()
    p.writeTo(out)
    out.close()
    gp.parseFrom(out.toInputStream)
  }

  val clearMemoMethod = {
    val lookup = MethodHandles.privateLookupIn(classOf[GGeneratedMessage], MethodHandles.lookup())
    lookup.findVirtual(classOf[GGeneratedMessage], "setMemoizedSerializedSize", MethodType.methodType(Void.TYPE, Integer.TYPE))
  }

  private def clearMemo(m: GGeneratedMessage): Unit = {
    clearMemoMethod.invokeExact(m, -1)
  }

  @Setup(Level.Trial)
  def buildInvocationData(): Unit =
    testCase match
      case "version1000" =>
        pMessage = (new PPluginProtos.Version).setMajor(1).setMinor(2).setPatch(3) //.setSuffix("foo")
        gMessage = p2g(pMessage, GPluginProtos.Version.parser())
        count = 1000
      case "versionString1000" =>
        pMessage = (new PPluginProtos.Version).setMajor(1).setMinor(2).setPatch(3).setSuffix("foo")
        gMessage = p2g(pMessage, GPluginProtos.Version.parser())
        count = 1000
      case "versionString1" =>
        pMessage = (new PPluginProtos.Version).setMajor(1).setMinor(2).setPatch(3).setSuffix("foo")
        gMessage = p2g(pMessage, GPluginProtos.Version.parser())
        count = 1
      case "strings" =>
        val r = new PPluginProtos.CodeGeneratorRequest
        for(i <- 1 to 1000) r.addFileToGenerate(s"file $i")
        pMessage = r
        gMessage = p2g(pMessage, GPluginProtos.CodeGeneratorRequest.parser())
        count = 10
      case "nested" =>
        val r = new PPluginProtos.CodeGeneratorRequest
        r.setCompilerVersion((new PPluginProtos.Version).setMajor(1).setMinor(2).setPatch(3).setSuffix("foo"))
        pMessage = r
        gMessage = p2g(pMessage, GPluginProtos.CodeGeneratorRequest.parser())
        count = 100

  private def writeGoogle(out: OutputStream): Unit =
    val c = CodedOutputStream.newInstance(out, 4096)
    var i = 0
    while i < count do
      //clearMemo(gMessage.asInstanceOf[GPluginProtos.CodeGeneratorRequest].getCompilerVersion)
      gMessage.writeTo(c)
      i += 1
    c.flush()
    out.close()

  private def writePerfio(out: BufferedOutput): Unit =
    var i = 0
    while i < count do
      pMessage.writeTo(out)
      i += 1
    out.close()

  @Benchmark
  def array_google(bh: Blackhole): Unit =
    val baos = new ByteArrayOutputStream()
    writeGoogle(baos)
    bh.consume(baos.size())

  @Benchmark
  def array_perfio(bh: Blackhole): Unit =
    val bo = BufferedOutput.growing(4096)
    writePerfio(bo)
    bh.consume(bo.length())

  @Benchmark
  def file_google(bh: Blackhole): Unit =
    val out = new BufferedOutputStream(new FileOutputStream("/dev/null"))
    writeGoogle(out)
    out.close()

  @Benchmark
  def file_perfio(bh: Blackhole): Unit =
    val bo = BufferedOutput.ofFile(Path.of("/dev/null"), 8192)
    writePerfio(bo)
    bo.close()
