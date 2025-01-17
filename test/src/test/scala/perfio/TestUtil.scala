package perfio

import org.junit.Assert

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream, File, FileOutputStream, InputStream}
import java.lang.foreign.{Arena, MemorySegment, ValueLayout}
import java.nio.{ByteBuffer, ByteOrder}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.reflect.ClassTag

trait TestUtil:
  def toGlobal(testData: Array[Byte]): MemorySegment =
    val m = Arena.global().allocate(testData.length, 8)
    MemorySegment.copy(testData, 0, m, ValueLayout.JAVA_BYTE, 0, testData.length)
    m

  def createTestData(name: String)(f: DataOutputStream => Unit): TestData =
    val out = new ByteArrayOutputStream()
    val dout = new DataOutputStream(out)
    f(dout)
    new TestData(out.toByteArray, name, getClass)

  def reverseByteOrder(i: Int): Int =
    ByteBuffer.wrap(new Array[Byte](4)).order(ByteOrder.BIG_ENDIAN).putInt(0, i).order(ByteOrder.LITTLE_ENDIAN).getInt(0)

  def reverseByteOrder(l: Long): Long =
    ByteBuffer.wrap(new Array[Byte](8)).order(ByteOrder.BIG_ENDIAN).putLong(0, l).order(ByteOrder.LITTLE_ENDIAN).getLong(0)

  def assertException[T <: Throwable](f: => Unit)(implicit ct: ClassTag[T]): Unit =
    var caught = false
    try f catch
      case t: Throwable if ct.runtimeClass.isInstance(t) => caught = true
    if(!caught) Assert.fail(s"Expected exception ${ct.runtimeClass.getName}")


class TestData(val bytes: Array[Byte], val name: String, owner: Class[?]):
  private val file = new File(s"/tmp/test-data-${owner.getName}-$name")
  val length = bytes.length

  def getFile(): File =
    if(!file.exists())
      val out = new FileOutputStream(file)
      out.write(bytes)
      out.close()
    file

  def createDataInput() = new DataInputStream(new ByteArrayInputStream(bytes))

  def createBufferedInputFromByteArrayInputStream(initialBufferSize: Int = 64, limit: Int = -1): BufferedInput =
    var in: InputStream = new ByteArrayInputStream(bytes)
    if(limit > 0) in = new LimitedInputStream(in, limit)
    BufferedInput.of(in, initialBufferSize)

  def createBufferedInputFromMappedFile(maxDirectBufferSize: Int = 128): BufferedInput =
    BufferedInput.MaxDirectBufferSize = maxDirectBufferSize // ensure that we test rebuffering
    BufferedInput.ofMappedFile(getFile().toPath)

  def createBufferedInputFromArray(): BufferedInput = BufferedInput.ofArray(bytes)

  def createBufferedOutputToOutputStream(initialBufferSize: Int = 64): (BufferedOutput, () => Unit) =
    val bout = new ByteArrayOutputStream()
    val bo = BufferedOutput.of(bout, initialBufferSize)
    val checker = () =>
      bo.close()
      val a = bout.toByteArray
      Assert.assertArrayEquals(bytes, a)
    (bo, checker)

  def createGrowingBufferedOutput(initialBufferSize: Int = 64): (BufferedOutput, () => Unit) =
    val bo = BufferedOutput.growing(initialBufferSize)
    val checker = () =>
      bo.close()
      val a = bo.copyToByteArray
      Assert.assertArrayEquals(bytes, a)
    (bo, checker)

  def createBlockBufferedOutput(initialBufferSize: Int = 64): (BufferedOutput, () => Unit) =
    val bo = BufferedOutput.ofBlocks(initialBufferSize)
    val checker = () =>
      bo.close()
      val in = bo.toInputStream
      Assert.assertArrayEquals(bytes, in.readAllBytes())
    (bo, checker)

  def createPipeBufferedOutput(initialBufferSize: Int = 64): (BufferedOutput, () => Unit) =
    val bo = BufferedOutput.pipe(initialBufferSize)
    val res = Future { bo.toInputStream.readAllBytes() }(ExecutionContext.global)
    val checker = () =>
      bo.close()
      Assert.assertArrayEquals(bytes, Await.result(res, Duration.Inf))
    (bo, checker)

  def createFixedBufferedOutput(buf: Array[Byte], start: Int = 0, len: Int = -1): (BufferedOutput, () => Unit) =
    val bo = BufferedOutput.ofArray(buf, start, if(len == -1) buf.length-start else len)
    val checker = () =>
      bo.close()
      val checkLen = bo.totalBytesWritten.toInt
      Assert.assertArrayEquals("Array slice should match", bytes, buf.slice(start, start+checkLen))
      Assert.assertArrayEquals("Array prefix should be empty", new Array[Byte](start), buf.slice(0, start))
      Assert.assertArrayEquals("Array suffix should be empty", new Array[Byte](buf.length-start-checkLen), buf.slice(start + checkLen, buf.length))
    (bo, checker)


class LimitedInputStream(in: InputStream, limit: Int) extends InputStream:
  def read(): Int = in.read()
  override def read(b: Array[Byte], off: Int, len: Int): Int = in.read(b, off, len min limit)
