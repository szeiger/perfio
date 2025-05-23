package perfio

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.*

import java.io.*
import java.nio.charset.Charset
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

@BenchmarkMode(Array(Mode.AverageTime))
@Fork(value = 1, jvmArgsAppend = Array("-Xmx12g", "-Xss32M", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC", "--enable-native-access=ALL-UNNAMED", "--add-modules", "jdk.incubator.vector"))
@Threads(1)
@Warmup(iterations = 12, time = 1)
@Measurement(iterations = 10, time = 1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
class BufferedOutputStringBenchmark extends BenchUtil:

  val count = 10000000
  val stringData = "abcdefghijklmnopqrstuvwxyz"
  val totalLength = count * stringData.length

  @Param(Array("UTF-8", "ISO-8859-1"))
  var charset: String = null
  var cs: Charset = null

  @Setup(Level.Trial)
  def setup(): Unit =
    cs = Charset.forName(charset)

  private def writeTo(out: Writer): Unit =
    var i = 0
    while i < count do
      out.write(stringData)
      i += 1
    out.close()

  private def writeTo(out: PrintStream): Unit =
    var i = 0
    while i < count do
      out.print(stringData)
      i += 1
    out.close()

  private def writeTo(out: BufferedOutput): Unit =
    var i = 0
    while i < count do
      out.string(stringData, cs)
      i += 1
    out.close()

  @Benchmark
  def array_PrintStream_growing(bh: Blackhole): Unit =
    val bout = new MyByteArrayOutputStream
    val out = new PrintStream(bout, false, cs)
    writeTo(out)
    bh.consume(bout.getSize)
    bh.consume(bout.getBuffer)

  @Benchmark
  def array_PrintStream_preallocated(bh: Blackhole): Unit =
    val bout = new MyByteArrayOutputStream(totalLength)
    val out = new PrintStream(bout, false, cs)
    writeTo(out)
    bh.consume(bout.getSize)
    bh.consume(bout.getBuffer)

  @Benchmark
  def array_OutputStreamWriter_growing(bh: Blackhole): Unit =
    val bout = new MyByteArrayOutputStream
    val out = new OutputStreamWriter(bout, cs)
    writeTo(out)
    bh.consume(bout.getSize)
    bh.consume(bout.getBuffer)

  @Benchmark
  def array_OutputStreamWriter_preallocated(bh: Blackhole): Unit =
    val bout = new MyByteArrayOutputStream(totalLength)
    val out = new OutputStreamWriter(bout, cs)
    writeTo(out)
    bh.consume(bout.getSize)
    bh.consume(bout.getBuffer)

  @Benchmark
  def array_FlushingBufferedOutput_growing(bh: Blackhole): Unit =
    val bout = new MyByteArrayOutputStream
    val out = BufferedOutput.of(bout)
    writeTo(out)
    bh.consume(bout.getSize)
    bh.consume(bout.getBuffer)

  @Benchmark
  def array_FlushingBufferedOutput_fixed(bh: Blackhole): Unit =
    val bout = new MyByteArrayOutputStream(totalLength)
    val out = BufferedOutput.of(bout)
    writeTo(out)
    bh.consume(bout.getSize)
    bh.consume(bout.getBuffer)

  @Benchmark
  def array_ArrayBufferedOutput_growing(bh: Blackhole): Unit =
    val out = BufferedOutput.growing()
    writeTo(out)
    bh.consume(out.buffer)
    bh.consume(out.length)

  @Benchmark
  def array_ArrayBufferedOutput_growing_preallocated(bh: Blackhole): Unit =
    val out = BufferedOutput.growing(totalLength)
    writeTo(out)
    bh.consume(out.buffer)
    bh.consume(out.length)

  @Benchmark
  def array_ArrayBufferedOutput_fixed(bh: Blackhole): Unit =
    val out = BufferedOutput.ofArray(new Array[Byte](totalLength))
    writeTo(out)
    bh.consume(out.buffer)
    bh.consume(out.length)

  @Benchmark
  def file_PrintStream(bh: Blackhole): Unit =
    val fout = new FileOutputStream("/dev/null")
    val out = new PrintStream(fout, false, cs)
    writeTo(out)

  @Benchmark
  def file_FileWriter(bh: Blackhole): Unit =
    val fout = new FileWriter("/dev/null", cs)
    val out = new BufferedWriter(fout)
    writeTo(out)

  @Benchmark
  def file_FlushingBufferedOutput(bh: Blackhole): Unit =
    val out = BufferedOutput.ofFile(Paths.get("/dev/null"))
    writeTo(out)
