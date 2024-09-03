package perfio

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra._

import java.io._
import java.lang.foreign.MemorySegment
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.Files
import java.util.concurrent.TimeUnit

@BenchmarkMode(Array(Mode.AverageTime))
@Fork(value = 1, jvmArgsAppend = Array("-Xmx12g", "-Xss32M", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC", "--enable-native-access=ALL-UNNAMED", "--add-modules", "jdk.incubator.vector"))
@Threads(1)
@Warmup(iterations = 7, time = 1)
@Measurement(iterations = 7, time = 1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
class LineTokenizerBenchmark extends BenchUtil {
  @Param(Array("UTF-8", "ISO-8859-1"))
  var charset: String = _
  var cs: Charset = _

  private[this] var testData: Array[Byte] = _
  private[this] var diskTestDataMedium, diskTestDataLarge: File = _
  private[this] var testDataOffHeap: MemorySegment = _

  @Setup(Level.Trial)
  def buildTestData(): Unit = {
    cs = Charset.forName(charset)
    val testStrings = (for(i <- 0 until 1000) yield "x" * i).toArray
    val repetitions = 1000
    val bout = new ByteArrayOutputStream()
    val wr = new BufferedWriter(new OutputStreamWriter(bout, StandardCharsets.UTF_8))
    for {
      i <- 1 to repetitions
      s <- testStrings
    } {
      wr.write(s)
      wr.write('\n')
    }
    wr.close()
    testData = bout.toByteArray
    diskTestDataMedium = writeFileIfMissing("medium", testData, 1024*1024)
    diskTestDataLarge = writeFileIfMissing("large", testData, Int.MaxValue.toLong+1)
    testDataOffHeap = toGlobal(testData)
  }

  private[this] def count(in: BufferedReader): Int = {
    var count = 0
    while(in.readLine() != null) count += 1
    in.close()
    count
  }

  private[this] def count(in: LineTokenizer): Int = {
    var count = 0
    while(in.readLine() != null) count += 1
    in.close()
    count
  }

  @Benchmark
  def array_BufferedReader(bh: Blackhole): Unit =
    bh.consume(count(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(testData), cs))))
  @Benchmark
  def array_ScalarLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(ScalarLineTokenizer(new ByteArrayInputStream(testData), cs)))
  @Benchmark
  def array_ScalarLineTokenizer_fromArray(bh: Blackhole): Unit =
    bh.consume(count(ScalarLineTokenizer.fromArray(testData, charset = cs)))
  @Benchmark
  def array_ForeignScalarLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(ForeignScalarLineTokenizer.fromMemorySegment(MemorySegment.ofArray(testData), cs)))
  @Benchmark
  def array_VectorizedLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(VectorizedLineTokenizer(new ByteArrayInputStream(testData), cs)))
  @Benchmark
  def array_VectorizedLineTokenizer_fromArray(bh: Blackhole): Unit =
    bh.consume(count(VectorizedLineTokenizer.fromArray(testData, charset = cs)))
  @Benchmark
  def array_ForeignVectorizedLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(ForeignVectorizedLineTokenizer.fromMemorySegment(MemorySegment.ofArray(testData), cs)))

  @Benchmark
  def offHeap_ForeignScalarLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(ForeignScalarLineTokenizer.fromMemorySegment(testDataOffHeap, cs)))
    @Benchmark
  def offHeap_ForeignVectorizedLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(ForeignVectorizedLineTokenizer.fromMemorySegment(testDataOffHeap, cs)))

  @Benchmark
  def mediumFile_BufferedReader(bh: Blackhole): Unit =
    bh.consume(count(new BufferedReader(new InputStreamReader(new FileInputStream(diskTestDataMedium), cs))))
  @Benchmark
  def mediumFile_Lines(bh: Blackhole): Unit =
    bh.consume(Files.lines(diskTestDataMedium.toPath, cs).count())
  @Benchmark
  def mediumFile_ScalarLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(ScalarLineTokenizer(new FileInputStream(diskTestDataMedium), cs)))
  @Benchmark
  def mediumFile_ForeignScalarLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(ForeignScalarLineTokenizer.fromMappedFile(diskTestDataMedium.toPath, cs)))
  @Benchmark
  def mediumFile_VectorizedLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(VectorizedLineTokenizer(new FileInputStream(diskTestDataMedium), cs)))
  def mediumFile_ForeignVectorizedLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(ForeignVectorizedLineTokenizer.fromMappedFile(diskTestDataMedium.toPath, cs)))

  @Benchmark
  def largeFile_BufferedReader(bh: Blackhole): Unit =
    bh.consume(count(new BufferedReader(new InputStreamReader(new FileInputStream(diskTestDataLarge), cs))))
  @Benchmark
  def largeFile_Lines(bh: Blackhole): Unit =
    bh.consume(Files.lines(diskTestDataLarge.toPath, cs).count())
  @Benchmark
  def largeFile_ScalarLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(ScalarLineTokenizer(new FileInputStream(diskTestDataLarge), cs)))
  @Benchmark
  def largeFile_ForeignScalarLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(ForeignScalarLineTokenizer.fromMappedFile(diskTestDataLarge.toPath, cs)))
  @Benchmark
  def largeFile_VectorizedLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(VectorizedLineTokenizer(new FileInputStream(diskTestDataLarge), cs)))
  @Benchmark
  def largeFile_ForeignVectorizedLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(ForeignVectorizedLineTokenizer.fromMappedFile(diskTestDataLarge.toPath, cs)))
}
