package perfio

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import java.io.{ByteArrayInputStream, EOFException}
import java.util.concurrent.CompletableFuture
import java.util.zip.GZIPInputStream

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
        assert(bo2.state == BufferedOutput.STATE_OPEN)
        val bo2t = bo2.totalBytesWritten.toInt
        bo2.close()
        cnt1(bo2t)

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
        ("_SyncXorPartialFlush", xorOutputTester(new XorFlushingBufferedOutput(_, true))),
        ("_SyncXorFlush", xorOutputTester(new XorFlushingBufferedOutput(_, false))),
        ("_SyncXorBlock", xorOutputTester(new XorBlockBufferedOutput(_))),
        ("_SyncXorBlockSwapping", xorOutputTester(new XorBlockSwappingBufferedOutput(_))),
        ("_SimpleAsyncXor", xorOutputTester(new SimpleAsyncXorBlockBufferedOutput(_))),
        ("_SimpleAsyncSequentialXor", xorOutputTester(new SimpleAsyncSequentialXorBlockBufferedOutput(_))),
        ("_AsyncXor", xorOutputTester(new AsyncXorBlockBufferedOutput(_, false, 0))),
        ("_AsyncSequentialXor", xorOutputTester(new AsyncXorBlockBufferedOutput(_, true, 0))),
        ("_Gzip", gzipOutputTester(new GzipBufferedOutput(_))),
        ("_AsyncGzip", gzipOutputTester(new AsyncGzipBufferedOutput(_))),
      )
    yield Array[Any](n+fn, c.andThen(tr))
    java.util.List.of(a*)

  def gzipOutputTester(f: BufferedOutput => BufferedOutput)(ot: OutputTester): OutputTester =
    ot.bo = f(ot.bo)
    ot.decoder = { a => new GZIPInputStream(new ByteArrayInputStream(a, 0, a.length)).readAllBytes() }
    ot

  def xorOutputTester(f: BufferedOutput => BufferedOutput)(ot: OutputTester): OutputTester =
    ot.bo = f(ot.bo)
    ot.decoder = (a => a.map(b => (b ^ 85).toByte))
    ot

  class XorFlushingBufferedOutput(parent: BufferedOutput, partialFlush: Boolean) extends FilteringBufferedOutput(parent, partialFlush):
    def filterBlock(b: BufferedOutput): Unit =
      val blen = b.pos - b.start
      val p = parent.fwd(blen)
      var i = 0
      while i < blen do
        parent.buf(p + i) = (b.buf(b.start + i) ^ 85.toByte).toByte
        i += 1
      if(state == BufferedOutput.STATE_CLOSED) releaseBlock(b)

  class XorBlockBufferedOutput(parent: BufferedOutput) extends FilteringBufferedOutput(parent, false):
    def filterBlock(b: BufferedOutput): Unit =
      var i = b.start
      while i < b.pos do
        b.buf(i) = (b.buf(i) ^ 85.toByte).toByte
        i += 1
      appendBlockToParent(b)

  class XorBlockSwappingBufferedOutput(parent: BufferedOutput) extends FilteringBufferedOutput(parent, false):
    def filterBlock(b: BufferedOutput): Unit =
      val out = allocBlock()
      val blen = b.pos - b.start
      var i = 0
      while i < blen do
        out.buf(i) = (b.buf(i + b.start) ^ 85.toByte).toByte
        i += 1
      out.start = 0
      out.pos = blen
      appendBlockToParent(out)
      returnToCache(b)

  class SimpleAsyncXorBlockBufferedOutput(parent: BufferedOutput) extends FilteringBufferedOutput(parent, false):
    private var pending: BufferedOutput = null

    private def enqueue(b: BufferedOutput): Unit =
      if(pending == null)
        pending = b
        b.next = b
        b.prev = b
      else b.insertBefore(pending)

    private def dequeueFirst(): BufferedOutput =
      val p = pending
      if(p.next eq p) pending = null
      else
        pending = p.next
        p.unlinkOnly()
      p

    def filterBlock(b: BufferedOutput): Unit =
      while pending != null && pending.filterState.asInstanceOf[CompletableFuture[Unit]].isDone do
        val p = dequeueFirst()
        appendBlockToParent(p)
      b.filterState = CompletableFuture.runAsync: () =>
        //println(s"Starting $b on ${Thread.currentThread()}")
        //Thread.sleep(100L)
        var i = b.start
        while i < b.pos do
          b.buf(i) = (b.buf(i) ^ 85.toByte).toByte
          i += 1
        //println(s"Finished $b on ${Thread.currentThread()}")
      enqueue(b)


    override protected def flushPending(): Unit =
      while pending != null do
        val p = dequeueFirst()
        p.filterState.asInstanceOf[CompletableFuture[Unit]].get
        appendBlockToParent(p)

  class SimpleAsyncSequentialXorBlockBufferedOutput(parent: BufferedOutput) extends FilteringBufferedOutput(parent, false):
    private var pending: BufferedOutput = null
    private var previousFuture: CompletableFuture[?] = null

    private def enqueue(b: BufferedOutput): Unit =
      if(pending == null)
        pending = b
        b.next = b
        b.prev = b
      else b.insertBefore(pending)

    private def dequeueFirst(): BufferedOutput =
      val p = pending
      if(p.next eq p) pending = null
      else
        pending = p.next
        p.unlinkOnly()
      p

    def filterBlock(b: BufferedOutput): Unit =
      while pending != null && pending.filterState.asInstanceOf[CompletableFuture[?]].isDone do
        val p = dequeueFirst()
        appendBlockToParent(p)
      val r: Runnable = () =>
        //println(s"Starting $b on ${Thread.currentThread()}")
        //Thread.sleep(100L)
        var i = b.start
        while i < b.pos do
          b.buf(i) = (b.buf(i) ^ 85.toByte).toByte
          i += 1
        //println(s"Finished $b on ${Thread.currentThread()}")
      if(previousFuture == null)
        previousFuture = CompletableFuture.runAsync(r)
        b.filterState = previousFuture
      else
        previousFuture = previousFuture.thenRun(r)
        b.filterState = previousFuture
      enqueue(b)


    override protected def flushPending(): Unit =
      while pending != null do
        val p = dequeueFirst()
        p.filterState.asInstanceOf[CompletableFuture[Unit]].get
        appendBlockToParent(p)

  class AsyncXorBlockBufferedOutput(parent: BufferedOutput, sequential: Boolean, depth: Int) extends AsyncFilteringBufferedOutput(parent, sequential, depth, false):
    def filterAsync(t: AsyncFilteringBufferedOutput#Task): Unit =
      val tolen = t.to.buf.length min 1024 // force some buffer overflows to test the overflow logic
      val fromlen = t.from.pos - t.from.start
      val prlen = fromlen min tolen
      var i = 0
      while i < prlen do
        t.to.int8((t.from.buf(i + t.from.start) ^ 85.toByte).toByte)
        i += 1
      t.from.start += prlen
