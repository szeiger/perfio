package perfio

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import java.io.{ByteArrayInputStream, DataInputStream}
import java.util.zip.{CRC32, CheckedInputStream}

@RunWith(classOf[Parameterized])
class TracingBufferedInputTest(params: TracingBufferedInputTest.Params) extends TestUtil:
  val count = 1000

  lazy val numTestData = createTestData("num"): dout =>
    for i <- 0 until count do
      dout.writeByte(i)
      dout.writeInt(i+2)
      dout.writeLong(i+3)

  lazy val stringTestData = createTestData("string"): dout =>
    for i <- 0 until count do
      val s = "abcdefghijklmnopqrstuvwxyz"
      dout.writeUTF(s)

  def readNum(cbuf: BufferedInput, din: DataInputStream, i: Int): Unit =
    assertEquals(i.toByte, cbuf.int8())
    assertEquals((i+2).toShort, cbuf.int32())
    assertEquals(i+3, cbuf.int64())
    assertEquals(i.toByte, din.readByte())
    assertEquals((i+2).toShort, din.readInt())
    assertEquals(i+3, din.readLong())

  def readString(cbuf: BufferedInput, din: DataInputStream, i: Int): Unit =
    val s = "abcdefghijklmnopqrstuvwxyz"
    assert(cbuf.int16() == s.length)
    assert(cbuf.string(s.length) == s)
    assert(din.readUTF() == s)

  @Test def num: Unit = runTest(numTestData, readNum)
  @Test def string: Unit = runTest(stringTestData, readString)

  def runTest(td: TestData, read: (BufferedInput, DataInputStream, Int) => Unit): Unit =
    val crc1, crc2 = new CRC32
    def createBI(buf: Array[Byte]) =
      if(params.direct) BufferedInput.ofByteBuffer(directBB(buf))
      else BufferedInput.ofArray(buf)
    val cbuf =
      if(params.view1)
        val pad = Array.tabulate(10)(_.toByte)
        val buf = createBI(pad ++ td.bytes ++ pad)
        buf.bytes(10)
        TracingBufferedInput.checked(buf.limitedView(td.bytes.length), crc1)
      else
        val buf = createBI(td.bytes)
        TracingBufferedInput.checked(buf, crc1)
    val in = new ByteArrayInputStream(td.bytes)
    val cin = new CheckedInputStream(in, crc2)
    val din = new DataInputStream(cin)
    for i <- 0 until count do
      if(params.view2 && i%2 == 0)
        val v = cbuf.limitedView(Long.MaxValue)
        read(v, din, i)
        v.close()
      else
        read(cbuf, din, i)
      if(i % (count/10) == 0)
        if(params.update) cbuf.updateTrace()
    din.close()
    if(params.close) cbuf.close()
    else cbuf.updateTrace()
    assertEquals(crc2.getValue, crc1.getValue)

object TracingBufferedInputTest:
  case class Params(close: Boolean, blockSize: Int, update: Boolean, view1: Boolean, view2: Boolean, direct: Boolean):
    override def toString = s"close=$close,bs=$blockSize,update=$update,view1=$view1,view2=$view2,direct=$direct"

  @Parameterized.Parameters(name = "{0}")
  def parameters: java.util.List[Array[Any]] =
    val a: Array[Array[Any]] = for
      close <- Array(true, false)
      blockSize <- Array(64, 32768)
      update <- Array(false, true)
      view1 <- Array(false, true)
      view2 <- Array(false, true)
      direct <- Array(false, true)
    yield Array[Any](Params(close, blockSize, update, view1, view2, direct))
    java.util.List.of(a*)
