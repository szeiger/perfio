package perfio

import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import scala.collection.mutable
import scala.jdk.CollectionConverters._

@RunWith(classOf[JUnit4])
class VectorizedLineTokenizerTest {

  @Test def smallAligned1: Unit = check(4096,
    """aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
      |a""".stripMargin
  )

  @Test def smallAligned2: Unit = check(128,
    """aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
      |""".stripMargin
  )

  @Test def smallAligned3: Unit = check(128,
    """aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
      |a
      |""".stripMargin
  )

  @Test def smallUnaligned1: Unit = check(128,
    """a""".stripMargin
  )

  @Test def largeAligned1: Unit = check(64,
    """b
      |daaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaae
      |""".stripMargin
  )

  @Test def largeUnaligned1: Unit = check(64,
    """aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
      |""".stripMargin
  )

  @Test def smallAlignedLimited1: Unit = check(4096,
    """aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa""".stripMargin, 30
  )

  @Test def smallAlignedLimited2: Unit = check(4096,
    """aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
      |a""".stripMargin, 30
  )

  @Test def split1: Unit = check(4096,
    """
      |a""".stripMargin, split = true
  )

  def check(ib: Int, s: String, maxRead: Int = Int.MaxValue, split: Boolean = false): Unit = {
    val exp = s.lines().toList.asScala
    val expL = exp.map(_.length)
    val bytes = s.getBytes(StandardCharsets.UTF_8)
    val in = BufferedInput(new LimitedInputStream(new ByteArrayInputStream(bytes), maxRead), initialBufferSize = ib)
    val (buf, t) = if(split) buildSplit(in) else buildNormal(in)
    val res = buf.map(_.length)
    assertEquals(
      s"""
         |---- expected: -------------------
         |${exp.mkString("\n")}
         |---- got: ------------------------
         |${buf.mkString("\n")}
         |----------------------------------
         |""".stripMargin, expL, res)
    assertNull(t.readLine())
    t.close()
  }

  def buildSplit(in: BufferedInput): (mutable.ArrayBuffer[String], LineTokenizer) = {
    var i = 0
    var t = VectorizedLineTokenizer(in)
    val buf = mutable.ArrayBuffer.empty[String]
    while(t.readLine() match {
      case null => false
      case s =>
        buf += s
        i += 1
        if(i % 2 == 0) {
          t.end()
          t = VectorizedLineTokenizer(in)
        }
        true
    }) ()
    (buf, t)
  }

  def buildNormal(in: BufferedInput): (mutable.ArrayBuffer[String], LineTokenizer) = {
    val t = VectorizedLineTokenizer(in)
    val buf = mutable.ArrayBuffer.empty[String]
    while(t.readLine() match {
      case null => false
      case s => buf += s; true
    }) ()
    (buf, t)
  }
}
