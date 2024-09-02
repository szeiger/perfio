package perfio

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream, File, FileOutputStream}
import java.lang.foreign.{Arena, MemorySegment, ValueLayout}

trait TestUtil {
  def toGlobal(testData: Array[Byte]): MemorySegment = {
    val m = Arena.global().allocate(testData.length, 8)
    MemorySegment.copy(testData, 0, m, ValueLayout.JAVA_BYTE, 0, testData.length)
    m
  }

  def createTestData(name: String)(f: DataOutputStream => Unit): TestData = {
    val out = new ByteArrayOutputStream()
    val dout = new DataOutputStream(out)
    f(dout)
    new TestData(out.toByteArray, name, getClass)
  }
}

class TestData(val bytes: Array[Byte], val name: String, owner: Class[_]) {
  private[this] val file = new File(s"/tmp/test-data-${owner.getName}-$name")
  val length = bytes.length

  def getFile(): File = {
    if(!file.exists()) {
      val out = new FileOutputStream(file)
      out.write(bytes)
      out.close()
    }
    file
  }

  def createDataInput() = new DataInputStream(new ByteArrayInputStream(bytes))

  def createBufferedInputFromByteArrayInputStream(initialBufferSize: Int = 64): BufferedInput =
    BufferedInput(new ByteArrayInputStream(bytes), initialBufferSize = initialBufferSize)
  def createBufferedInputFromMappedFile(maxDirectBufferSize: Int = 128): BufferedInput = {
    BufferedInput.MaxDirectBufferSize = maxDirectBufferSize // ensure that we test rebuffering
    BufferedInput.fromMappedFile(getFile().toPath)
  }
  def createBufferedInputFromArray(): BufferedInput = BufferedInput.fromArray(bytes)
}
