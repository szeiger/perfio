package perfio

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra._

import java.io._
import java.util.concurrent.TimeUnit

@BenchmarkMode(Array(Mode.AverageTime))
@Fork(value = 1, jvmArgsAppend = Array("-Xmx12g", "-Xss32M", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC", "--enable-native-access=ALL-UNNAMED", "--add-modules", "jdk.incubator.vector"))
@Threads(1)
@Warmup(iterations = 12, time = 1)
@Measurement(iterations = 10, time = 1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
class BufferedOutputViewBenchmark extends BenchUtil {

  val count = 20000000
  val div = 10
  val byteSize = (count * 13) + (count / div * 4)

  private[this] def writeDirectTo(out: DataOutputStream): Unit = {
    var i = 0
    while(i < count/div) {
      out.writeInt(13 * div)
      var j = 0
      while(j < div) {
        out.writeByte(i+j)
        out.writeInt(i+j+100)
        out.writeLong(i+j+101)
        j += 1
      }
      i += 1
    }
    out.flush()
  }

  private[this] def writeNestedTo(out: DataOutputStream): Unit = {
    val b2 = new MyByteArrayOutputStream()
    val d2 = new DataOutputStream(b2)
    var i = 0
    while(i < count/div) {
      var j = 0
      while(j < div) {
        d2.writeByte(i+j)
        d2.writeInt(i+j+100)
        d2.writeLong(i+j+101)
        j += 1
      }
      out.writeInt(b2.getSize)
      out.write(b2.getBuffer, 0, b2.getSize)
      b2.reset()
      i += 1
    }
    out.flush()
  }

  private[this] def writeDirectTo(out: BufferedOutput): Unit = {
    var i = 0
    while(i < count/div) {
      out.int32(13 * div)
      var j = 0
      while(j < div) {
        out.int8((i+j).toByte)
        out.int32(i+j+100)
        out.int64(i+j+101)
        j += 1
      }
      i += 1
    }
    out.flush()
  }

  private[this] def writeReserveTo(out: BufferedOutput): Unit = {
    var i = 0
    while(i < count/div) {
      val o2 = out.reserve(4)
      val pos = out.totalBytesWritten
      var j = 0
      while(j < div) {
        out.int8((i+j).toByte)
        out.int32(i+j+100)
        out.int64(i+j+101)
        j += 1
      }
      o2.int32((out.totalBytesWritten-pos).toInt)
      o2.close()
      i += 1
    }
    out.flush()
  }

  private[this] def writeDeferTo(out: BufferedOutput): Unit = {
    var i = 0
    while(i < count/div) {
      val o2 = out.defer()
      val pos = out.totalBytesWritten
      var j = 0
      while(j < div) {
        out.int8((i+j).toByte)
        out.int32(i+j+100)
        out.int64(i+j+101)
        j += 1
      }
      o2.int32((out.totalBytesWritten-pos).toInt)
      o2.close()
      i += 1
    }
    out.flush()
  }

  private[this] def writeSubTo(out: BufferedOutput): Unit = {
    var i = 0
    while(i < count/div) {
      val o2 = out.sub()
      var j = 0
      while(j < div) {
        o2.int8((i+j).toByte)
        o2.int32(i+j+100)
        o2.int64(i+j+101)
        j += 1
      }
      out.int32(o2.totalBytesWritten.toInt)
      o2.close()
      i += 1
    }
    out.flush()
  }

//  @Benchmark
//  def dataOutputStream_direct_toFixedBAOS(bh: Blackhole): Unit = {
//    val bout = new MyByteArrayOutputStream(byteSize)
//    val out = new DataOutputStream(bout)
//    writeDirectTo(out)
//    assert(bout.getSize == byteSize)
//    bh.consume(bout.getSize)
//    bh.consume(bout.getBuffer)
//  }
//
//  @Benchmark
//  def dataOutputStream_nested_toFixedBAOS(bh: Blackhole): Unit = {
//    val bout = new MyByteArrayOutputStream(byteSize)
//    val out = new DataOutputStream(bout)
//    writeNestedTo(out)
//    assert(bout.getSize == byteSize)
//    bh.consume(bout.getSize)
//    bh.consume(bout.getBuffer)
//  }
//
//  @Benchmark
//  def bufferedOutput_direct_toFixedBAOS(bh: Blackhole): Unit = {
//    val bout = new MyByteArrayOutputStream(byteSize)
//    val out = BufferedOutput(bout)
//    writeDirectTo(out)
//    bh.consume(bout.getSize)
//    bh.consume(bout.getBuffer)
//  }

//  @Benchmark
//  def bufferedOutput_reserve_toFixedBAOS(bh: Blackhole): Unit = {
//    val bout = new MyByteArrayOutputStream(byteSize)
//    val out = BufferedOutput(bout)
//    writeReserveTo(out)
//    bh.consume(bout.getSize)
//    bh.consume(bout.getBuffer)
//  }
//
//  @Benchmark
//  def bufferedOutput_defer_toFixedBAOS(bh: Blackhole): Unit = {
//    val bout = new MyByteArrayOutputStream(byteSize)
//    val out = BufferedOutput(bout)
//    writeDeferTo(out)
//    bh.consume(bout.getSize)
//    bh.consume(bout.getBuffer)
//  }

  @Benchmark
  def bufferedOutput_sub_toFixedBAOS(bh: Blackhole): Unit = {
    val bout = new MyByteArrayOutputStream(byteSize)
    val out = BufferedOutput(bout)
    writeSubTo(out)
    bh.consume(bout.getSize)
    bh.consume(bout.getBuffer)
  }
}
