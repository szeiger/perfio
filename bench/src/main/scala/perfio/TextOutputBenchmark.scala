package perfio

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.*
import perfio.internal.StringInternals

import java.io.*
import java.nio.charset.Charset
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

@BenchmarkMode(Array(Mode.AverageTime))
@Fork(value = 1, jvmArgsAppend = Array("-Xmx12g", "-Xss32M", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC", "--enable-native-access=ALL-UNNAMED", "--add-modules", "jdk.incubator.vector"))
@Threads(1)
@Warmup(iterations = 15, time = 1)
@Measurement(iterations = 10, time = 1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
class TextOutputBenchmark extends BenchUtil:

  val count = 10000000
  val stringData = "abcdefghijklmnopqrstuvwxyz"
  val eol = System.lineSeparator()
  val totalLength = count * (stringData.length + eol.length)
  val autoFlush = false

  //@Param(Array("UTF-8", "UTF-8-internal", "ASCII", "ASCII-internal", "Latin1", "Latin1-internal", "UTF-16"))
  //@Param(Array("Latin1", "Latin1-internal"))
  //@Param(Array("ASCII", "ASCII-internal"))
  @Param(Array("Latin1-internal"))
  var charset: String = null
  var cs: Charset = null

  //@Param(Array("array", "file"))
  @Param(Array("file"))
  var output: String = null

  //@Param(Array("PrintWriter", "TextOutput"))
  @Param(Array("TextOutput"))
  var mode: String = null

  @Setup(Level.Trial)
  def setup(): Unit =
    if(charset.endsWith("-internal"))
      if(mode == "PrintWriter") throw new IllegalArgumentException()
      sys.props.put("perfio.disableStringInternals", "false")
      if(StringInternals.internalAccessError != null) throw StringInternals.internalAccessError
      assert(StringInternals.internalAccessEnabled)
      charset = charset.substring(0, charset.length - 9)
    else
      sys.props.put("perfio.disableStringInternals", "true")
      assert(!StringInternals.internalAccessEnabled)
    cs = Charset.forName(charset)

  private def printlnString(out: PrintWriter): Unit =
    var i = 0
    while i < count do
      out.println(stringData)
      i += 1

  private def printlnString(out: TextOutput): Unit =
    var i = 0
    while i < count do
      out.println(stringData)
      i += 1

  private def printlnNull(out: PrintWriter): Unit =
    var i = 0
    while i < count/2 do
      out.println(null: String)
      out.println(null: String)
      i += 1

  private def printlnNull(out: TextOutput): Unit =
    var i = 0
    while i < count/2 do
      out.println(null: String)
      out.println(null: String)
      i += 1

  private def printlnInt(out: PrintWriter): Unit =
    var i = 0
    while i < count/200000 do
      var j = 0
      while j <= 200000 do
        out.println(j)
        j += 1
      i += 1

  private def printlnInt(out: TextOutput): Unit =
    var i = 0
    while i < count/200000 do
      var j = 0
      while j <= 200000 do
        out.println(j)
        j += 1
      i += 1

  private def printlnLong(out: PrintWriter): Unit =
    var i = 0
    while i < count/200000 do
      var j = 0
      while j <= 200000 do
        out.println(j.toLong)
        j += 1
      i += 1

  private def printlnLong(out: TextOutput): Unit =
    var i = 0
    while i < count/200000 do
      var j = 0
      while j <= 200000 do
        out.println(j.toLong)
        j += 1
      i += 1

  private def printlnBoolean(out: PrintWriter): Unit =
    var i = 0
    while i < count/2 do
      out.println(true)
      out.println(false)
      i += 1

  private def printlnBoolean(out: TextOutput): Unit =
    var i = 0
    while i < count/2 do
      out.println(true)
      out.println(false)
      i += 1

  private def printlnChar(out: PrintWriter): Unit =
    var i = 0
    while i < count/100 do
      var j = 0
      while j <= 100 do
        out.println(j.toChar)
        j += 1
      i += 1

  private def printlnChar(out: TextOutput): Unit =
    var i = 0
    while i < count/100 do
      var j = 0
      while j <= 100 do
        out.println(j.toChar)
        j += 1
      i += 1

  private def printlnUnit(out: PrintWriter): Unit =
    var i = 0
    while i < count do
      out.println()
      out.println()
      i += 1

  private def printlnUnit(out: TextOutput): Unit =
    var i = 0
    while i < count do
      out.println()
      out.println()
      i += 1

  private def printChar(out: PrintWriter): Unit =
    var i = 0
    while i < count/50 do
      var j = 0
      while j <= 100 do
        out.print(j.toChar)
        j += 1
      i += 1

  private def printChar(out: TextOutput): Unit =
    var i = 0
    while i < count/50 do
      var j = 0
      while j <= 100 do
        out.print(j.toChar)
        j += 1
      i += 1

  private def runPrintWriter(bh: Blackhole, f: PrintWriter => Unit): Unit =
    var bout: MyByteArrayOutputStream = null
    val fout = if(output == "file") new FileOutputStream("/dev/null")
    else
      bout = new MyByteArrayOutputStream(totalLength)
      bout
    // Adding a BufferedWriter may seem counter-productive when writing to a ByteArrayOutputStream
    // but the extra buffering before character encoding does improve performance.
    val out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(fout, cs)), autoFlush)
    f(out)
    out.close()
    if(bout != null)
      bh.consume(bout.getBuffer)
      bh.consume(bout.getSize)

  private def runTextOutput(bh: Blackhole, f: TextOutput => Unit): Unit =
    var fbout: ArrayBufferedOutput = null
    val bout = if(output == "file") BufferedOutput.ofFile(Paths.get("/dev/null"))
    else
      fbout = BufferedOutput.growing(totalLength)
      fbout
    val tout = TextOutput.of(bout, cs, System.lineSeparator()).autoFlush(autoFlush)
    f(tout)
    tout.close()
    if(fbout != null)
      bh.consume(fbout.buffer)
      bh.consume(fbout.length)

  private def run(bh: Blackhole)(f: PrintWriter => Unit)(g: TextOutput => Unit): Unit =
    if(mode == "PrintWriter") runPrintWriter(bh, f)
    else runTextOutput(bh, g)

  @Benchmark
  def println_String(bh: Blackhole): Unit = run(bh)(printlnString)(printlnString)

  @Benchmark
  def println_null(bh: Blackhole): Unit = run(bh)(printlnNull)(printlnNull)

  @Benchmark
  def println_Int(bh: Blackhole): Unit = run(bh)(printlnInt)(printlnInt)

  @Benchmark
  def println_Long(bh: Blackhole): Unit = run(bh)(printlnLong)(printlnLong)

  @Benchmark
  def println_Boolean(bh: Blackhole): Unit = run(bh)(printlnBoolean)(printlnBoolean)

  @Benchmark
  def println_Char(bh: Blackhole): Unit = run(bh)(printlnChar)(printlnChar)

  @Benchmark
  def print_Char(bh: Blackhole): Unit = run(bh)(printChar)(printChar)

  @Benchmark
  def println_unit(bh: Blackhole): Unit = run(bh)(printlnUnit)(printlnUnit)
