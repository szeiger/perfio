package perfio

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.{JUnit4, Parameterized}

import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

@RunWith(classOf[Parameterized])
class LineTokenizerTest(_name: String, createTokenizer: BufferedInput => LineTokenizer, mode: String) extends TestUtil:

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

  @Test def splitPosition: Unit = check(4096,
    """
      |
      |a""".stripMargin, split = true
  )

  def check(ib: Int, s: String, maxRead: Int = Int.MaxValue, split: Boolean = false): Unit =
    val exp = s.lines().toList.asScala
    val expL = exp.map(_.length)
    val bytes = s.getBytes(StandardCharsets.UTF_8)
    def mkHeap = BufferedInput.of(new LimitedInputStream(new ByteArrayInputStream(bytes), maxRead), ib)
    def mkDirect =
      val bb = ByteBuffer.allocateDirect(bytes.length)
      bb.put(bytes)
      bb.position(0)
      BufferedInput.ofByteBuffer(bb)
    val in = if(mode == "direct") mkDirect else mkHeap
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

  def buildSplit(in: BufferedInput): (mutable.ArrayBuffer[String], LineTokenizer) =
    var i = 0
    var t = createTokenizer(in)
    val buf = mutable.ArrayBuffer.empty[String]
    while t.readLine() match
      case null => false
      case s =>
        buf += s
        i += 1
        if(i % 2 == 0)
          t.close(false)
          t = createTokenizer(in)
        true
    do ()
    (buf, t)

  def buildNormal(in: BufferedInput): (mutable.ArrayBuffer[String], LineTokenizer) =
    val t = createTokenizer(in)
    val buf = mutable.ArrayBuffer.empty[String]
    while t.readLine() match
      case null => false
      case s => buf += s; true
    do ()
    (buf, t)

object LineTokenizerTest:
  @Parameterized.Parameters(name = "{0}")
  def params: java.util.List[Array[Any]] =
    val a: Array[Array[Any]] = for
      (n, c) <- Array(
        ("vectorized", { (in: BufferedInput) => VectorizedLineTokenizer.of(in, StandardCharsets.UTF_8, '\n'.toByte, '\r'.toByte) }),
        ("scalar", { (in: BufferedInput) => ScalarLineTokenizer.of(in, StandardCharsets.UTF_8, '\n'.toByte, '\r'.toByte) }),
      )
      m <- Array("heap", "direct")
    yield Array[Any](s"${n}_$m", c, m)
    java.util.List.of(a*)
