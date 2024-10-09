package perfio

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra._

import java.io._
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

@BenchmarkMode(Array(Mode.AverageTime))
@Fork(value = 1, jvmArgsAppend = Array("-Xmx12g", "-Xss32M", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC", "--enable-native-access=ALL-UNNAMED", "--add-modules", "jdk.incubator.vector"))
@Threads(1)
@Warmup(iterations = 12, time = 1)
@Measurement(iterations = 10, time = 1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
class TextOutputBenchmark extends BenchUtil {

  val count = 10000000
  val stringData = "abcdefghijklmnopqrstuvwxyz"
  val eol = System.lineSeparator()
  val totalLength = count * (stringData.length + eol.length)
  val autoFlush = false

  //@Param(Array("UTF-8", "ASCII", "Latin1", "Latin1-fast", "UTF-16"))
  @Param(Array("Latin1"))
  var charset: String = _
  var cs: Charset = _
  var strictUnicode = true

  //@Param(Array("array", "file"))
  @Param(Array("array"))
  var output: String = _

  @Param(Array("PrintWriter", "TextOutput"))
  //@Param(Array("TextOutput"))
  var mode: String = _

  @Setup(Level.Trial)
  def setup(): Unit = {
    if(charset == "Latin1-fast") {
      if(mode == "PrintWriter") throw new IllegalArgumentException()
      cs = StandardCharsets.ISO_8859_1
      strictUnicode = false
    } else cs = Charset.forName(charset)
  }

  private[this] def printlnString(out: PrintWriter): Unit = {
    var i = 0
    while(i < count) {
      out.println(stringData)
      i += 1
    }
  }

  private[this] def printlnString(out: TextOutput): Unit = {
    var i = 0
    while(i < count) {
      out.println(stringData)
      i += 1
    }
  }

  private[this] def printlnNull(out: PrintWriter): Unit = {
    var i = 0
    while(i < count) {
      out.println(null: String)
      out.println(null: String)
      i += 1
    }
  }

  private[this] def printlnNull(out: TextOutput): Unit = {
    var i = 0
    while(i < count) {
      out.println(null: String)
      out.println(null: String)
      i += 1
    }
  }

  private[this] def printlnInt(out: PrintWriter): Unit = {
    var i = 0
    while(i < count/100000) {
      var j = 0
      while(j <= 200000) {
        out.println(j)
        j += 1
      }
      i += 1
    }
  }

  private[this] def printlnInt(out: TextOutput): Unit = {
    var i = 0
    while(i < count/100000) {
      var j = 0
      while(j <= 200000) {
        out.println(j)
        j += 1
      }
      i += 1
    }
  }

  private[this] def printlnLong(out: PrintWriter): Unit = {
    var i = 0
    while(i < count/100000) {
      var j = 0
      while(j <= 200000) {
        out.println(j.toLong)
        j += 1
      }
      i += 1
    }
  }

  private[this] def printlnLong(out: TextOutput): Unit = {
    var i = 0
    while(i < count/100000) {
      var j = 0
      while(j <= 200000) {
        out.println(j.toLong)
        j += 1
      }
      i += 1
    }
  }

  private[this] def printlnBoolean(out: PrintWriter): Unit = {
    var i = 0
    while(i < count) {
      out.println(true)
      out.println(false)
      i += 1
    }
  }

  private[this] def printlnBoolean(out: TextOutput): Unit = {
    var i = 0
    while(i < count) {
      out.println(true)
      out.println(false)
      i += 1
    }
  }

  private[this] def printlnChar(out: PrintWriter): Unit = {
    var i = 0
    while(i < count/100) {
      var j = 0
      while(j <= 100) {
        out.println(j.toChar)
        j += 1
      }
      i += 1
    }
  }

  private[this] def printlnChar(out: TextOutput): Unit = {
    var i = 0
    while(i < count/100) {
      var j = 0
      while(j <= 100) {
        out.println(j.toChar)
        j += 1
      }
      i += 1
    }
  }

  private[this] def printlnUnit(out: PrintWriter): Unit = {
    var i = 0
    while(i < count) {
      out.println()
      i += 1
    }
  }

  private[this] def printlnUnit(out: TextOutput): Unit = {
    var i = 0
    while(i < count) {
      out.println()
      i += 1
    }
  }

  private[this] def printChar(out: PrintWriter): Unit = {
    var i = 0
    while(i < count/100) {
      var j = 0
      while(j <= 100) {
        out.print(j.toChar)
        j += 1
      }
      i += 1
    }
  }

  private[this] def printChar(out: TextOutput): Unit = {
    var i = 0
    while(i < count/100) {
      var j = 0
      while(j <= 100) {
        out.print(j.toChar)
        j += 1
      }
      i += 1
    }
  }

  private[this] def runPrintWriter(bh: Blackhole, f: PrintWriter => Unit): Unit = {
    var bout: MyByteArrayOutputStream = null
    val fout = if(output == "file") new FileOutputStream("/dev/null")
    else {
      bout = new MyByteArrayOutputStream(totalLength)
      bout
    }
    // Adding a BufferedWriter may seem counter-productive when writing to a ByteArrayOutputStream
    // but the extra buffering before character encoding does improve performance.
    val out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(fout, cs)), autoFlush)
    f(out)
    out.close()
    if(bout != null) {
      bh.consume(bout.getBuffer)
      bh.consume(bout.getSize)
    }
  }

  private[this] def runTextOutput(bh: Blackhole, f: TextOutput => Unit): Unit = {
    var fbout: FullyBufferedOutput = null
    val bout = if(output == "file") BufferedOutput.ofFile(Paths.get("/dev/null"))
    else {
      fbout = BufferedOutput.growing(initialBufferSize = totalLength)
      fbout
    }
    val tout =
      if((cs eq StandardCharsets.ISO_8859_1) && !strictUnicode) TextOutput.fastLatin1(bout, autoFlush = autoFlush)
      else TextOutput(bout, cs, autoFlush = autoFlush)
    f(tout)
    tout.close()
    if(fbout != null) {
      bh.consume(fbout.getBuffer)
      bh.consume(fbout.getLength)
    }
  }

  private[this] def run(bh: Blackhole)(f: PrintWriter => Unit)(g: TextOutput => Unit): Unit =
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
}
