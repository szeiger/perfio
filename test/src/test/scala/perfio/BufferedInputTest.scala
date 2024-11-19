package perfio

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

import java.io.ByteArrayInputStream
import java.nio.ByteOrder

abstract class AbstractBufferedInputTest extends TestUtil:
  val count = 1000
  def createBufferedInput(td: TestData): BufferedInput

  lazy val testData = createTestData("small"): dout =>
    for i <- 0 until count do
      dout.writeByte(i)
      dout.writeInt(i+2)
      dout.writeLong(i+3)

  lazy val testDataLE = createTestData("small-le"): dout =>
    for i <- 0 until count do
      dout.writeByte(i)
      dout.writeInt(reverseByteOrder(i+2))
      dout.writeLong(reverseByteOrder(i+3L))

  lazy val stringTestData = createTestData("string"): dout =>
    for i <- 0 until count do
      dout.writeUTF("abcdefghijklmnopqrstuvwxyz")

  @Test def simple(): Unit =
    val din = testData.createDataInput()
    val bin = createBufferedInput(testData)
    assertEquals(0, bin.totalBytesRead)
    for i <- 1 to count do
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
    assert(!bin.hasMore)
    assertEquals(testData.length, bin.totalBytesRead)

  @Test def little(): Unit =
    val din = testDataLE.createDataInput()
    val bin = createBufferedInput(testDataLE).order(ByteOrder.LITTLE_ENDIAN)
    assertEquals(0, bin.totalBytesRead)
    for i <- 1 to count do
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
    assert(!bin.hasMore)
    assertEquals(testDataLE.length, bin.totalBytesRead)

  @Test def nested(): Unit =
    val din = testData.createDataInput()
    val bin = createBufferedInput(testData)
    assertEquals(0, bin.totalBytesRead)
    for i <- 1 to count do
      val b1 = din.readByte()
      val i1 = din.readInt()
      val l1 = din.readLong()
      val bin2 = bin.limitedView(13)
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
    assert(!bin.hasMore)
    assertEquals(testData.length, bin.totalBytesRead)

  @Test def nestedNoSkip(): Unit =
    val din = testData.createDataInput()
    val bin = createBufferedInput(testData)
    assertEquals(0, bin.totalBytesRead)
    for i <- 1 to count do
      val b1 = din.readByte()
      val i1 = din.readInt()
      val l1 = din.readLong()
      val bin2 = bin.limitedView(13)
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
    assert(!bin.hasMore)
    assertEquals(testData.length, bin.totalBytesRead)

  @Test def nestedSkip(): Unit =
    val din = testData.createDataInput()
    val bin = createBufferedInput(testData)
    assertEquals(0, bin.totalBytesRead)
    for i <- 1 to count do
      val b1 = din.readByte()
      val i1 = din.readInt()
      din.readLong()
      val bin2 = bin.limitedView(13, true)
      assertEquals(0, bin2.totalBytesRead)
      val b2 = bin2.int8()
      val i2 = bin2.int32()
      assertEquals(b1, b2)
      assertEquals(i1, i2)
      assertEquals(5, bin2.totalBytesRead)
      assertTrue(bin2.hasMore)
      bin2.close()
      assertEquals(i * 13, bin.totalBytesRead)
    assert(!bin.hasMore)
    assertEquals(testData.length, bin.totalBytesRead)

  @Test def string(): Unit =
    val din = stringTestData.createDataInput()
    val bin = createBufferedInput(stringTestData)
    for i <- 1 to count do
      val str1 = din.readUTF()
      val len2 = bin.uint16()
      val str2 = bin.string(len2)
      assertEquals(str1, str2)
    assert(!bin.hasMore)


@RunWith(classOf[JUnit4])
class BufferedInputFromInputStreamTest extends AbstractBufferedInputTest:
  def createBufferedInput(td: TestData): BufferedInput = td.createBufferedInputFromByteArrayInputStream()

@RunWith(classOf[JUnit4])
class BufferedInputFromLimitedInputStreamTest extends AbstractBufferedInputTest:
  def createBufferedInput(td: TestData): BufferedInput = td.createBufferedInputFromByteArrayInputStream(limit = 1)

@RunWith(classOf[JUnit4])
class BufferedInputFromMappedFileTest extends AbstractBufferedInputTest:
  def createBufferedInput(td: TestData): BufferedInput = td.createBufferedInputFromMappedFile()

@RunWith(classOf[JUnit4])
class BufferedInputFromArrayTest extends AbstractBufferedInputTest:
  def createBufferedInput(td: TestData): BufferedInput = td.createBufferedInputFromArray()


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
