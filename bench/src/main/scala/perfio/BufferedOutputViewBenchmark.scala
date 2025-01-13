package perfio

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.*

import java.io.*
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

@BenchmarkMode(Array(Mode.AverageTime))
@Fork(value = 1, jvmArgsAppend = Array("-Xmx12g", "-Xss32M", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC", "--enable-native-access=ALL-UNNAMED", "--add-modules", "jdk.incubator.vector"))
@Threads(1)
@Warmup(iterations = 12, time = 1)
@Measurement(iterations = 10, time = 1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
class BufferedOutputViewBenchmark extends BenchUtil:

  val count = 20000000
  val div = 10
  val byteSize = (count * 13) + (count / div * 4)

  private def writeDirectTo(out: DataOutputStream): Unit =
    var i = 0
    while i < count/div do
      out.writeInt(13 * div)
      var j = 0
      while j < div do
        out.writeByte(i+j)
        out.writeInt(i+j+100)
        out.writeLong(i+j+101)
        j += 1
      i += 1
    out.close()

  private def writeNestedTo(out: DataOutputStream): Unit =
    val b2 = new MyByteArrayOutputStream()
    val d2 = new DataOutputStream(b2)
    var i = 0
    while i < count/div do
      var j = 0
      while j < div do
        d2.writeByte(i+j)
        d2.writeInt(i+j+100)
        d2.writeLong(i+j+101)
        j += 1
      out.writeInt(b2.getSize)
      out.write(b2.getBuffer, 0, b2.getSize)
      b2.reset()
      i += 1
    out.close()

  private def writeDirectTo(out: BufferedOutput): Unit =
    var i = 0
    while i < count/div do
      out.int32(13 * div)
      var j = 0
      while j < div do
        out.int8((i+j).toByte)
        out.int32(i+j+100)
        out.int64(i+j+101)
        j += 1
      i += 1
    out.close()

  private def writeReserveTo(out: BufferedOutput): Unit =
    var i = 0
    while i < count/div do
      val o2 = out.reserve(4)
      val pos = out.totalBytesWritten
      var j = 0
      while j < div do
        out.int8((i+j).toByte)
        out.int32(i+j+100)
        out.int64(i+j+101)
        j += 1
      o2.int32((out.totalBytesWritten-pos).toInt)
      o2.close()
      i += 1
    out.close()

  private def writeDeferTo(out: BufferedOutput): Unit =
    var i = 0
    while i < count/div do
      val o2 = out.defer()
      var j = 0
      while j < div do
        o2.int8((i+j).toByte)
        o2.int32(i+j+100)
        o2.int64(i+j+101)
        j += 1
      out.int32(o2.totalBytesWritten.toInt)
      o2.close()
      i += 1
    out.close()

//  @Benchmark
//  def array_DataOutputStream_direct_preallocated(bh: Blackhole): Unit =
//    val bout = new MyByteArrayOutputStream(byteSize)
//    val out = new DataOutputStream(bout)
//    writeDirectTo(out)
//    assert(bout.getSize == byteSize)
//    bh.consume(bout.getSize)
//    bh.consume(bout.getBuffer)
//
//  @Benchmark
//  def array_DataOutputStream_nested_preallocated(bh: Blackhole): Unit =
//    val bout = new MyByteArrayOutputStream(byteSize)
//    val out = new DataOutputStream(bout)
//    writeNestedTo(out)
//    assert(bout.getSize == byteSize)
//    bh.consume(bout.getSize)
//    bh.consume(bout.getBuffer)

  @Benchmark
  def array_FlushingBufferedOutput_direct_preallocated(bh: Blackhole): Unit =
    val bout = new MyByteArrayOutputStream(byteSize)
    val out = BufferedOutput.of(bout)
    writeDirectTo(out)
    bh.consume(bout.getSize)
    bh.consume(bout.getBuffer)

  @Benchmark
  def array_FlushingBufferedOutput_reserve_preallocated(bh: Blackhole): Unit =
    val bout = new MyByteArrayOutputStream(byteSize)
    val out = BufferedOutput.of(bout)
    writeReserveTo(out)
    bh.consume(bout.getSize)
    bh.consume(bout.getBuffer)

  @Benchmark
  def array_FlushingBufferedOutput_defer_preallocated(bh: Blackhole): Unit =
    val bout = new MyByteArrayOutputStream(byteSize)
    val out = BufferedOutput.of(bout)
    writeDeferTo(out)
    bh.consume(bout.getSize)
    bh.consume(bout.getBuffer)

  @Benchmark
  def array_FullyBufferedOutput_reserve_fixed(bh: Blackhole): Unit =
    val out = BufferedOutput.ofArray(new Array[Byte](byteSize))
    writeReserveTo(out)
    bh.consume(out.buffer)
    bh.consume(out.length)

  @Benchmark
  def array_FullyBufferedOutput_defer_fixed(bh: Blackhole): Unit =
    val out = BufferedOutput.ofArray(new Array[Byte](byteSize))
    writeDeferTo(out)
    bh.consume(out.buffer)
    bh.consume(out.length)

//  @Benchmark
//  def file_DataOutputStream_direct(bh: Blackhole): Unit =
//    val fout = new FileOutputStream("/dev/null")
//    val bout = new BufferedOutputStream(fout)
//    val out = new DataOutputStream(bout)
//    writeDirectTo(out)
//    out.close()
//
//  @Benchmark
//  def file_DataOutputStream_nested(bh: Blackhole): Unit =
//    val fout = new FileOutputStream("/dev/null")
//    val bout = new BufferedOutputStream(fout)
//    val out = new DataOutputStream(bout)
//    writeNestedTo(out)
//    out.close()

  @Benchmark
  def file_FlushingBufferedOutput_direct(bh: Blackhole): Unit =
    val out = BufferedOutput.ofFile(Paths.get("/dev/null"))
    writeDirectTo(out)
    out.close()

  @Benchmark
  def file_FlushingBufferedOutput_reserve(bh: Blackhole): Unit =
    val out = BufferedOutput.ofFile(Paths.get("/dev/null"))
    writeReserveTo(out)
    out.close()

  @Benchmark
  def file_FlushingBufferedOutput_defer(bh: Blackhole): Unit =
    val out = BufferedOutput.ofFile(Paths.get("/dev/null"))
    writeDeferTo(out)
    out.close()
