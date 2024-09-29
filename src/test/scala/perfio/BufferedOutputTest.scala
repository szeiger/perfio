package perfio

import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

import java.io.{EOFException, IOException}

@RunWith(classOf[JUnit4])
class BufferedOutputTest extends TestUtil {
  val count = 1000

  lazy val testData = createTestData("small") { dout =>
    for(i <- 0 until count) {
      dout.writeByte(i)
      dout.writeInt(i+2)
      dout.writeLong(i+3)
    }
  }

  lazy val viewTestData = createTestData("small") { dout =>
    for(i <- 0 until count) {
      dout.writeInt(13)
      dout.writeByte(i)
      dout.writeInt(i+2)
      dout.writeLong(i+3)
    }
  }

  @Test def simple(): Unit = {
    val (bo, checker) = testData.createBufferedOutputToOutputStream()
    assertEquals(0L, bo.totalBytesWritten)
    for(i <- 0 until count) {
      bo.int8(i.toByte)
      bo.int32(i+2)
      bo.int64(i+3)
    }
    assertEquals(count * 13, bo.totalBytesWritten)
    checker()
  }

  final class Counter(bo: BufferedOutput) {
    var bocount = 0
    def apply(i: Int): Unit = {
      bocount += i
      assertEquals(bocount, bo.totalBytesWritten)
    }
  }

  @Test def reserve(): Unit = {
    val (bo, checker) = viewTestData.createBufferedOutputToOutputStream()
    val cnt = new Counter(bo)
    for(i <- 0 until count) {
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
    }
    checker()
  }

  @Test def defer(): Unit = {
    val (bo, checker) = viewTestData.createBufferedOutputToOutputStream()
    val cnt = new Counter(bo)
    for(i <- 0 until count) {
      //println("****** "+i)
      cnt(0)
      val bo2 = bo.defer(4)
      cnt(0)
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
    }
    checker()
  }

  private def testFixed(padLeft: Int, padRight: Int): Unit = {
    val buf = new Array[Byte](count*13 + padLeft + padRight)
    val (bo, checker) = testData.createFixedBufferedOutput(buf, padLeft, count*13)
    assertEquals(0L, bo.totalBytesWritten)
    for(i <- 0 until count) {
      bo.int8(i.toByte)
      bo.int32(i+2)
      bo.int64(i+3)
    }
    assertEquals(count * 13, bo.totalBytesWritten)
    assertException[EOFException](bo.int8(0))
    checker()
  }

  @Test def fixedFull(): Unit = testFixed(0, 0)
  @Test def fixedOffset(): Unit = testFixed(25, 0)
  @Test def fixedLimit(): Unit = testFixed(0, 25)
  @Test def fixedBoth(): Unit = testFixed(139, 17)
}