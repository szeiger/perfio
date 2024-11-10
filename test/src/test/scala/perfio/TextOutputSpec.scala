package perfio

import hedgehog.*
import hedgehog.runner.*

import java.io.{ByteArrayOutputStream, PrintWriter}
import java.nio.charset.{Charset, StandardCharsets}

object TextOutputSpec extends Properties:

  def tests: List[Test] =
    (for
      cs <- List(StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1, StandardCharsets.UTF_16, StandardCharsets.US_ASCII)
      eol <- List("\n", "\r\n", "XXX")
      t <- createTests(cs, eol)
    yield t).sortBy(_.name)

  def showEol(eol: String) = eol match
    case "\n" => "LF"
    case "\r\n" => "CRLF"
    case "XXX" => "XXX"

  def createTests(cs: Charset, eol: String): List[Test] = List(
    property(s"println_String_${cs}_${showEol(eol)}", printlnStringP(cs, eol)),
    property(s"print_String_${cs}_${showEol(eol)}", printStringP(cs, eol)),
    property(s"println_Int_${cs}_${showEol(eol)}", printlnIntP(cs, eol)),
    property(s"print_Int_${cs}_${showEol(eol)}", printIntP(cs, eol)),
  )

  def printlnStringP(cs: Charset, eol: String): Property =
    for
      l1 <- Gen.string(Gen.char('a', 'c'), Range.linear(0, 100)).forAll
    yield
      check(cs, eol) { out =>
        out.println(l1)
      } { out =>
        out.print(l1)
        out.print(eol)
      }

  def printStringP(cs: Charset, eol: String): Property =
    for
      l1 <- Gen.string(Gen.char('a', 'c'), Range.linear(0, 100)).forAll
    yield
      check(cs, eol) { out =>
        out.print(l1)
      } { out =>
        out.print(l1)
      }

  def printlnIntP(cs: Charset, eol: String): Property =
    for
      l1 <- Gen.int(Range.constantFrom(0, Int.MinValue, Int.MaxValue)).forAll
    yield
      check(cs, eol) { out =>
        out.println(l1)
      } { out =>
        out.print(l1)
        out.print(eol)
      }

  def printIntP(cs: Charset, eol: String): Property =
    for
      l1 <- Gen.int(Range.constantFrom(0, Int.MinValue, Int.MaxValue)).forAll
    yield
      check(cs, eol) { out =>
        out.print(l1)
      } { out =>
        out.print(l1)
      }

  def check(cs: Charset, eol: String)(f: TextOutput => Unit)(g: PrintWriter => Unit): Result =
    val bout = BufferedOutput.growing()
    val tout = TextOutput.of(bout, cs, eol)
    f(tout)
    tout.close()
    val buf: Vector[Int] = bout.copyToByteArray.toVector.map(b => b & 0xFF)
    val ba = new ByteArrayOutputStream()
    val pw = new PrintWriter(ba, false, cs)
    g(pw)
    pw.close()
    val exp: Vector[Int] = ba.toByteArray.toVector.map(b => b & 0xFF)
    buf ==== exp
