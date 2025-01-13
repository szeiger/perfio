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

  @Param(Array("num"))
  var dataSet: String = null
  final lazy val data = BenchmarkDataSet.forName(dataSet)
  import data._

//  @Benchmark
//  def array_DataOutputStream_growing(bh: Blackhole): Unit =
//    val bout = new MyByteArrayOutputStream
//    writeTo(bout)
//    bh.consume(bout.getSize)
//    bh.consume(bout.getBuffer)
//
//  @Benchmark
//  def array_DataOutputStream_preallocated(bh: Blackhole): Unit =
//    val bout = new MyByteArrayOutputStream(byteSize)
//    writeTo(bout)
//    bh.consume(bout.getSize)
//    bh.consume(bout.getBuffer)
//
//  @Benchmark
//  def array_Kryo_growing(bh: Blackhole): Unit =
//    val bout = new MyByteArrayOutputStream
//    val out = new Output(bout)
//    writeTo(out)
//    bh.consume(bout.getSize)
//    bh.consume(bout.getBuffer)
//
//  @Benchmark
//  def array_Kryo_preallocated(bh: Blackhole): Unit =
//    val out = new Output(byteSize)
//    writeTo(out)
//    bh.consume(out.position())
//
//  @Benchmark
//  def array_KryoUnsafe_growing(bh: Blackhole): Unit =
//    val bout = new MyByteArrayOutputStream
//    val out = new UnsafeOutput(bout)
//    writeTo(out)
//    bh.consume(bout.getSize)
//    bh.consume(bout.getBuffer)
//
//  @Benchmark
//  def array_KryoUnsafe_preallocated(bh: Blackhole): Unit =
//    val bb = ByteBuffer.allocate(byteSize)
//    val out = new UnsafeOutput(byteSize)
//    writeTo(out)
//    bh.consume(out.position())
//
//  @Benchmark
//  def array_KryoBB_growing(bh: Blackhole): Unit =
//    val bout = new MyByteArrayOutputStream
//    val out = new ByteBufferOutput(bout)
//    writeTo(out)
//    bh.consume(bout.getSize)
//    bh.consume(bout.getBuffer)
//
//  @Benchmark
//  def array_KryoBB_preallocated(bh: Blackhole): Unit =
//    val bb = ByteBuffer.allocate(byteSize)
//    val out = new ByteBufferOutput(bb)
//    writeTo(out)
//    bh.consume(out.position())
//
//  @Benchmark
//  def array_KryoBBUnsafe_growing(bh: Blackhole): Unit =
//    val bout = new MyByteArrayOutputStream
//    val out = new UnsafeByteBufferOutput(bout)
//    writeTo(out)
//    bh.consume(bout.getSize)
//    bh.consume(bout.getBuffer)
//
//  @Benchmark
//  def array_KryoBBUnsafe_preallocated(bh: Blackhole): Unit =
//    val bb = ByteBuffer.allocate(byteSize)
//    val out = new UnsafeByteBufferOutput(byteSize)
//    writeTo(out)
//    bh.consume(out.position())

  @Benchmark
  def array_FlushingBufferedOutput_growing(bh: Blackhole): Unit =
    val bout = new MyByteArrayOutputStream
    val out = BufferedOutput.of(bout)
    writeTo(out)
    bh.consume(bout.getSize)
    bh.consume(bout.getBuffer)

  @Benchmark
  def array_FlushingBufferedOutput_fixed(bh: Blackhole): Unit =
    val bout = new MyByteArrayOutputStream(byteSize)
    val out = BufferedOutput.of(bout)
    writeTo(out)
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
    val out = BufferedOutput.growing(byteSize)
    writeTo(out)
    bh.consume(out.buffer)
    bh.consume(out.length)

  @Benchmark
  def array_FullyBufferedOutput_fixed(bh: Blackhole): Unit =
    val out = BufferedOutput.ofArray(new Array[Byte](byteSize))
    writeTo(out)
    bh.consume(out.buffer)
    bh.consume(out.length)

//  @Benchmark
//  def array_ByteBuffer(bh: Blackhole): Unit =
//    val out = ByteBuffer.allocate(byteSize)
//    writeTo(out)
//    bh.consume(out)
//
//  @Benchmark
//  def file_DataOutputStream(bh: Blackhole): Unit =
//    val fout = new FileOutputStream("/dev/null")
//    val bout = new BufferedOutputStream(fout)
//    writeTo(bout)

  @Benchmark
  def file_FlushingBufferedOutput(bh: Blackhole): Unit =
    val out = BufferedOutput.ofFile(Paths.get("/dev/null"))
    writeTo(out)

//  @Benchmark
//  def file_Kryo(bh: Blackhole): Unit =
//    val fout = new FileOutputStream("/dev/null")
//    val out = new Output(fout)
//    writeTo(out)
//
//  @Benchmark
//  def file_KryoUnsafe(bh: Blackhole): Unit =
//    val fout = new FileOutputStream("/dev/null")
//    val out = new UnsafeOutput(fout)
//    writeTo(out)
//
//  @Benchmark
//  def file_KryoBB(bh: Blackhole): Unit =
//    val fout = new FileOutputStream("/dev/null")
//    val out = new ByteBufferOutput(fout)
//    writeTo(out)
//
//  @Benchmark
//  def file_KryoBBUnsafe(bh: Blackhole): Unit =
//    val fout = new FileOutputStream("/dev/null")
//    val out = new UnsafeByteBufferOutput(fout)
//    writeTo(out)
