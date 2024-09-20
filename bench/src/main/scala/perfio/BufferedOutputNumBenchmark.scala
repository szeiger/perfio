package perfio

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra._

import java.io._
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

@BenchmarkMode(Array(Mode.AverageTime))
@Fork(value = 1, jvmArgsAppend = Array("-Xmx12g", "-Xss32M", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC", "--enable-native-access=ALL-UNNAMED", "--add-modules", "jdk.incubator.vector"))
@Threads(1)
@Warmup(iterations = 12, time = 1)
@Measurement(iterations = 10, time = 1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
class BufferedOutputNumBenchmark extends BenchUtil {

  val count = 20000000
  val byteSize = count * 13

  private[this] def writeTo(out: DataOutputStream): Unit = {
    var i = 0
    while(i < count) {
      out.writeByte(i)
      out.writeInt(i+100)
      out.writeLong(i+101)
      i += 1
    }
    out.flush()
  }

  private[this] def writeTo(out: BufferedOutput): Unit = {
    var i = 0
    while(i < count) {
      out.int8(i.toByte)
      out.int32(i+100)
      out.int64(i+101)
      i += 1
    }
    out.flush()
  }

//  @Benchmark
//  def array_DataOutputStream_growing(bh: Blackhole): Unit = {
//    val bout = new MyByteArrayOutputStream
//    val out = new DataOutputStream(bout)
//    writeTo(out)
//    bh.consume(bout.getSize)
//    bh.consume(bout.getBuffer)
//  }
//
  @Benchmark
  def array_DataOutputStream_fixed(bh: Blackhole): Unit = {
    val bout = new MyByteArrayOutputStream(count * 13)
    val out = new DataOutputStream(bout)
    writeTo(out)
    bh.consume(bout.getSize)
    bh.consume(bout.getBuffer)
  }

//  @Benchmark
//  def array_FushingBufferedOutput_growing(bh: Blackhole): Unit = {
//    val bout = new MyByteArrayOutputStream
//    val out = BufferedOutput(bout)
//    writeTo(out)
//    bh.consume(bout.getSize)
//    bh.consume(bout.getBuffer)
//  }
//
//  @Benchmark
//  def array_FushingBufferedOutput_fixed(bh: Blackhole): Unit = {
//    val bout = new MyByteArrayOutputStream(count * 13)
//    val out = BufferedOutput(bout)
//    writeTo(out)
//    bh.consume(bout.getSize)
//    bh.consume(bout.getBuffer)
//  }
//
//  @Benchmark
//  def array_GrowingBufferedOutput_growing(bh: Blackhole): Unit = {
//    val out = BufferedOutput.growing()
//    writeTo(out)
//    bh.consume(out.getSize)
//    bh.consume(out.getBuffer)
//  }

  @Benchmark
  def array_GrowingBufferedOutput_fixed(bh: Blackhole): Unit = {
    val out = BufferedOutput.growing(initialBufferSize = count*13)
    writeTo(out)
    bh.consume(out.getSize)
    bh.consume(out.getBuffer)
  }

//  @Benchmark
//  def array_ByteBuffer(bh: Blackhole): Unit = {
//    val out = ByteBuffer.allocate(count*13)
//    var i = 0
//    while(i < count) {
//      out.put(i.toByte)
//      out.putInt(i+100)
//      out.putLong(i+101)
//      i += 1
//    }
//    bh.consume(out)
//  }

}

class MyByteArrayOutputStream(capacity: Int = 32768) extends ByteArrayOutputStream(capacity) {
  def getBuffer = buf
  def getSize = count
}
