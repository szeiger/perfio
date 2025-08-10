package perfio

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.*

import java.io.{BufferedInputStream, BufferedOutputStream, ByteArrayInputStream, ByteArrayOutputStream, DataOutputStream}
import java.lang.foreign.MemorySegment
import java.nio.ByteBuffer
import java.util.concurrent.{ForkJoinPool, TimeUnit}
import java.util.zip.{Deflater, GZIPInputStream, GZIPOutputStream}

@BenchmarkMode(Array(Mode.AverageTime))
@Fork(value = 1, jvmArgsAppend = Array("-Xmx12g", "-Xss32M", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC",
  "--enable-native-access=ALL-UNNAMED", "--add-modules", "jdk.incubator.vector"
))
@Threads(1)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 10, time = 1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
class GzipInputBenchmark extends BenchUtil:

  //@Param(Array("numSmall", "chunks"))
  @Param(Array("chunks"))
  var dataSet: String = null

  //@Param(Array("1024", "32768"))
  @Param(Array("32768"))
  var blockSize: Int = 0

  private var testData: Array[Byte] = null
  private var testDataGlobal: MemorySegment = null
  private var gzipTestData: Array[Byte] = null
  private var gzipTestDataGlobal: MemorySegment = null

  final lazy val data = BenchmarkDataSet.forName(dataSet)
  import data.*

  @Setup(Level.Trial)
  def buildTestData(): Unit =
    val out = new ByteArrayOutputStream()
    writeTo(out)
    out.close();
    testData = out.toByteArray
    //testDataGlobal = toGlobal(testData)
    gzipTestData = gzipCompress(testData)
    //gzipTestDataGlobal = toGlobal(gzipTestData)

  @Benchmark
  def noop(bh: Blackhole): Unit =
    val in = BufferedInput.ofArray(testData)
    readFrom(bh, in)
    in.close(true)

  @Benchmark
  def gzipInputStream(bh: Blackhole): Unit =
    val bin = new ByteArrayInputStream(gzipTestData)
    val gin = new GZIPInputStream(bin)
    readFrom(bh, gin)
    gin.close()

  @Benchmark
  def gzipBufferedInputStream(bh: Blackhole): Unit =
    val bin = new ByteArrayInputStream(gzipTestData)
    val gin = new BufferedInputStream(new GZIPInputStream(bin))
    readFrom(bh, gin)
    gin.close()

  @Benchmark
  def sync(bh: Blackhole): Unit =
    val bin = BufferedInput.ofArray(gzipTestData)
    val gin = new GzipBufferedInput(bin)
    readFrom(bh, gin)
    gin.close(true)
