package perfio

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.*
import perfio.internal.StringInternals

import java.io.*
import java.lang.foreign.MemorySegment
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.Files
import java.util.concurrent.TimeUnit

@BenchmarkMode(Array(Mode.AverageTime))
@Fork(value = 1, jvmArgsAppend = Array("-Xmx12g", "-Xss32M", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC", "--enable-native-access=ALL-UNNAMED", "--add-modules", "jdk.incubator.vector"))
@Threads(1)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 10, time = 1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
class LineTokenizerBenchmark extends BenchUtil:
  //@Param(Array("UTF-8", "Latin1", "Latin1-internal"))
  @Param(Array("Latin1"))
  var charset: String = null
  var cs: Charset = null

  private var testData: Array[Byte] = null
  private var diskTestDataMedium, diskTestDataLarge: File = null
  private var testDataOffHeap: MemorySegment = null

  @Setup(Level.Trial)
  def buildTestData(): Unit =
    if(charset.endsWith("-internal"))
      sys.props.put("perfio.disableStringInternals", "false")
      if(StringInternals.internalAccessError != null) throw StringInternals.internalAccessError
      assert(StringInternals.internalAccessEnabled)
      charset = charset.substring(0, charset.length - 9)
    else
      sys.props.put("perfio.disableStringInternals", "true")
      assert(!StringInternals.internalAccessEnabled)
    cs = Charset.forName(charset)

    val testStrings = (for(i <- 0 until 1000) yield "x" * i).toArray
    val repetitions = 1000
    val bout = new ByteArrayOutputStream()
    val wr = new BufferedWriter(new OutputStreamWriter(bout, StandardCharsets.UTF_8))
    for
      i <- 1 to repetitions
      s <- testStrings
    do
      wr.write(s)
      wr.write('\n')
    wr.close()
    testData = bout.toByteArray
    diskTestDataMedium = writeFileIfMissing("medium", testData, 1024*1024)
    diskTestDataLarge = writeFileIfMissing("large", testData, Int.MaxValue.toLong+1)
    testDataOffHeap = toGlobal(testData)

  private def count(in: BufferedReader): Int =
    var count = 0
    while(in.readLine() != null) count += 1
    in.close()
    count

  private def count(in: LineTokenizer): Int =
    var count = 0
    while(in.readLine() != null) count += 1
    in.close()
    count

  @Benchmark
  def array_BufferedReader(bh: Blackhole): Unit =
    bh.consume(count(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(testData), cs))))
  @Benchmark
  def array_HeapScalarLineTokenizer_fromInputStream(bh: Blackhole): Unit =
    bh.consume(count(ScalarLineTokenizer.of(BufferedInput.of(new ByteArrayInputStream(testData)), cs)))
  @Benchmark
  def array_HeapScalarLineTokenizer_fromArray(bh: Blackhole): Unit =
    bh.consume(count(ScalarLineTokenizer.of(BufferedInput.ofArray(testData), cs)))
  @Benchmark
  def array_DirectScalarLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(ScalarLineTokenizer.of(BufferedInput.ofMemorySegment(MemorySegment.ofArray(testData)), cs)))
  @Benchmark
  def offHeap_DirectScalarLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(ScalarLineTokenizer.of(BufferedInput.ofMemorySegment(testDataOffHeap), cs)))
  @Benchmark
  def array_HeapVectorizedLineTokenizer_fromInputStream(bh: Blackhole): Unit =
    bh.consume(count(VectorizedLineTokenizer.of(BufferedInput.of(new ByteArrayInputStream(testData)), cs)))
  @Benchmark
  def array_HeapVectorizedLineTokenizer_fromArray(bh: Blackhole): Unit =
    bh.consume(count(VectorizedLineTokenizer.of(BufferedInput.ofArray(testData), cs)))
  @Benchmark
  def array_DirectVectorizedLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(VectorizedLineTokenizer.of(BufferedInput.ofMemorySegment(MemorySegment.ofArray(testData)), cs)))
  @Benchmark
  def offHeap_DirectVectorizedLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(VectorizedLineTokenizer.of(BufferedInput.ofMemorySegment(testDataOffHeap), cs)))

  @Benchmark
  def mediumFile_BufferedReader(bh: Blackhole): Unit =
    bh.consume(count(new BufferedReader(new InputStreamReader(new FileInputStream(diskTestDataMedium), cs))))
  @Benchmark
  def mediumFile_Lines(bh: Blackhole): Unit =
    bh.consume(Files.lines(diskTestDataMedium.toPath, cs).count())
  @Benchmark
  def mediumFile_HeapScalarLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(ScalarLineTokenizer.of(BufferedInput.of(new FileInputStream(diskTestDataMedium)), cs)))
  @Benchmark
  def mediumFile_DirectScalarLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(ScalarLineTokenizer.of(BufferedInput.ofMappedFile(diskTestDataMedium.toPath), cs)))
  @Benchmark
  def mediumFile_HeapVectorizedLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(VectorizedLineTokenizer.of(BufferedInput.of(new FileInputStream(diskTestDataMedium)), cs)))
  @Benchmark
  def mediumFile_DirectVectorizedLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(VectorizedLineTokenizer.of(BufferedInput.ofMappedFile(diskTestDataMedium.toPath), cs)))

  @Benchmark
  def largeFile_BufferedReader(bh: Blackhole): Unit =
    bh.consume(count(new BufferedReader(new InputStreamReader(new FileInputStream(diskTestDataLarge), cs))))
  @Benchmark
  def largeFile_Lines(bh: Blackhole): Unit =
    bh.consume(Files.lines(diskTestDataLarge.toPath, cs).count())
  @Benchmark
  def largeFile_HeapScalarLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(ScalarLineTokenizer.of(BufferedInput.of(new FileInputStream(diskTestDataLarge)), cs)))
  @Benchmark
  def largeFile_DirectScalarLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(ScalarLineTokenizer.of(BufferedInput.ofMappedFile(diskTestDataLarge.toPath), cs)))
  @Benchmark
  def largeFile_HeapVectorizedLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(VectorizedLineTokenizer.of(BufferedInput.of(new FileInputStream(diskTestDataLarge)), cs)))
  @Benchmark
  def largeFile_DirectVectorizedLineTokenizer(bh: Blackhole): Unit =
    bh.consume(count(VectorizedLineTokenizer.of(BufferedInput.ofMappedFile(diskTestDataLarge.toPath), cs)))
