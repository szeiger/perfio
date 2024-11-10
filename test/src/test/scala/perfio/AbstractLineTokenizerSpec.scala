package perfio

import hedgehog.*
import hedgehog.runner.*

import java.io.{ByteArrayInputStream, IOException}
import java.lang.foreign.MemorySegment
import java.nio.charset.{Charset, StandardCharsets}
import java.util.Arrays
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

abstract class AbstractLineTokenizerSpec[T] extends Properties with TestUtil:

  def base: List[(String, Int, Int, T, Int)]

  def createTok(in: BufferedInput, cs: Charset): LineTokenizer

  def tests: List[Test] =
    for
      cs <- List(StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1)
      (n, min, max, params, count) <- base
      t <- List(
        property(s"${n}.${cs}", createProp(min, max, cs, false, params)).config(_.copy(testLimit = count)),
        property(s"${n}.${cs}.split", createProp(min, max, cs, true, params)).config(_.copy(testLimit = count))
      )
    yield t

  def createProp(min: Int, max: Int, cs: Charset, split: Boolean, params: T): Property

  def run(s: String, in: BufferedInput, cs: Charset, split: Boolean): Result =
    try
      val (buf, next) = if(split) buildSplit(in, cs) else buildNormal(in, cs)
      val exp = s.lines().toList.asScala.map(_.length)
      Result.all(List(
        (buf: mutable.Buffer[Int]) ==== exp,
        next ==== null
      ))
    finally in.close()

  def buildSplit(in: BufferedInput, cs: Charset): (mutable.ArrayBuffer[Int], String) =
    var i = 0
    var t = createTok(in, cs)
    val buf = mutable.ArrayBuffer.empty[Int]
    while t.readLine() match
      case null => false
      case s =>
        buf += s.length
        i += 1
        if(i % 2 == 0)
          t.end()
          assertException[IOException](t.readLine())
          t = createTok(in, cs)
        true
    do ()
    val next = t.readLine()
    t.close()
    assertException[IOException](t.readLine())
    (buf, next)

  def buildNormal(in: BufferedInput, cs: Charset): (mutable.ArrayBuffer[Int], String) =
    val t = createTok(in, cs)
    val buf = mutable.ArrayBuffer.empty[Int]
    while t.readLine() match
      case null => false
      case s => buf += s.length; true
    do ()
    val next = t.readLine()
    t.close()
    assertException[IOException](t.readLine())
    (buf, next)


abstract class SimpleLineTokenizerSpec[T] extends AbstractLineTokenizerSpec[T]:
  def createBI(s: String, cs: Charset, params: T): BufferedInput

  def createProp(min: Int, max: Int, cs: Charset, split: Boolean, params: T): Property =
    for
      s <- Gen.string(Gen.element1('a', '\n'), Range.linear(min, max)).forAll
    yield run(s, createBI(s, cs, params), cs, split)


object HeapScalarLineTokenizerSpec extends SimpleLineTokenizerSpec[Int]:
  val base: List[(String, Int, Int, Int, Int)] = List(
    ("small.aligned", 64, 64, 4096, 2000),
    ("large.aligned", 128, 128, 64, 2000),
    ("small.unligned", 0, 63, 4096, 2000),
    ("large.unaligned", 65, 127, 64, 2000),
    ("mixed", 0, 513, 128, 10000),
  )

  def createBI(s: String, cs: Charset, ib: Int): BufferedInput =
    BufferedInput.of(new ByteArrayInputStream(s.getBytes(cs)), ib)
  def createTok(in: BufferedInput, cs: Charset): LineTokenizer = ScalarLineTokenizer.of(in, cs, '\n'.toByte, '\r'.toByte)


object HeapVectorizedLineTokenizerSpec extends SimpleLineTokenizerSpec[(Int, Int)]:
  val base: List[(String, Int, Int, (Int, Int), Int)] = List(
    ("small.aligned", 64, 64, (4096, Int.MaxValue), 2000),
    ("large.aligned", 128, 128, (64, Int.MaxValue), 2000),
    ("small.unaligned", 0, 63, (4096, Int.MaxValue), 2000),
    ("large.unaligned", 65, 127, (64, Int.MaxValue), 2000),
    ("small.aligned.limited1", 64, 64, (4096, 1), 2000),
    ("large.aligned.limited1", 128, 128, (64, 1), 2000),
    ("small.unaligned.limited1", 0, 63, (4096, 1), 2000),
    ("large.unaligned.limited1", 65, 127, (64, 1), 2000),
    ("small.aligned.limited16", 64, 64, (4096, 16), 2000),
    ("large.aligned.limited16", 128, 128, (64, 16), 2000),
    ("small.unaligned.limited16", 0, 63, (4096, 16), 2000),
    ("large.unaligned.limited16", 65, 127, (64, 16), 2000),
    ("mixed", 0, 513, (128, Int.MaxValue), 10000),
  )

  def createBI(s: String, cs: Charset, params: (Int, Int)): BufferedInput =
    val (ib, maxRead) = params
    BufferedInput.of(new LimitedInputStream(new ByteArrayInputStream(s.getBytes(cs)), maxRead), ib)
  def createTok(in: BufferedInput, cs: Charset): LineTokenizer = VectorizedLineTokenizer.of(in, cs, '\n'.toByte, '\r'.toByte)


abstract class FromArraySpec extends AbstractLineTokenizerSpec[(Int, Int)]:
  val base: List[(String, Int, Int, (Int, Int), Int)] = List(
    ("small.aligned", 64, 64, (0, 0), 2000),
    ("large.aligned", 128, 128, (0, 0), 2000),
    ("small.unligned", 0, 63, (0, 0), 2000),
    ("large.unaligned", 65, 127, (0, 0), 2000),
    ("padRight", 0, 65, (0, 33), 2000),
    ("padLeft", 0, 65, (33, 0), 2000),
    ("padBoth", 0, 65, (33, 33), 2000),
    ("mixed", 0, 513, (0, 0), 2000),
  )

  def createProp(min: Int, max: Int, cs: Charset, split: Boolean, params: (Int, Int)): Property =
    val (padLeft, padRight) = params
    for
      s <- Gen.string(Gen.element1('a', '\n'), Range.linear(min, max)).forAll
      pl <- Gen.int(Range.linear(if(padLeft == 0) 0 else 1, padLeft)).forAll
      pr <- Gen.int(Range.linear(if(padRight == 0) 0 else 1, padRight)).forAll
    yield run(s, createBI(s, cs, (pl, pr)), cs, split)

  def createBI(s: String, cs: Charset, params: (Int, Int)): BufferedInput =
    val (padLeft, padRight) = params
    val bytes = s.getBytes(cs)
    val a = new Array[Byte](bytes.length+padLeft+padRight)
    Arrays.fill(a, '\n'.toByte)
    System.arraycopy(bytes, 0, a, padLeft, bytes.length)
    createBI(a, padLeft, bytes.length+padLeft)

  def createBI(a: Array[Byte], off: Int, len: Int): BufferedInput


abstract class ForeignSpec extends FromArraySpec:
  override val base: List[(String, Int, Int, (Int, Int), Int)] = List(
    ("small.aligned", 64, 64, (0, 0), 2000),
    ("large.aligned", 128, 128, (0, 0), 2000),
    ("small.unligned", 0, 63, (0, 0), 2000),
    ("large.unaligned", 65, 127, (0, 0), 2000),
    ("mixed", 0, 513, (0, 0), 10000),
  )


object ScalarLineTokenizerFromArraySpec extends FromArraySpec:
  def createBI(a: Array[Byte], off: Int, len: Int): BufferedInput = BufferedInput.ofArray(a, off, len-off)
  def createTok(in: BufferedInput, cs: Charset): LineTokenizer = ScalarLineTokenizer.of(in, cs, '\n'.toByte, '\r'.toByte)

object VectorizedLineTokenizerFromArraySpec extends FromArraySpec:
  def createBI(a: Array[Byte], off: Int, len: Int): BufferedInput = BufferedInput.ofArray(a, off, len-off)
  def createTok(in: BufferedInput, cs: Charset): LineTokenizer = VectorizedLineTokenizer.of(in, cs, '\n'.toByte, '\r'.toByte)

object DirectScalarLineTokenizerSpec extends ForeignSpec:
  def createBI(a: Array[Byte], off: Int, len: Int): BufferedInput = BufferedInput.ofMemorySegment(MemorySegment.ofArray(a))
  def createTok(in: BufferedInput, cs: Charset): LineTokenizer = ScalarLineTokenizer.of(in, cs, '\n'.toByte, '\r'.toByte)

object DirectVectorizedLineTokenizerSpec extends ForeignSpec:
  def createBI(a: Array[Byte], off: Int, len: Int): BufferedInput = BufferedInput.ofMemorySegment(MemorySegment.ofArray(a))
  def createTok(in: BufferedInput, cs: Charset): LineTokenizer = VectorizedLineTokenizer.of(in, cs, '\n'.toByte, '\r'.toByte)
