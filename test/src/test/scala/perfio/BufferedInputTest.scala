package perfio

import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

import java.io.ByteArrayInputStream
import java.nio.ByteOrder

abstract class AbstractBufferedInputTest extends TestUtil {
  val count = 1000
  def createBufferedInput(td: TestData): BufferedInput

  lazy val testData = createTestData("small") { dout =>
    for(i <- 0 until count) {
      dout.writeByte(i)
      dout.writeInt(i+2)
      dout.writeLong(i+3)
    }
  }

  lazy val testDataLE = createTestData("small-le") { dout =>
    for(i <- 0 until count) {
      dout.writeByte(i)
      dout.writeInt(reverseByteOrder(i+2))
      dout.writeLong(reverseByteOrder(i+3L))
    }
  }

  lazy val stringTestData = createTestData("string") { dout =>
    for(i <- 0 until count) {
      dout.writeUTF("abcdefghijklmnopqrstuvwxyz")
    }
  }

  @Test def simple(): Unit = {
    val din = testData.createDataInput()
    val bin = createBufferedInput(testData)
    assertEquals(0, bin.totalBytesRead)
    for(i <- 1 to count) {
      val b1 = din.readByte()
      val b2 = bin.int8()
      assertEquals(b1, b2)
      val i1 = din.readInt()
      val i2 = bin.int32()
      assertEquals(i1, i2)
      val l1 = din.readLong()
      val l2 = bin.int64()
      assertEquals(l1, l2)
      assertEquals(i * 13, bin.totalBytesRead)
    }
    assert(!bin.hasMore)
    assertEquals(testData.length, bin.totalBytesRead)
  }

  @Test def little(): Unit = {
    val din = testDataLE.createDataInput()
    val bin = createBufferedInput(testDataLE).order(ByteOrder.LITTLE_ENDIAN)
    assertEquals(0, bin.totalBytesRead)
    for(i <- 1 to count) {
      val b1 = din.readByte()
      val b2 = bin.int8()
      assertEquals(b1, b2)
      val i1 = reverseByteOrder(din.readInt())
      val i2 = bin.int32()
      assertEquals(i1, i2)
      val l1 = reverseByteOrder(din.readLong())
      val l2 = bin.int64()
      assertEquals(l1, l2)
      assertEquals(i * 13, bin.totalBytesRead)
    }
    assert(!bin.hasMore)
    assertEquals(testDataLE.length, bin.totalBytesRead)
  }

  @Test def nested(): Unit = {
    val din = testData.createDataInput()
    val bin = createBufferedInput(testData)
    assertEquals(0, bin.totalBytesRead)
    for(i <- 1 to count) {
      val b1 = din.readByte()
      val i1 = din.readInt()
      val l1 = din.readLong()
      val bin2 = bin.delimitedView(13)
      assertEquals(0, bin2.totalBytesRead)
      val b2 = bin2.int8()
      val i2 = bin2.int32()
      val l2 = bin2.int64()
      assertEquals(b1, b2)
      assertEquals(i1, i2)
      assertEquals(l1, l2)
      assertEquals(13, bin2.totalBytesRead)
      assertFalse(bin2.hasMore)
      bin2.close()
      assertEquals(i * 13, bin.totalBytesRead)
    }
    assert(!bin.hasMore)
    assertEquals(testData.length, bin.totalBytesRead)
  }

  @Test def nestedNoSkip(): Unit = {
    val din = testData.createDataInput()
    val bin = createBufferedInput(testData)
    assertEquals(0, bin.totalBytesRead)
    for(i <- 1 to count) {
      val b1 = din.readByte()
      val i1 = din.readInt()
      val l1 = din.readLong()
      val bin2 = bin.delimitedView(13)
      assertEquals(0, bin2.totalBytesRead)
      val b2 = bin2.int8()
      val i2 = bin2.int32()
      assertEquals(b1, b2)
      assertEquals(i1, i2)
      assertEquals(5, bin2.totalBytesRead)
      assertTrue(bin2.hasMore)
      bin2.close()
      assertEquals(i * 13 - 8, bin.totalBytesRead)
      val l2 = bin.int64()
      assertEquals(l1, l2)
    }
    assert(!bin.hasMore)
    assertEquals(testData.length, bin.totalBytesRead)
  }

  @Test def nestedSkip(): Unit = {
    val din = testData.createDataInput()
    val bin = createBufferedInput(testData)
    assertEquals(0, bin.totalBytesRead)
    for(i <- 1 to count) {
      val b1 = din.readByte()
      val i1 = din.readInt()
      din.readLong()
      val bin2 = bin.delimitedView(13, true)
      assertEquals(0, bin2.totalBytesRead)
      val b2 = bin2.int8()
      val i2 = bin2.int32()
      assertEquals(b1, b2)
      assertEquals(i1, i2)
      assertEquals(5, bin2.totalBytesRead)
      assertTrue(bin2.hasMore)
      bin2.close()
      assertEquals(i * 13, bin.totalBytesRead)
    }
    assert(!bin.hasMore)
    assertEquals(testData.length, bin.totalBytesRead)
  }

  @Test def string(): Unit = {
    val din = stringTestData.createDataInput()
    val bin = createBufferedInput(stringTestData)
    for(i <- 1 to count) {
      val str1 = din.readUTF()
      val len2 = bin.uint16()
      val str2 = bin.string(len2)
      assertEquals(str1, str2)
    }
    assert(!bin.hasMore)
  }
}

@RunWith(classOf[JUnit4])
class BufferedInputFromInputStreamTest extends AbstractBufferedInputTest {
  def createBufferedInput(td: TestData): BufferedInput = td.createBufferedInputFromByteArrayInputStream()
}

@RunWith(classOf[JUnit4])
class BufferedInputFromLimitedInputStreamTest extends AbstractBufferedInputTest {
  def createBufferedInput(td: TestData): BufferedInput = td.createBufferedInputFromByteArrayInputStream(limit = 1)
}

@RunWith(classOf[JUnit4])
class BufferedInputFromMappedFileTest extends AbstractBufferedInputTest {
  def createBufferedInput(td: TestData): BufferedInput = td.createBufferedInputFromMappedFile()
}

@RunWith(classOf[JUnit4])
class BufferedInputFromArrayTest extends AbstractBufferedInputTest {
  def createBufferedInput(td: TestData): BufferedInput = td.createBufferedInputFromArray()
}

@RunWith(classOf[JUnit4])
class MiscBufferedInputTest {
  @Test
  def testCodeGeneratorRequestRaw(): Unit = {
    val data3 = Array[Byte](1, 2, 3, 4)

    def read(in: BufferedInput): Unit = {
      println(s"in: ${in.show}")

      println("in2 = in.delimitedView(4)")
      val in2 = in.delimitedView(4L)
      println(s"in: ${in.show}")
      println(s"in2: ${in2.show}")

      println("reading 2 bytes")
      assertEquals(1, in2.int8())
      assertEquals(2, in2.int8())
      println(s"in2: ${in2.show}")

      println("in3 = in2.delimitedView(1)")
      val in3 = in2.delimitedView(1L)
      println(s"in2: ${in2.show}")
      println(s"in3: ${in3.show}")

      println("in4 = in3.delimitedView(1)")
      val in4 = in3.delimitedView(1L)
      println(s"in3: ${in3.show}")
      println(s"in4: ${in4.show}")

      println("reading 1 byte")
      assertEquals(3, in4.int8())
      println(s"in4: ${in4.show}")

      assert(!in4.hasMore)
      println(s"in4: ${in4.show}")

      println("in4.close")
      in4.close()
      println(s"in3: ${in3.show}")
      assert(!in3.hasMore)
      println(s"in3: ${in3.show}")

      println("in3.close")
      in3.close()
      println(s"in2: ${in2.show}")

      assertEquals(4, in2.int8())

      assert(!in2.hasMore)
      in2.close()
      assert(!in.hasMore)
      in.close()
    }

    println("========== unlimited")
    read(BufferedInput.of(new ByteArrayInputStream(data3)))
    println("========== limited")
    read(BufferedInput.of(new LimitedInputStream(new ByteArrayInputStream(data3), 2)))
  }
}