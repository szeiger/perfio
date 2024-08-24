package perfio

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra._

import java.io._
import java.lang.foreign.{Arena, MemorySegment, ValueLayout}
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
class LineTokenizerBenchmark {
  @Param(Array("UTF-8", "ISO-8859-1"))
  var charset: String = _
  var cs: Charset = _

  private[this] var testData: Array[Byte] = _
  private[this] val diskTestDataSmall = new File("/tmp/testdata-small")
  private[this] val diskTestDataLarge = new File("/tmp/testdata-large")
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
    writeFile(diskTestDataSmall, 1024*1024)
    writeFile(diskTestDataLarge, Int.MaxValue.toLong+1)
    testDataOffHeap = Arena.global().allocate(testData.length, 8)
    MemorySegment.copy(testData, 0, testDataOffHeap, ValueLayout.JAVA_BYTE, 0, testData.length)
  }

  private[this] def writeFile(f: File, minLength: Long): Unit = {
    if(!f.exists()) {
      val out = new FileOutputStream(f)
      var written = 0L
      while(written < minLength) {
        out.write(testData)
        written += testData.length
      }
      out.close()
    }
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
  def array_ScalarForeignLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(ScalarForeignLineTokenizer.fromMemorySegment(MemorySegment.ofArray(testData), cs)))
  @Benchmark
  def array_VectorizedLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(VectorizedLineTokenizer(new ByteArrayInputStream(testData), cs)))
  @Benchmark
  def array_VectorizedLineTokenizer_fromArray(bh: Blackhole): Unit =
    bh.consume(count(VectorizedLineTokenizer.fromArray(testData, charset = cs)))
  @Benchmark
  def array_VectorizedForeignLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(VectorizedForeignLineTokenizer.fromMemorySegment(MemorySegment.ofArray(testData), cs)))

  @Benchmark
  def offHeap_ScalarForeignLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(ScalarForeignLineTokenizer.fromMemorySegment(testDataOffHeap, cs)))
    @Benchmark
  def offHeap_VectorizedForeignLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(VectorizedForeignLineTokenizer.fromMemorySegment(testDataOffHeap, cs)))

  @Benchmark
  def smallFile_BufferedReader(bh: Blackhole): Unit =
    bh.consume(count(new BufferedReader(new InputStreamReader(new FileInputStream(diskTestDataSmall), cs))))
  @Benchmark
  def smallFile_Lines(bh: Blackhole): Unit =
    bh.consume(Files.lines(diskTestDataSmall.toPath, cs).count())
  @Benchmark
  def smallFile_ScalarLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(ScalarLineTokenizer(new FileInputStream(diskTestDataSmall), cs)))
  @Benchmark
  def smallFile_ScalarForeignLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(ScalarForeignLineTokenizer.fromMappedFile(diskTestDataSmall.toPath, cs)))
  @Benchmark
  def smallFileVectorizedLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(VectorizedLineTokenizer(new FileInputStream(diskTestDataSmall), cs)))
  def smallFile_VectorizedForeignLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(VectorizedForeignLineTokenizer.fromMappedFile(diskTestDataSmall.toPath, cs)))

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
  def largeFile_ScalarForeignLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(ScalarForeignLineTokenizer.fromMappedFile(diskTestDataLarge.toPath, cs)))
  @Benchmark
  def largeFile_VectorizedLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(VectorizedLineTokenizer(new FileInputStream(diskTestDataLarge), cs)))
  @Benchmark
  def largeFile_VectorizedForeignLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(VectorizedForeignLineTokenizer.fromMappedFile(diskTestDataLarge.toPath, cs)))
}
