package perfio.scalaapi

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import perfio.{BufferedOutput, FullyBufferedOutput, TextOutput}

import java.nio.charset.StandardCharsets

@RunWith(classOf[JUnit4])
class TextFormattingTest:

  @Test
  def test(): Unit =
    check("foo42bar\n")(p"foo${42}bar\n")
    check("foo1barabaz\n")(p"foo${1}bar${"a"}baz\n")
    val x = 23
    check("x23\nyY\n\n<nested output>\n\n"):
      pm"""x$x
          |y${"Y"}\n
          |<$nested>
          |"""

  def nested(using TextOutputContext) =
    p"nested output"

  def check(exp: String)(f: TextOutputContext ?=> Unit): Unit =
    val bo = BufferedOutput.growing()
    val to = bo.text(StandardCharsets.UTF_8, "\n")
    f(using TextOutputContext(to))
    bo.close()
    val got = new String(bo.buffer(), 0, bo.length(), StandardCharsets.UTF_8)
    assertEquals(exp, got)
