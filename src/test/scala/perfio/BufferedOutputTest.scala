package perfio

import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(classOf[JUnit4])
class BufferedOutputTest extends TestUtil {
  val count = 1000

  lazy val testData = createTestData("small") { dout =>
    for(i <- 0 until count) {
      dout.writeByte(i)
      dout.writeInt(i+2)
      dout.writeLong(i+3)
    }
  }

  @Test def simple(): Unit = {
    val (bo, checker) = testData.createBufferedOutputToOutputStream()
    for(i <- 0 until count) {
      bo.int8(i.toByte)
      bo.int32(i+2)
      bo.int64(i+3)
    }
    checker()
  }
}
