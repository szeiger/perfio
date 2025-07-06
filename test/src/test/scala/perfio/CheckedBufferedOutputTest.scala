package perfio

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import java.io.{ByteArrayOutputStream, DataOutputStream}
import java.util.zip.{CRC32, CheckedOutputStream}

@RunWith(classOf[Parameterized])
class CheckedBufferedOutputTest(params: CheckedBufferedOutputTest.Params) extends TestUtil:
  val count = 1000

  def writeNum(cbuf: CheckedBufferedOutput, dout: DataOutputStream, i: Int): Unit =
    cbuf.int8(i.toByte).int32(i+2).int64(i+3)
    dout.writeByte(i)
    dout.writeInt(i+2)
    dout.writeLong(i+3)

  def writeString(cbuf: CheckedBufferedOutput, dout: DataOutputStream, i: Int): Unit =
    val s = "abcdefghijklmnopqrstuvwxyz"
    cbuf.int16(s.length.toShort).string(s)
    dout.writeUTF(s)

  @Test def num: Unit = runTest(writeNum)
  @Test def string: Unit = runTest(writeString)

  def runTest(write: (CheckedBufferedOutput, DataOutputStream, Int) => Unit): Unit =
    val crc1, crc2 = new CRC32
    val buf = BufferedOutput.growing(params.blockSize, params.blockSize)
    val cbuf = new CheckedBufferedOutput(buf, crc1, params.flushPartial)
    val out = new ByteArrayOutputStream()
    val cout = new CheckedOutputStream(out, crc2)
    val dout = new DataOutputStream(cout)
    for i <- 0 until count do
      write(cbuf, dout, i)
      if(i % (count/10) == 0)
        if(params.update) cbuf.updateChecksum()
        if(params.flush) cbuf.flush()
    dout.close()
    if(params.close) cbuf.close()
    else cbuf.updateChecksum()
    assertEquals(crc2.getValue, crc1.getValue)

object CheckedBufferedOutputTest:
  case class Params(close: Boolean, blockSize: Int, update: Boolean, flush: Boolean, flushPartial: Boolean):
    override def toString = s"close=$close,bs=$blockSize,update=$update,flush=$flush,partial=$flushPartial"

  @Parameterized.Parameters(name = "{0}")
  def parameters: java.util.List[Array[Any]] =
    val a: Array[Array[Any]] = for
      close <- Array(true, false)
      blockSize <- Array(64, 32768)
      update <- Array(true, false)
      flush <- Array(true, false)
      flushPartial <- Array(true, false)
    yield Array[Any](Params(close, blockSize, update, flush, flushPartial))
    java.util.List.of(a*)
