package perfio.scalaapi

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import perfio.{BufferedOutput, ArrayBufferedOutput, TextOutput}

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
    check("""abc
            |  123
            |  nested 1
            |  nested 2
            |456
            |""".stripMargin):
      pm"""abc
          |  123
          >  $nestedMulti
          |456"""
    check("**str\n", "**"):
      pm"""${str}"""
    check("""**xxx nested output {
            |**  x str
            |**    2
            |**  x str
            |**    2
            |""".stripMargin, "**"):
      pm"""xxx $nested {
          >  ${Printed(for(i <- 1 to 2) nestedMulti2)}"""


  def nested(using TextOutputContext) =
    p"nested output"

  def str = "str"

  def nestedMulti(using TextOutputContext) =
    pm"""nested 1
        |nested 2"""

  def nestedMulti2(using toc: TextOutputContext) =
    pm"x $str"
    pm"  2"

  def check(exp: String, pre: String = "")(f: TextOutputContext ?=> Unit): Unit =
    val bo = BufferedOutput.growing()
    val to = bo.text(StandardCharsets.UTF_8, "\n")
    f(using TextOutputContext(to, pre))
    bo.close()
    val got = new String(bo.buffer(), 0, bo.length(), StandardCharsets.UTF_8)
    assertEquals(exp, got)
