package perfio

import com.esotericsoftware.kryo.io.Output
import org.openjdk.jmh.infra.Blackhole

import java.io.{DataInputStream, DataOutputStream, InputStream, OutputStream}
import java.nio.ByteBuffer
import java.util.Random

abstract class BenchmarkDataSet:
  def byteSize: Int
  def writeTo(out: OutputStream): Unit
  def writeTo(out: BufferedOutput): Unit
  def writeTo(out: Output): Unit // Kryo: little endian (safe) or native endian (unsafe)
  def writeTo(out: ByteBuffer): Unit
  def readFrom(bh: Blackhole, bin: BufferedInput): Unit
  def readFrom(bh: Blackhole, in: InputStream): Unit

object BenchmarkDataSet:
  def forName(s: String): BenchmarkDataSet = s match
    case "num" => NumDataSet
    case "numSmall" => NumDataSetSmall
    case "bytes" => BytesDataSet
    case "chunks" => ChunksDataSet
    case "chunksSlow50" => ChunksSlowDataSet50
    case "chunksSlow500" => ChunksSlowDataSet500
    case "chunksVerySlow" => ChunksVerySlowDataSet
    case "randomChunks" => RandomChunksDataSet


class NumDataSet(count: Int) extends BenchmarkDataSet:
  val byteSize = count * 13

  def writeTo(out: OutputStream): Unit =
    val dout = new DataOutputStream(out)
    var i = 0
    while i < count do
      dout.writeByte(i)
      dout.writeInt(i+100)
      dout.writeLong(i+101)
      i += 1
    dout.close()

  def writeTo(out: Output): Unit =
    var i = 0
    while i < count do
      out.writeByte(i)
      out.writeInt(i+100)
      out.writeLong(i+101)
      i += 1
    out.close()

  def writeTo(out: ByteBuffer): Unit =
    var i = 0
    while i < count do
      out.put(i.toByte)
      out.putInt(i+100)
      out.putLong(i+101)
      i += 1

  def writeTo(out: BufferedOutput): Unit =
    var i = 0
    while i < count do
      out.int8(i.toByte)
      out.int32b(i+100)
      out.int64b(i+101)
      i += 1
    out.close()

  def readFrom(bh: Blackhole, bin: BufferedInput): Unit = {
    var i = 0
    while i < count do
      bh.consume(bin.int8())
      bh.consume(bin.int32b())
      bh.consume(bin.int64b())
      i += 1
    bin.close()
  }

  def readFrom(bh: Blackhole, in: InputStream): Unit = {
    val din = new DataInputStream(in)
    var i = 0
    while i < count do
      bh.consume(din.readByte())
      bh.consume(din.readInt())
      bh.consume(din.readLong())
      i += 1
    din.close()
  }

object NumDataSet extends NumDataSet(20000000)
object NumDataSetSmall extends NumDataSet(1000000)


object BytesDataSet extends BenchmarkDataSet:
  val count = 100000000
  val byteSize = count

  def writeTo(out: OutputStream): Unit =
    var i = 0
    while i < count do
      out.write(i.toByte)
      i += 1
    out.close()

  def writeTo(out: Output): Unit =
    var i = 0
    while i < count do
      out.writeByte(i.toByte)
      i += 1
    out.close()

  def writeTo(out: ByteBuffer): Unit =
    var i = 0
    while i < count do
      out.put(i.toByte)
      i += 1

  def writeTo(out: BufferedOutput): Unit =
    var i = 0
    while i < count do
      out.int8(i.toByte)
      i += 1
    out.close()

  def readFrom(bh: Blackhole, bin: BufferedInput): Unit = {
    var i = 0
    while i < count do
      bh.consume(bin.int8())
      i += 1
    bin.close()
  }

  def readFrom(bh: Blackhole, in: InputStream): Unit = {
    var i = 0
    while i < count do
      bh.consume(in.read().toByte)
      i += 1
    in.close()
  }


object ChunksDataSet extends CompressibleChunksDataSet(1024, 0)

object RandomChunksDataSet extends RandomChunksDataSet(1024, 0)

object ChunksSlowDataSet50 extends CompressibleChunksDataSet(1024, 50000)

object ChunksSlowDataSet500 extends CompressibleChunksDataSet(1024, 500000)

object ChunksVerySlowDataSet extends CompressibleChunksDataSet(1024, 5000, 10)

class CompressibleChunksDataSet(chunkSize: Int, val delay: Int = 0, val delayInterval: Int = 1000) extends ChunksDataSet:
  val count = ((100000L * 1024) / chunkSize).toInt
  val chunk = Array.tabulate[Byte](chunkSize)(_.toByte)
  val byteSize = count * chunkSize

class RandomChunksDataSet(chunkSize: Int, val delay: Int) extends ChunksDataSet:
  val rnd = new Random(0L)
  val count = ((100000L * 1024) / chunkSize).toInt
  val chunk = Array.fill[Byte](chunkSize)(rnd.nextInt().toByte)
  val byteSize = count * chunkSize
  val delayInterval = 1000

abstract class ChunksDataSet extends BenchmarkDataSet:
  val delay: Int
  val delayInterval: Int
  val count: Int
  val chunk: Array[Byte]
  val byteSize: Int

  def writeTo(out: OutputStream): Unit =
    var i = 0
    while i < count do
      out.write(chunk)
      if(i % delayInterval == 0) Thread.sleep(0, delay)
      i += 1
    out.close()

  def writeTo(out: Output): Unit =
    var i = 0
    while i < count do
      out.write(chunk)
      if(i % delayInterval == 0) Thread.sleep(0, delay)
      i += 1
    out.close()

  def writeTo(out: ByteBuffer): Unit =
    var i = 0
    while i < count do
      out.put(chunk)
      if(i % delayInterval == 0) Thread.sleep(0, delay)
      i += 1

  def writeTo(out: BufferedOutput): Unit =
    var i = 0
    while i < count do
      out.write(chunk)
      if(i % delayInterval == 0) Thread.sleep(0, delay)
      i += 1
    out.close()

  def readFrom(bh: Blackhole, bin: BufferedInput): Unit = {
    val a = new Array[Byte](1024)
    var i = 0
    while i < count do
      bin.bytes(a, 0, a.length)
      bh.consume(a)
      i += 1
    bin.close()
  }

  def readFrom(bh: Blackhole, in: InputStream): Unit = {
    val a = new Array[Byte](1024)
    var i = 0
    while i < count do
      in.read(a)
      bh.consume(a)
      i += 1
    in.close()
  }
