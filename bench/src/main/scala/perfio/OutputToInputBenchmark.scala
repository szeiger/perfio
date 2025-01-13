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

  @Param(Array("num"))
  var dataSet: String = null
  final lazy val data = BenchmarkDataSet.forName(dataSet)
  import data._

//  @Benchmark
//  def jdk_ByteArrayOutputStream_growing(bh: Blackhole): Unit =
//    val bout = new MyByteArrayOutputStream
//    writeTo(bout)
//    val in = new ByteArrayInputStream(bout.getBuffer, 0, bout.getSize)
//    readFrom(bh, in)
//
//  @Benchmark
//  def jdk_ByteArrayOutputStream_preallocated(bh: Blackhole): Unit =
//    val bout = new MyByteArrayOutputStream(byteSize)
//    writeTo(bout)
//    val in = new ByteArrayInputStream(bout.getBuffer, 0, bout.getSize)
//    readFrom(bh, in)
//
//  @Benchmark
//  def jdk_PipeOutputStream(bh: Blackhole): Unit =
//    // This works best. Adding a BufferedOutputStream or BufferedInputStream only makes it slower.
//    val pout = new PipedOutputStream()
//    val pin = new PipedInputStream(pout)
//    val t1 = new Thread(() => writeTo(pout))
//    val t2 = new Thread(() => readFrom(bh, pin))
//    t1.start()
//    t2.start()
//    t1.join()
//    t2.join()

  @Benchmark
  def perfIO_ArrayBufferedOutput_growing(bh: Blackhole): Unit =
    val out = BufferedOutput.growing()
    writeTo(out)
    readFrom(bh, out.toBufferedInput)

  @Benchmark
  def perfIO_ArrayBufferedOutput_preallocated(bh: Blackhole): Unit =
    val out = BufferedOutput.growing(byteSize)
    writeTo(out)
    readFrom(bh, out.toBufferedInput)

  @Benchmark
  def perfIO_BlockBufferedOutput(bh: Blackhole): Unit =
    val out = BufferedOutput.ofBlocks()
    writeTo(out)
    readFrom(bh, out.toBufferedInput)

  @Benchmark
  def perfIO_PipeBufferedOutput(bh: Blackhole): Unit =
    val out = BufferedOutput.pipe()
    val in = out.toBufferedInput
    val t1 = new Thread(() => writeTo(out))
    val t2 = new Thread(() => readFrom(bh, in))
    t1.start()
    t2.start()
    t1.join()
    t2.join()
