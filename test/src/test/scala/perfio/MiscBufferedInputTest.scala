package perfio

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

import java.io.ByteArrayInputStream

@RunWith(classOf[JUnit4])
class MiscBufferedInputTest:
  @Test
  def testCodeGeneratorRequestRaw(): Unit =
    val data3 = Array[Byte](1, 2, 3, 4)

    def read(in: BufferedInput): Unit =
      val in2 = in.limitedView(4L)
      assertEquals(1, in2.int8())
      assertEquals(2, in2.int8())
      val in3 = in2.limitedView(1L)
      val in4 = in3.limitedView(1L)
      assertEquals(3, in4.int8())
      assert(!in4.hasMore)
      in4.close()
      assert(!in3.hasMore)
      in3.close()
      assertEquals(4, in2.int8())
      assert(!in2.hasMore)
      in2.close()
      assert(!in.hasMore)
      in.close()

    read(BufferedInput.of(new ByteArrayInputStream(data3)))
    read(BufferedInput.of(new LimitedInputStream(new ByteArrayInputStream(data3), 2)))
