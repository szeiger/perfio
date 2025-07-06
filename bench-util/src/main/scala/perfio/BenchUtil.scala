package perfio

import org.openjdk.jmh.infra.Blackhole

import java.io.{ByteArrayOutputStream, File, FileOutputStream}
import java.lang.foreign.{Arena, MemorySegment, ValueLayout}

class BenchUtil:
  def writeFileIfMissing(name: String, testData: Array[Byte], minLength: Long = 1L): File =
    val f = new File(s"/tmp/benchmark-test-data-${getClass.getName}-$name")
    if(!f.exists())
      val out = new FileOutputStream(f)
      var written = 0L
      while written < minLength do
        out.write(testData)
        written += testData.length
      out.close()
    f

  def toGlobal(testData: Array[Byte]): MemorySegment = {
    val m = Arena.global().allocate(testData.length, 8)
    MemorySegment.copy(testData, 0, m, ValueLayout.JAVA_BYTE, 0, testData.length)
    m
  }

  def runWarmup(f: Blackhole => Unit): Unit = {
    val bh = new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.")
    f(bh)
  }


class MyByteArrayOutputStream(capacity: Int = 32768) extends ByteArrayOutputStream(capacity):
  def getBuffer = buf
  def getSize = count
