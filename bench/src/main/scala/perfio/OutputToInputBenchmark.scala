package perfio

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.*

import java.io.*
import java.nio.ByteBuffer
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

@BenchmarkMode(Array(Mode.AverageTime))
@Fork(value = 1, jvmArgsAppend = Array("-Xmx12g", "-Xss32M", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC", "--enable-native-access=ALL-UNNAMED", "--add-modules", "jdk.incubator.vector"))
@Threads(1)
@Warmup(iterations = 15, time = 1)
@Measurement(iterations = 15, time = 1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
class OutputToInputBenchmark extends BenchUtil:

  val count = 20000000
  val byteSize = count * 13

  private def writeTo(out: DataOutputStream): Unit =
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

  private def readFrom(bh: Blackhole, bin: BufferedInput): Unit = {
    var i = 0
    while(i < count) {
      bh.consume(bin.int8())
      bh.consume(bin.int32())
      bh.consume(bin.int64())
      i += 1
    }
    bin.close()
  }

  def readFrom(bh: Blackhole, din: DataInputStream): Unit = {
    var i = 0
    while(i < count) {
      bh.consume(din.readByte())
      bh.consume(din.readInt())
      bh.consume(din.readLong())
      i += 1
    }
    din.close()
  }

  @Benchmark
  def num_DataOutputStream_growing(bh: Blackhole): Unit =
    val bout = new MyByteArrayOutputStream
    val out = new DataOutputStream(bout)
    writeTo(out)
    val in = new DataInputStream(new ByteArrayInputStream(bout.getBuffer, 0, bout.getSize))
    readFrom(bh, in)

  @Benchmark
  def num_DataOutputStream_preallocated(bh: Blackhole): Unit =
    val bout = new MyByteArrayOutputStream(count * 13)
    val out = new DataOutputStream(bout)
    writeTo(out)
    val in = new DataInputStream(new ByteArrayInputStream(bout.getBuffer, 0, bout.getSize))
    readFrom(bh, in)

  @Benchmark
  def num_FullyBufferedOutput_growing(bh: Blackhole): Unit =
    val out = BufferedOutput.growing()
    writeTo(out)
    readFrom(bh, out.toBufferedInput)

  @Benchmark
  def num_FullyBufferedOutput_preallocated(bh: Blackhole): Unit =
    val out = BufferedOutput.growing(count * 13)
    writeTo(out)
    readFrom(bh, out.toBufferedInput)

  @Benchmark
  def num_BlockBufferedOutput(bh: Blackhole): Unit =
    val out = BufferedOutput.ofBlocks()
    writeTo(out)
    readFrom(bh, out.toBufferedInput)
