package perfio

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import java.io.DataInputStream
import java.nio.ByteOrder
import java.util.zip.CRC32

@RunWith(classOf[Parameterized])
class BufferedInputTest(_name: String, create: TestData => InputTester) extends TestUtil:
  val count = 1000

  def run(td: TestData)(f: (DataInputStream, BufferedInput) => Unit): Unit =
    create(td): bin =>
      val din = td.createDataInput()
      f(din, bin)

  lazy val testData = createTestData("small-"+count): dout =>
    for i <- 0 until count do
      dout.writeByte(i)
      dout.writeInt(i+2)
      dout.writeLong(i+3)

  lazy val testDataLE = createTestData("small-le-"+count): dout =>
    for i <- 0 until count do
      dout.writeByte(i)
      dout.writeInt(reverseByteOrder(i+2))
      dout.writeLong(reverseByteOrder(i+3L))

  lazy val stringTestData = createTestData("string-"+count): dout =>
    for i <- 0 until count do
      dout.writeUTF("abcdefghijklmnopqrstuvwxyz")

  @Test def simple(): Unit =
    run(testData): (din, bin) =>
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
    run(testDataLE): (din, bin) =>
      bin.order(ByteOrder.LITTLE_ENDIAN)
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
    run(testData): (din, bin) =>
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
    run(testData): (din, bin) =>
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
    run(testData): (din, bin) =>
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
    run(stringTestData): (din, bin) =>
      for i <- 1 to count do
        val str1 = din.readUTF()
        val len2 = bin.uint16()
        val str2 = bin.string(len2)
        assertEquals(str1, str2)
      assert(!bin.hasMore)

object BufferedInputTest:
  @Parameterized.Parameters(name = "{0}")
  def params: java.util.List[Array[Any]] =
    val a: Array[Array[Any]] = for
      (n, c) <- Array(
        ("array", (_: TestData).createInputTesterFromArray()),
        ("mapped", (_: TestData).createInputTesterFromMappedFile()),
        ("stream", (_: TestData).createInputTesterFromByteArrayInputStream()),
        ("stream_limit1", (_: TestData).createInputTesterFromByteArrayInputStream(limit = 1)),
      )
      (fn, tr) <- Array(
        ("", identity[InputTester]),
        ("_filter_xor", xorInputTester(new XorBufferedInput(_))),
        ("_checked", checkedInputTester),
      )
      if !n.contains("mapped") || (!fn.contains("filter") && !fn.contains("checked"))
    yield Array[Any](n+fn, c.andThen(tr))
    java.util.List.of(a*)

  def xorInputTester(f: BufferedInput => BufferedInput)(it: InputTester): InputTester =
    InputTester(it.td, it.data.map(b => (b ^ 85).toByte))(a => f(it.build(a)))

  def checkedInputTester(it: InputTester): InputTester = {
    val crc = new CRC32
    InputTester(it.td, it.data)(a => new CheckedHeapBufferedInput(it.build(a).asInstanceOf[HeapBufferedInput], crc))
  }


class XorBufferedInput(parent: BufferedInput) extends FilteringBufferedInput(parent, 32768):
  def filterBlock(from: BufferedInput, to: WritableBuffer[?]): Unit =
    val avail = to.available()
    from.request(1)
    val l = Math.min(from.available(), avail)
    from.bytes(to.buf, to.pos, l)
    for(i <- to.pos until to.pos + l)
      to.buf(i) = (to.buf(i) ^ 85).toByte
    to.pos += l
