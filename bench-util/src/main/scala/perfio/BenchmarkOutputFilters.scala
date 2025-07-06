package perfio

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.*

import java.nio.file.Paths
import java.util.concurrent.{CompletableFuture, TimeUnit}

// These classes are used by FilteringBufferedOutputBenchmark. They are defined in this
// project because they trigger a bytecode verification bug when JMH tries to process them.
object BenchmarkOutputFilters:

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
      releaseBlock(b)

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
        releaseBlock(p)

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
        releaseBlock(p)

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
        releaseBlock(p)

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
        releaseBlock(p)

  class AsyncXorBlockBufferedOutput(parent: BufferedOutput, sequential: Boolean, depth: Int) extends AsyncFilteringBufferedOutput[Nothing](parent, sequential, depth, false, 0, 0, false, null):
    def filterAsync(t: FilterTask[Nothing]): Unit =
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
