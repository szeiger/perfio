package perfio

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import java.io.{EOFException, IOException}

@RunWith(classOf[Parameterized])
class BufferedOutputTest(_name: String, create: TestData => OutputTester) extends TestUtil:
  val count = 1000

  lazy val numTestData = createTestData("num"): dout =>
    for i <- 0 until count do
      dout.writeByte(i)
      dout.writeInt(i+2)
      dout.writeLong(i+3)

  lazy val viewTestData = createTestData("view"): dout =>
    for i <- 0 until count do
      dout.writeInt(13)
      dout.writeByte(i)
      dout.writeInt(i+2)
      dout.writeLong(i+3)

  lazy val nestedViewTestData = createTestData("nesdView"): dout =>
    dout.writeByte(1)
    dout.writeByte(2)
    dout.writeByte(3)
    dout.writeByte(4)

  lazy val stringTestData = createTestData("string"): dout =>
    for i <- 0 until count do
      val s = "abcdefghijklmnopqrstuvwxyz"
      dout.writeUTF(s)

  @Test def num(): Unit =
    create(numTestData): bo =>
      assertEquals(0L, bo.totalBytesWritten)
      for i <- 0 until count do
        bo.int8(i.toByte)
        bo.int32(i+2)
        bo.int64(i+3)
      assertEquals(count * 13, bo.totalBytesWritten)

  @Test def string(): Unit =
    create(stringTestData): bo =>
      val cnt = new Counter(bo)
      for i <- 0 until count do
        cnt(0)
        val s = "abcdefghijklmnopqrstuvwxyz"
        bo.int16(s.length.toShort)
        cnt(2)
        bo.string(s)
        cnt(s.length)

  final class Counter(bo: BufferedOutput):
    var bocount = 0
    def apply(i: Int): Unit =
      bocount += i
      assertEquals(bocount, bo.totalBytesWritten)

  @Test def reserve(): Unit =
    create(viewTestData): bo =>
      val cnt = new Counter(bo)
      for i <- 0 until count do
        //println("****** "+i)
        cnt(0)
        val bo2 = bo.reserve(4)
        cnt(4)
        assertEquals(0, bo2.totalBytesWritten)
        bo.int8(i.toByte)
        cnt(1)
        bo.int32(i+2)
        cnt(4)
        bo.int64(i+3)
        cnt(8)
        bo2.int32(13)
        cnt(0)
        assertEquals(4, bo2.totalBytesWritten)
        assertException[EOFException](bo2.int8(0))
        bo2.close()
        cnt(0)
        assertException[IOException](bo2.int8(0))

  @Test def defer(): Unit =
    create(viewTestData): bo1 =>
      val cnt1 = new Counter(bo1)
      for i <- 0 until count do
        //println("****** "+i)
        cnt1(0)
        val bo2 = bo1.defer()
        val cnt2 = new Counter(bo2)
        cnt1(0)
        cnt2(0)
        bo2.int8(i.toByte)
        cnt2(1)
        bo2.int32(i+2)
        cnt2(4)
        bo2.int64(i+3)
        cnt2(8)
        bo1.int32(13)
        cnt1(4)
        cnt2(0)
        bo2.close()
        assertException[IOException](bo2.int8(0))
        cnt1(bo2.totalBytesWritten.toInt)

  private def testFixed(padLeft: Int, padRight: Int): Unit =
    val buf = new Array[Byte](count*13 + padLeft + padRight)
    numTestData.createFixedBufferedOutput(buf, padLeft, count*13): bo =>
      assertEquals(0L, bo.totalBytesWritten)
      for i <- 0 until count do
        bo.int8(i.toByte)
        bo.int32(i+2)
        bo.int64(i+3)
      assertEquals(count * 13, bo.totalBytesWritten)
      assertException[EOFException](bo.int8(0))

  @Test def fixedFull(): Unit = testFixed(0, 0)
  @Test def fixedOffset(): Unit = testFixed(25, 0)
  @Test def fixedLimit(): Unit = testFixed(0, 25)
  @Test def fixedBoth(): Unit = testFixed(139, 17)

  @Test
  def testNestedView(): Unit =
    create(nestedViewTestData): out1 =>
      out1.uint8(1)
      val out2 = out1.defer()
      out2.uint8(3)
      val out3 = out2.defer()
      out3.uint8(4)
      out3.close()
      out1.uint8(2)
      out2.close()
      out1.close()

  def show(b: BufferedOutput): Unit =
    println(s"    ${b.hashCode()}: buf=${b.buf.hashCode()} ${b.buf.slice(0, b.pos).mkString("[",",","]")} sharing=${b.sharing}")

  def showAll(b: BufferedOutput): Unit =
    var n = b.next
    while true do
      show(n)
      if(n eq b) return
      n = n.next

object BufferedOutputTest:
  @Parameterized.Parameters(name = "{0}")
  def params: java.util.List[Array[Any]] =
    val a: Array[Array[Any]] = for
      (n, c) <- Array(
        ("baos_64", (_: TestData).createBufferedOutputToOutputStream(64)),
        ("baos_32768", (_: TestData).createBufferedOutputToOutputStream(32768)),
        ("growing_64", (_: TestData).createGrowingBufferedOutput(64)),
        ("fixed_32768", (_: TestData).createFixedBufferedOutput(new Array[Byte](32768))),
        ("block_64", (_: TestData).createBlockBufferedOutput(64)),
        ("pipe_64", (_: TestData).createPipeBufferedOutput(64)),
      )
      (fn, tr) <- Array(
        ("", identity[OutputTester]),
        ("_SyncXor", xorOutputTester(85)),
      )
    yield Array[Any](n+fn, c.andThen(tr))
    java.util.List.of(a*)

  def xorOutputTester(mask: Byte)(ot: OutputTester): OutputTester =
    ot.bo = new SyncXorFilteringBufferedOutput(ot.bo, mask)
    ot.decoder = (a => a.map(b => (b ^ mask).toByte))
    ot

  class SyncXorFilteringBufferedOutput(parent: BufferedOutput, mask: Byte) extends FilteringBufferedOutput(parent):
    def finalizeBlock(b: BufferedOutput, blocking: Boolean): Unit =
      assert((b.prev eq b.topLevel) || (b.prev.state == BufferedOutput.STATE_CLOSED))

//      var i = b.start
//      while i < b.pos do
//        b.buf(i) = (b.buf(i) ^ mask).toByte
//        i += 1
//      b.state = BufferedOutput.STATE_CLOSED
//
//      parent.write(b.buf, b.start, b.pos - b.start)
//      println(s"${b.showList()}")
//      b.totalFlushed += (b.pos - b.start)
//      b.pos = b.lim
//      b.start = b.lim

      val blen = b.pos - b.start
      val p = parent.fwd(blen)
      var i = 0
      while i < blen do
        parent.buf(p + i) = (b.buf(b.start + i) ^ mask).toByte
        i += 1
      b.state = BufferedOutput.STATE_CLOSED
      b.totalFlushed += blen
      b.pos = b.lim
      b.start = b.lim
