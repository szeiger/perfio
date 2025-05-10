package perfio

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.*

import java.nio.file.Paths
import java.util.concurrent.{CompletableFuture, TimeUnit}

@BenchmarkMode(Array(Mode.AverageTime))
@Fork(value = 1, jvmArgsAppend = Array("-Xmx12g", "-Xss32M", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC",
  "--enable-native-access=ALL-UNNAMED", "--add-modules", "jdk.incubator.vector"
))
@Threads(1)
@Warmup(iterations = 15, time = 1)
@Measurement(iterations = 10, time = 1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
class FilteringBufferedOutputBenchmark extends BenchUtil:

  @Param(Array("num", "chunks"))
  var dataSet: String = null
  final lazy val data = BenchmarkDataSet.forName(dataSet)
  import data.*

  //@Param(Array("array", "file"))
  @Param(Array("file"))
  var output: String = null
  var fileOut = false

  @Setup
  def setup: Unit =
    fileOut = output == "file"

  private def run(bh: Blackhole)(f: BufferedOutput => BufferedOutput) =
    if(fileOut) runFile(bh)(f) else runArray(bh)(f)

  private def runArray(bh: Blackhole)(f: BufferedOutput => BufferedOutput) =
    val out = BufferedOutput.growing(byteSize)
    writeTo(f(out))
    bh.consume(out.buffer)
    bh.consume(out.length)

  private def runFile(bh: Blackhole)(f: BufferedOutput => BufferedOutput) =
    val out = BufferedOutput.ofFile(Paths.get("/dev/null"))
    writeTo(f(out))
    out.close()

//  @Benchmark
//  def syncInPlace(bh: Blackhole): Unit = runArray(bh)(new XorBlockBufferedOutput(_))
//
//  @Benchmark
//  def sync(bh: Blackhole): Unit = runArray(bh)(new XorBlockSwappingBufferedOutput(_))
//
//  @Benchmark
//  def partialFlushing(bh: Blackhole): Unit = runArray(bh)(new XorFlushingBufferedOutput(_, true))
//
//  @Benchmark
//  def flushing(bh: Blackhole): Unit = runArray(bh)(new XorFlushingBufferedOutput(_, false))
//
//  @Benchmark
//  def simpleParallelInPlace(bh: Blackhole): Unit = runArray(bh)(new SimpleAsyncXorBlockBufferedOutput(_))
//
//  @Benchmark
//  def simpleAsyncInPlace(bh: Blackhole): Unit = runArray(bh)(new SimpleAsyncSequentialXorBlockBufferedOutput(_))
//
//  @Benchmark
//  def simpleParallel(bh: Blackhole): Unit = runArray(bh)(new SimpleAsyncSwappingXorBlockBufferedOutput(_))
//
//  @Benchmark
//  def simpleAsync(bh: Blackhole): Unit = runArray(bh)(new SimpleAsyncSequentialSwappingXorBlockBufferedOutput(_))

  @Benchmark
  def parallel(bh: Blackhole): Unit = runArray(bh)(new AsyncXorBlockBufferedOutput(_, false, -1))

  @Benchmark
  def async(bh: Blackhole): Unit = runArray(bh)(new AsyncXorBlockBufferedOutput(_, true, 0))

  class XorFlushingBufferedOutput(parent: BufferedOutput, partialFlush: Boolean) extends FilteringBufferedOutput(parent, partialFlush):
    def filterBlock(b: BufferedOutput): Unit =
      val blen = b.pos - b.start
      val p = parent.fwd(blen)
      var i = 0
      while i < blen do
        parent.buf(p + i) = (b.buf(b.start + i) ^ 85.toByte).toByte
        i += 1
      if(state == BufferedOutput.STATE_CLOSED) releaseBlock(b)

  class XorBlockBufferedOutput(parent: BufferedOutput) extends FilteringBufferedOutput(parent, false):
    def filterBlock(b: BufferedOutput): Unit =
      var i = b.start
      while i < b.pos do
        b.buf(i) = (b.buf(i) ^ 85.toByte).toByte
        i += 1
      appendBlockToParent(b)

  class XorBlockSwappingBufferedOutput(parent: BufferedOutput) extends FilteringBufferedOutput(parent, false):
    def filterBlock(b: BufferedOutput): Unit =
      val out = allocBlock()
      val blen = b.pos - b.start
      var i = 0
      while i < blen do
        out.buf(i) = (b.buf(i + b.start) ^ 85.toByte).toByte
        i += 1
      out.start = 0
      out.pos = blen
      appendBlockToParent(out)
      returnToCache(b)

  class SimpleAsyncXorBlockBufferedOutput(parent: BufferedOutput) extends FilteringBufferedOutput(parent, false):
    private var pending: BufferedOutput = null

    private def enqueue(b: BufferedOutput): Unit =
      if(pending == null)
        pending = b
        b.next = b
        b.prev = b
      else b.insertBefore(pending)

    private def dequeueFirst(): BufferedOutput =
      val p = pending
      if(p.next eq p) pending = null
      else
        pending = p.next
        p.unlinkOnly()
      p

    def filterBlock(b: BufferedOutput): Unit =
      while pending != null && pending.filterState.asInstanceOf[CompletableFuture[Unit]].isDone do
        val p = dequeueFirst()
        appendBlockToParent(p)
      b.filterState = CompletableFuture.runAsync: () =>
        var i = b.start
        while i < b.pos do
          b.buf(i) = (b.buf(i) ^ 85.toByte).toByte
          i += 1
      enqueue(b)


    override protected def flushPending(): Unit =
      while pending != null do
        val p = dequeueFirst()
        p.filterState.asInstanceOf[CompletableFuture[Unit]].get
        appendBlockToParent(p)

  class SimpleAsyncSequentialXorBlockBufferedOutput(parent: BufferedOutput) extends FilteringBufferedOutput(parent, false):
    private var pending: BufferedOutput = null
    private var previousFuture: CompletableFuture[?] = null

    private def enqueue(b: BufferedOutput): Unit =
      if(pending == null)
        pending = b
        b.next = b
        b.prev = b
      else b.insertBefore(pending)

    private def dequeueFirst(): BufferedOutput =
      val p = pending
      if(p.next eq p) pending = null
      else
        pending = p.next
        p.unlinkOnly()
      p

    def filterBlock(b: BufferedOutput): Unit =
      while pending != null && pending.filterState.asInstanceOf[CompletableFuture[?]].isDone do
        val p = dequeueFirst()
        appendBlockToParent(p)
      val r: Runnable = () =>
        var i = b.start
        while i < b.pos do
          b.buf(i) = (b.buf(i) ^ 85.toByte).toByte
          i += 1
      if(previousFuture == null)
        previousFuture = CompletableFuture.runAsync(r)
        b.filterState = previousFuture
      else
        previousFuture = previousFuture.thenRun(r)
        b.filterState = previousFuture
      enqueue(b)

    override protected def flushPending(): Unit =
      while pending != null do
        val p = dequeueFirst()
        p.filterState.asInstanceOf[CompletableFuture[Unit]].get
        appendBlockToParent(p)

  class SimpleAsyncSwappingXorBlockBufferedOutput(parent: BufferedOutput) extends FilteringBufferedOutput(parent, false):
    private var pending: BufferedOutput = null

    private def enqueue(b: BufferedOutput): Unit =
      if(pending == null)
        pending = b
        b.next = b
        b.prev = b
      else b.insertBefore(pending)

    private def dequeueFirst(): BufferedOutput =
      val p = pending
      if(p.next eq p) pending = null
      else
        pending = p.next
        p.unlinkOnly()
      p

    def filterBlock(b: BufferedOutput): Unit =
      while pending != null && pending.filterState.asInstanceOf[CompletableFuture[?]].isDone do
        val p = dequeueFirst()
        appendBlockToParent(p.rootBlock)
        returnToCache(p)

      b.rootBlock = allocBlock()
      val r: Runnable = () =>
        val blen = b.pos - b.start
        val out = b.rootBlock
        var i = 0
        while i < blen do
          out.buf(i) = (b.buf(i + b.start) ^ 85.toByte).toByte
          i += 1
        out.start = 0
        out.pos = blen
      b.filterState = CompletableFuture.runAsync(r)
      enqueue(b)

    override protected def flushPending(): Unit =
      while pending != null do
        val p = dequeueFirst()
        p.filterState.asInstanceOf[CompletableFuture[Unit]].get
        appendBlockToParent(p.rootBlock)
        returnToCache(p)

  class SimpleAsyncSequentialSwappingXorBlockBufferedOutput(parent: BufferedOutput) extends FilteringBufferedOutput(parent, false):
    private var pending: BufferedOutput = null
    private var previousFuture: CompletableFuture[?] = null

    private def enqueue(b: BufferedOutput): Unit =
      if(pending == null)
        pending = b
        b.next = b
        b.prev = b
      else b.insertBefore(pending)

    private def dequeueFirst(): BufferedOutput =
      val p = pending
      if(p.next eq p) pending = null
      else
        pending = p.next
        p.unlinkOnly()
      p

    def filterBlock(b: BufferedOutput): Unit =
      while pending != null && pending.filterState.asInstanceOf[CompletableFuture[?]].isDone do
        val p = dequeueFirst()
        appendBlockToParent(p.rootBlock)
        returnToCache(p)

      b.rootBlock = allocBlock()
      val r: Runnable = () =>
        val blen = b.pos - b.start
        val out = b.rootBlock
        var i = 0
        while i < blen do
          out.buf(i) = (b.buf(i + b.start) ^ 85.toByte).toByte
          i += 1
        out.start = 0
        out.pos = blen
      if(previousFuture == null)
        previousFuture = CompletableFuture.runAsync(r)
        b.filterState = previousFuture
      else
        previousFuture = previousFuture.thenRun(r)
        b.filterState = previousFuture
      enqueue(b)

    override protected def flushPending(): Unit =
      while pending != null do
        val p = dequeueFirst()
        p.filterState.asInstanceOf[CompletableFuture[Unit]].get
        appendBlockToParent(p.rootBlock)
        returnToCache(p)

  class AsyncXorBlockBufferedOutput(parent: BufferedOutput, sequential: Boolean, depth: Int) extends AsyncFilteringBufferedOutput(parent, sequential, depth, false, 0):
    def filterAsync(t: AsyncFilteringBufferedOutput#Task): Unit =
      val tolen = t.to.buf.length
      val prlen = t.length min tolen
      var i = 0
      while i < prlen do
        t.to.buf(i) = (t.buf(i + t.start) ^ 85.toByte).toByte
        i += 1
      t.to.start = 0
      t.to.pos = prlen
      t.start += prlen
      if(t.start == t.end) t.consume()
      //Thread.sleep(0, 100000)
