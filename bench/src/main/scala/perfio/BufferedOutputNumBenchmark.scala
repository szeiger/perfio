package perfio

import com.esotericsoftware.kryo.io.{ByteBufferOutput, Output}
import com.esotericsoftware.kryo.unsafe.{UnsafeByteBufferOutput, UnsafeOutput}
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.*
import perfio.internal.BufferUtil

import java.io.*
import java.nio.ByteBuffer
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

@BenchmarkMode(Array(Mode.AverageTime))
@Fork(value = 1, jvmArgsAppend = Array("-Xmx12g", "-Xss32M", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC",
  "--enable-native-access=ALL-UNNAMED", "--add-modules", "jdk.incubator.vector",
  "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED" // for KryoUnsafe
))
@Threads(1)
@Warmup(iterations = 25, time = 1)
@Measurement(iterations = 10, time = 1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
class BufferedOutputNumBenchmark extends BenchUtil:

  val count = 20000000
  val byteSize = count * 13

  // JDK: always big endian
  private def writeTo(out: DataOutputStream): Unit =
    var i = 0
    while i < count do
      out.writeByte(i)
      out.writeInt(i+100)
      out.writeLong(i+101)
      i += 1
    out.close()

  // Kryo: little endian (safe) or native endian (unsafe)
  private def writeTo(out: Output): Unit =
    var i = 0
    while i < count do
      out.writeByte(i)
      out.writeInt(i+100)
      out.writeLong(i+101)
      i += 1
    out.close()

  private def writeTo(out: BufferedOutput): Unit =
    var i = 0
    while i < count do
      out.int8(i.toByte)
      out.int32(i+100)
      out.int64(i+101)
      i += 1
    out.close()

  private def writeInternalTo(out: BufferedOutput): Unit =
    var i = 0
    while i < count do
      val p = out.fwd(13)
      out.buf(p) = i.toByte
      BufferUtil.BA_INT_BIG.set(out.buf, p+1, i+100)
      BufferUtil.BA_LONG_BIG.set(out.buf, p+5, (i+101).toLong)
      i += 1
    out.close()

  @Benchmark
  def array_DataOutputStream_growing(bh: Blackhole): Unit =
    val bout = new MyByteArrayOutputStream
    val out = new DataOutputStream(bout)
    writeTo(out)
    bh.consume(bout.getSize)
    bh.consume(bout.getBuffer)

  @Benchmark
  def array_DataOutputStream_preallocated(bh: Blackhole): Unit =
    val bout = new MyByteArrayOutputStream(count * 13)
    val out = new DataOutputStream(bout)
    writeTo(out)
    bh.consume(bout.getSize)
    bh.consume(bout.getBuffer)

  @Benchmark
  def array_Kryo_growing(bh: Blackhole): Unit =
    val bout = new MyByteArrayOutputStream
    val out = new Output(bout)
    writeTo(out)
    bh.consume(bout.getSize)
    bh.consume(bout.getBuffer)

  @Benchmark
  def array_Kryo_preallocated(bh: Blackhole): Unit =
    val out = new Output(count * 13)
    writeTo(out)
    bh.consume(out.position())

  @Benchmark
  def array_KryoUnsafe_growing(bh: Blackhole): Unit =
    val bout = new MyByteArrayOutputStream
    val out = new UnsafeOutput(bout)
    writeTo(out)
    bh.consume(bout.getSize)
    bh.consume(bout.getBuffer)

  @Benchmark
  def array_KryoUnsafe_preallocated(bh: Blackhole): Unit =
    val bb = ByteBuffer.allocate(count * 13)
    val out = new UnsafeOutput(count * 13)
    writeTo(out)
    bh.consume(out.position())

  @Benchmark
  def array_KryoBB_growing(bh: Blackhole): Unit =
    val bout = new MyByteArrayOutputStream
    val out = new ByteBufferOutput(bout)
    writeTo(out)
    bh.consume(bout.getSize)
    bh.consume(bout.getBuffer)

  @Benchmark
  def array_KryoBB_preallocated(bh: Blackhole): Unit =
    val bb = ByteBuffer.allocate(count * 13)
    val out = new ByteBufferOutput(bb)
    writeTo(out)
    bh.consume(out.position())

  @Benchmark
  def array_KryoBBUnsafe_growing(bh: Blackhole): Unit =
    val bout = new MyByteArrayOutputStream
    val out = new UnsafeByteBufferOutput(bout)
    writeTo(out)
    bh.consume(bout.getSize)
    bh.consume(bout.getBuffer)

  @Benchmark
  def array_KryoBBUnsafe_preallocated(bh: Blackhole): Unit =
    val bb = ByteBuffer.allocate(count * 13)
    val out = new UnsafeByteBufferOutput(count * 13)
    writeTo(out)
    bh.consume(out.position())

  @Benchmark
  def array_FlushingBufferedOutput_growing(bh: Blackhole): Unit =
    val bout = new MyByteArrayOutputStream
    val out = BufferedOutput.of(bout)
    writeTo(out)
    bh.consume(bout.getSize)
    bh.consume(bout.getBuffer)

  @Benchmark
  def array_FlushingBufferedOutput_fixed(bh: Blackhole): Unit =
    val bout = new MyByteArrayOutputStream(count * 13)
    val out = BufferedOutput.of(bout)
    writeTo(out)
    bh.consume(bout.getSize)
    bh.consume(bout.getBuffer)

  @Benchmark
  def array_FlushingBufferedOutput_internal_fixed(bh: Blackhole): Unit =
    val bout = new MyByteArrayOutputStream(count * 13)
    val out = BufferedOutput.of(bout)
    writeInternalTo(out)
    bh.consume(bout.getSize)
    bh.consume(bout.getBuffer)

  @Benchmark
  def array_FullyBufferedOutput_growing(bh: Blackhole): Unit =
    val out = BufferedOutput.growing()
    writeTo(out)
    bh.consume(out.buffer)
    bh.consume(out.length)

  @Benchmark
  def array_FullyBufferedOutput_growing_preallocated(bh: Blackhole): Unit =
    val out = BufferedOutput.growing(count*13)
    writeTo(out)
    bh.consume(out.buffer)
    bh.consume(out.length)

  @Benchmark
  def array_FullyBufferedOutput_fixed(bh: Blackhole): Unit =
    val out = BufferedOutput.ofArray(new Array[Byte](count*13))
    writeTo(out)
    bh.consume(out.buffer)
    bh.consume(out.length)

  @Benchmark
  def array_ByteBuffer(bh: Blackhole): Unit =
    val out = ByteBuffer.allocate(count*13)
    var i = 0
    while i < count do
      out.put(i.toByte)
      out.putInt(i+100)
      out.putLong(i+101)
      i += 1
    bh.consume(out)

  @Benchmark
  def file_DataOutputStream(bh: Blackhole): Unit =
    val fout = new FileOutputStream("/dev/null")
    val bout = new BufferedOutputStream(fout)
    val out = new DataOutputStream(bout)
    writeTo(out)
    out.close()

  @Benchmark
  def file_FlushingBufferedOutput(bh: Blackhole): Unit =
    val out = BufferedOutput.ofFile(Paths.get("/dev/null"))
    writeTo(out)

  @Benchmark
  def file_Kryo(bh: Blackhole): Unit =
    val fout = new FileOutputStream("/dev/null")
    val out = new Output(fout)
    writeTo(out)

  @Benchmark
  def file_KryoUnsafe(bh: Blackhole): Unit =
    val fout = new FileOutputStream("/dev/null")
    val out = new UnsafeOutput(fout)
    writeTo(out)

  @Benchmark
  def file_KryoBB(bh: Blackhole): Unit =
    val fout = new FileOutputStream("/dev/null")
    val out = new ByteBufferOutput(fout)
    writeTo(out)

  @Benchmark
  def file_KryoBBUnsafe(bh: Blackhole): Unit =
    val fout = new FileOutputStream("/dev/null")
    val out = new UnsafeByteBufferOutput(fout)
    writeTo(out)
