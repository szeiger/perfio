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

  def check(ib: Int, s: String, maxRead: Int = Int.MaxValue): Unit = {
    val exp = s.lines().toList.asScala.map(_.length)
    val bytes = s.getBytes(StandardCharsets.UTF_8)
    val t = VectorizedLineTokenizer(new LimitedInputStream(new ByteArrayInputStream(bytes), maxRead), initialBufferSize = ib)
    val buf = mutable.ArrayBuffer.empty[String]
    while(t.readLine() match {
      case null => false
      case s => buf += s; true
    }) ()
    val res = buf.map(_.length)
    assertEquals(exp, res)
    assertNull(t.readLine())
  }
}
