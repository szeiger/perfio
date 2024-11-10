package perfio

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import scala.jdk.CollectionConverters.*

import java.io.{ByteArrayOutputStream, PrintWriter}
import java.nio.charset.{Charset, StandardCharsets}

@RunWith(classOf[Parameterized])
class TextOutputTest(_name: String, cs: Charset, eol: String) extends TestUtil:

  def check(cs: Charset, eol: String, msg: String)(f: TextOutput => Unit)(g: PrintWriter => Unit): Unit =
    val bout = BufferedOutput.growing()
    val tout = bout.text(cs, eol)
    f(tout)
    tout.close()
    val buf = bout.copyToByteArray
    val ba = new ByteArrayOutputStream()
    val pw = new PrintWriter(ba, false, cs)
    g(pw)
    pw.close()
    val exp = ba.toByteArray
    assertEquals(s"$msg\nexpected:\n  >${new String(exp, cs)}<\ngot:\n  >${new String(buf, cs)}<\n\n", exp.toVector.map(b => b & 0xFF), buf.toVector.map(b => b & 0xFF))

  val strings =
    if(cs eq StandardCharsets.ISO_8859_1) Vector("abc", "def")
    else Vector("abc", "\u2710\ud83d", "\ude0adef")
  val ints = Vector(Int.MinValue, Short.MinValue.toInt, -100, -99, -1, 0, 1, 99, 100, Byte.MaxValue.toInt, 1000, Short.MaxValue.toInt, Int.MaxValue)
  val longs = Vector(Long.MinValue, Int.MinValue.toLong, Short.MinValue.toLong, -100L, -99L, -1L, 0L, 1L, 99L, 100L, Byte.MaxValue.toLong, 1000L, Short.MaxValue.toLong, Int.MaxValue.toLong, Long.MaxValue)
  val booleans = Vector(true, false)
  val chars =
    if(cs eq StandardCharsets.ISO_8859_1) Vector('a', 0.toChar, 127.toChar, 128.toChar)
    else Vector('a', 0.toChar, 127.toChar, 128.toChar, '\u2710', '\ud83d', '\ude0a')
  val doubles = Vector(Double.MinValue, Double.MaxValue, Double.NegativeInfinity, Double.PositiveInfinity, Double.NaN, 0.0d, -0.0d, 1.0d, -1.0d)

  @Test
  def printString(): Unit =
    strings.foreach { i => check(cs, eol, String.valueOf(i)) { out => out.print(i) } { out => out.print(i) } }

  @Test
  def printlnString(): Unit =
    strings.foreach { i => check(cs, eol, String.valueOf(i)) { out => out.println(i) } { out => out.print(i); out.print(eol) } }

  @Test
  def printStrings(): Unit =
    check(cs, eol, "strings") { out => strings.foreach(i => out.print(i)) } { out => strings.foreach(i => out.print(i)) }

  @Test
  def printInt(): Unit =
    ints.foreach { i => check(cs, eol, String.valueOf(i)) { out => out.print(i) } { out => out.print(i) } }

  @Test
  def printlnInt(): Unit =
    ints.foreach { i => check(cs, eol, String.valueOf(i)) { out => out.println(i) } { out => out.print(i); out.print(eol) } }

  @Test
  def printLong(): Unit =
    longs.foreach { i => check(cs, eol, String.valueOf(i)) { out => out.print(i) } { out => out.print(i) } }

  @Test
  def printlnLong(): Unit =
    longs.foreach { i => check(cs, eol, String.valueOf(i)) { out => out.println(i) } { out => out.print(i); out.print(eol) } }

  @Test
  def printBoolean(): Unit =
    booleans.foreach { i => check(cs, eol, String.valueOf(i)) { out => out.print(i) } { out => out.print(i) } }

  @Test
  def printlnBoolean(): Unit =
    booleans.foreach { i => check(cs, eol, String.valueOf(i)) { out => out.println(i) } { out => out.print(i); out.print(eol) } }

  @Test
  def printChar(): Unit =
    chars.foreach { i => check(cs, eol, String.valueOf(i)) { out => out.print(i) } { out => out.print(i) } }

  @Test
  def printlnChar(): Unit =
    chars.foreach { i => check(cs, eol, String.valueOf(i)) { out => out.println(i) } { out => out.print(i); out.print(eol) } }

  @Test
  def printChars(): Unit =
    check(cs, eol, "chars") { out => chars.foreach(i => out.print(i)) } { out => chars.foreach(i => out.print(i)) }

  @Test
  def printlnDouble(): Unit =
    doubles.foreach { i => check(cs, eol, String.valueOf(i)) { out => out.println(i) } { out => out.print(i); out.print(eol) } }


object TextOutputTest:
  @Parameterized.Parameters(name = "{0}")
  def params: java.util.List[Array[Any]] = (for
    cs <- Vector(StandardCharsets.ISO_8859_1, StandardCharsets.UTF_8, StandardCharsets.UTF_16, StandardCharsets.US_ASCII)
    (eol, eolName) <- Vector(("\n", "LF"), ("\r\n", "CRLF"), ("XXX", "XXX"))
  yield Array[Any](s"${cs}_${eolName}", cs, eol)).asJava

