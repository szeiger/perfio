package perfio

import java.io.{File, FileOutputStream}
import java.lang.foreign.{Arena, MemorySegment, ValueLayout}

class BenchUtil {
  def writeFileIfMissing(name: String, testData: Array[Byte], minLength: Long = 1L): File = {
    val f = new File(s"/tmp/benchmark-test-data-${getClass.getName}-$name")
    if(!f.exists()) {
      val out = new FileOutputStream(f)
      var written = 0L
      while(written < minLength) {
        out.write(testData)
        written += testData.length
      }
      out.close()
    }
    f
  }

  def toGlobal(testData: Array[Byte]): MemorySegment = {
    val m = Arena.global().allocate(testData.length, 8)
    MemorySegment.copy(testData, 0, m, ValueLayout.JAVA_BYTE, 0, testData.length)
    m
  }
}
