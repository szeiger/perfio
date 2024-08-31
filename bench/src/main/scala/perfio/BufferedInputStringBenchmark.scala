package perfio

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra._

import java.io._
import java.util.concurrent.TimeUnit

@BenchmarkMode(Array(Mode.AverageTime))
@Fork(value = 1, jvmArgsAppend = Array("-Xmx12g", "-Xss32M", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC", "--enable-native-access=ALL-UNNAMED", "--add-modules", "jdk.incubator.vector"))
@Threads(1)
@Warmup(iterations = 7, time = 1)
@Measurement(iterations = 7, time = 1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
class BufferedInputStringBenchmark {

  private[this] var testData: Array[Byte] = _
  val count = 10000000

  @Setup(Level.Trial)
  def buildTestData(): Unit = {
    val out = new ByteArrayOutputStream()
    val dout = new DataOutputStream(out)
    for(i <- 0 until count) {
      dout.writeUTF("abcdefghijklmnopqrstuvwxyz")
    }
    testData = out.toByteArray
  }

  @Benchmark
  def array_DataInputStream(bh: Blackhole): Unit = {
    val din = new DataInputStream(new ByteArrayInputStream(testData))
    for(i <- 0 until count) {
      bh.consume(din.readUTF())
    }
    din.close()
  }

  @Benchmark
  def array_BufferedInput(bh: Blackhole): Unit = {
    val bin = BufferedInput(new ByteArrayInputStream(testData))
    for(i <- 0 until count) {
      val len = bin.uint16()
      bh.consume(bin.string(len))
    }
    bin.close()
  }

  @Benchmark
  def array_BufferedInput_fromArray(bh: Blackhole): Unit = {
    val bin = BufferedInput.fromArray(testData)
    for(i <- 0 until count) {
      val len = bin.uint16()
      bh.consume(bin.string(len))
    }
    bin.close()
  }
}
