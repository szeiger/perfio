package perfio

import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream}

abstract class AbstractBufferedInputTest {
  val count: Int
  def createBuferedInput(): BufferedInput

  lazy val testData: Array[Byte] = {
    val out = new ByteArrayOutputStream()
    val dout = new DataOutputStream(out)
    for(i <- 0 until count) {
      dout.writeByte(i)
      dout.writeInt(i+2)
      dout.writeLong(i+3)
    }
    out.toByteArray
  }

  def createDataInput() = new DataInputStream(new ByteArrayInputStream(testData))

  @Test def simple(): Unit = {
    val din = createDataInput()
    val bin = createBuferedInput()
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

  @Test def nested(): Unit = {
    val din = createDataInput()
    val bin = createBuferedInput()
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
    val din = createDataInput()
    val bin = createBuferedInput()
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
    val din = createDataInput()
    val bin = createBuferedInput()
    assertEquals(0, bin.totalBytesRead)
    for(i <- 1 to count) {
      val b1 = din.readByte()
      val i1 = din.readInt()
      din.readLong()
      val bin2 = bin.delimitedView(13, skipRemaining = true)
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
}

@RunWith(classOf[JUnit4])
class BufferedInputFromInputStreamTest extends AbstractBufferedInputTest {
  val count = 1000
  def createBuferedInput(): BufferedInput = BufferedInput(new ByteArrayInputStream(testData), initialBufferSize = 64)
}

@RunWith(classOf[JUnit4])
class BufferedInputFromArrayTest extends AbstractBufferedInputTest {
  val count = 1000
  def createBuferedInput(): BufferedInput = BufferedInput.fromArray(testData)
}
