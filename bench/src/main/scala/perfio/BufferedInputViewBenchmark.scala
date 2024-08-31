package perfio

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra._

import java.io._
import java.util.concurrent.TimeUnit

import com.google.common.io.ByteStreams

@BenchmarkMode(Array(Mode.AverageTime))
@Fork(value = 1, jvmArgsAppend = Array("-Xmx12g", "-Xss32M", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC", "--enable-native-access=ALL-UNNAMED", "--add-modules", "jdk.incubator.vector"))
@Threads(1)
@Warmup(iterations = 7, time = 1)
@Measurement(iterations = 7, time = 1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
class BufferedInputViewBenchmark {

  private[this] var testData: Array[Byte] = _
  val count = 20000000

  @Setup(Level.Trial)
  def buildTestData(): Unit = {
    val out = new ByteArrayOutputStream()
    val dout = new DataOutputStream(out)
    for(i <- 0 until count) {
      dout.writeByte(i)
      dout.writeInt(i+100)
      dout.writeLong(i+101)
    }
    testData = out.toByteArray
  }

  private[this] def runDirect(bh: Blackhole, bin: BufferedInput): Unit = {
    for(i <- 0 until count/10) {
      for(i <- 0 until 10) {
        bh.consume(bin.int8())
        bh.consume(bin.int32())
        bh.consume(bin.int64())
      }
    }
    bin.close()
  }

  private[this] def runCounting(bh: Blackhole, bin: BufferedInput): Unit = {
    for(i <- 0 until count/10) {
      val end = bin.totalBytesRead + 130
      while(bin.totalBytesRead < end) {
        bh.consume(bin.int8())
        bh.consume(bin.int32())
        bh.consume(bin.int64())
      }
    }
    bin.close()
  }

  private[this] def runView(bh: Blackhole, bin: BufferedInput): Unit = {
    for(i <- 0 until count/10) {
      val v = bin.delimitedView(130)
      while(v.hasMore) {
        bh.consume(v.int8())
        bh.consume(v.int32())
        bh.consume(v.int64())
      }
      v.close()
    }
    bin.close()
  }

//  @Benchmark
//  def array_LimitedInputStream(bh: Blackhole): Unit = {
//    val bin = new ByteArrayInputStream(testData)
//    val din = new DataInputStream(bin)
//    for(i <- 0 until count / 10) {
//      val v = new DataInputStream(ByteStreams.limit(bin, 130))
//      try {
//        while(true) {
//          bh.consume(v.readByte())
//          bh.consume(v.readInt())
//          bh.consume(v.readLong())
//        }
//      } catch { case _: EOFException => }
//    }
//    din.close()
//  }

  @Benchmark
  def array_fromInputStream_direct(bh: Blackhole): Unit = runDirect(bh, BufferedInput(new ByteArrayInputStream(testData)))

  @Benchmark
  def array_fromInputStream_counting(bh: Blackhole): Unit = runCounting(bh, BufferedInput(new ByteArrayInputStream(testData)))

  @Benchmark
  def array_fromInputStream_view(bh: Blackhole): Unit = runView(bh, BufferedInput(new ByteArrayInputStream(testData)))

  @Benchmark
  def array_fromArray_direct(bh: Blackhole): Unit = runDirect(bh, BufferedInput.fromArray(testData))

  @Benchmark
  def array_fromArray_counting(bh: Blackhole): Unit = runCounting(bh, BufferedInput.fromArray(testData))

  @Benchmark
  def array_fromArray_view(bh: Blackhole): Unit = runView(bh, BufferedInput.fromArray(testData))
}
