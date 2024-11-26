package perfio

import com.esotericsoftware.kryo.io.Output
import org.openjdk.jmh.infra.Blackhole

import java.io.{DataInputStream, DataOutputStream, InputStream, OutputStream}
import java.nio.ByteBuffer

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
    case "bytes" => BytesDataSet
    case "chunks" => ChunksDataSet


object NumDataSet extends BenchmarkDataSet:
  val count = 20000000
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


object ChunksDataSet extends BenchmarkDataSet:
  val count = 100000
  val chunk = Array.tabulate[Byte](1024)(_.toByte)
  val byteSize = count * 1024

  def writeTo(out: OutputStream): Unit =
    var i = 0
    while i < count do
      out.write(chunk)
      i += 1
    out.close()

  def writeTo(out: Output): Unit =
    var i = 0
    while i < count do
      out.write(chunk)
      i += 1
    out.close()

  def writeTo(out: ByteBuffer): Unit =
    var i = 0
    while i < count do
      out.put(chunk)
      i += 1

  def writeTo(out: BufferedOutput): Unit =
    var i = 0
    while i < count do
      out.write(chunk)
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
