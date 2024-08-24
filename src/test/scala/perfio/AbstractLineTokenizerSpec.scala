package perfio

import hedgehog._
import hedgehog.runner._

import java.io.{ByteArrayInputStream, InputStream}
import java.lang.foreign.MemorySegment
import java.nio.charset.{Charset, StandardCharsets}
import java.util.Arrays
import scala.collection.mutable
import scala.jdk.CollectionConverters._

abstract class AbstractLineTokenizerSpec[T] extends Properties {

  def base: List[(String, Int, Int, T, Int)]

  def createTokenizer(s: String, cs: Charset, params: T): LineTokenizer

  def tests: List[Test] =
    for {
      cs <- List(StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1)
      t <- base.map { case (n, min, max, params, count) => property(s"${n}.$cs", createProp(min, max, cs, params)).config(_.copy(testLimit = count)) }
    } yield t

  def createProp(min: Int, max: Int, cs: Charset, params: T): Property =
    for {
      s <- Gen.string(Gen.element1('a', '\n'), Range.linear(min, max)).forAll
    } yield res(s, createTokenizer(s, cs, params))

  def res(s: String, t: LineTokenizer): Result = {
    try {
      val buf = mutable.ArrayBuffer.empty[Int]
      while(t.readLine() match {
        case null => false
        case s => buf += s.length; true
      }) ()
      val next = t.readLine()
      val exp = s.lines().toList.asScala.map(_.length)
      Result.all(List(
        (buf: mutable.Buffer[Int]) ==== exp,
        next ==== null
      ))
    } finally t.close()
  }
}

class LimitedInputStream(in: InputStream, limit: Int) extends InputStream {
  def read(): Int = in.read()
  override def read(b: Array[Byte], off: Int, len: Int): Int = in.read(b, off, len min limit)
}

abstract class ScalarLineTokenizerSpec extends AbstractLineTokenizerSpec[Int] {
  val base: List[(String, Int, Int, Int, Int)] = List(
    ("small.aligned", 64, 64, 4096, 10000),
    ("large.aligned", 128, 128, 64, 10000),
    ("small.unligned", 0, 63, 4096, 10000),
    ("large.unaligned", 65, 127, 64, 10000),
    ("mixed", 0, 513, 128, 50000),
  )
}

object ScalarLineTokenizerSpec extends ScalarLineTokenizerSpec {
  def createTokenizer(s: String, cs: Charset, ib: Int): LineTokenizer =
    ScalarLineTokenizer(new ByteArrayInputStream(s.getBytes(cs)), cs, ib)
}

object LineTokenizerSpec extends ScalarLineTokenizerSpec {
  def createTokenizer(s: String, cs: Charset, ib: Int): LineTokenizer =
    LineTokenizer(new ByteArrayInputStream(s.getBytes(cs)), cs, ib)
}

object VectorizedLineTokenizerSpec extends AbstractLineTokenizerSpec[(Int, Int)] {
  val base: List[(String, Int, Int, (Int, Int), Int)] = List(
    ("small.aligned", 64, 64, (4096, Int.MaxValue), 10000),
    ("large.aligned", 128, 128, (64, Int.MaxValue), 10000),
    ("small.unaligned", 0, 63, (4096, Int.MaxValue), 10000),
    ("large.unaligned", 65, 127, (64, Int.MaxValue), 10000),
    ("small.aligned.limited1", 64, 64, (4096, 1), 10000),
    ("large.aligned.limited1", 128, 128, (64, 1), 10000),
    ("small.unaligned.limited1", 0, 63, (4096, 1), 10000),
    ("large.unaligned.limited1", 65, 127, (64, 1), 10000),
    ("small.aligned.limited16", 64, 64, (4096, 16), 10000),
    ("large.aligned.limited16", 128, 128, (64, 16), 10000),
    ("small.unaligned.limited16", 0, 63, (4096, 16), 10000),
    ("large.unaligned.limited16", 65, 127, (64, 16), 10000),
    ("mixed", 0, 513, (128, Int.MaxValue), 50000),
  )

  def createTokenizer(s: String, cs: Charset, params: (Int, Int)): LineTokenizer = {
    val (ib, maxRead) = params
    VectorizedLineTokenizer(new LimitedInputStream(new ByteArrayInputStream(s.getBytes(cs)), maxRead), cs, ib)
  }
}

abstract class FromArraySpec extends AbstractLineTokenizerSpec[(Int, Int)] {
  val base: List[(String, Int, Int, (Int, Int), Int)] = List(
    ("small.aligned", 64, 64, (0, 0), 10000),
    ("large.aligned", 128, 128, (0, 0), 10000),
    ("small.unligned", 0, 63, (0, 0), 10000),
    ("large.unaligned", 65, 127, (0, 0), 10000),
    ("padRight", 0, 65, (0, 33), 10000),
    ("padLeft", 0, 65, (33, 0), 10000),
    ("padBoth", 0, 65, (33, 33), 10000),
    ("mixed", 0, 513, (0, 0), 10000),
  )

  override def createProp(min: Int, max: Int, cs: Charset, params: (Int, Int)): Property = {
    val (padLeft, padRight) = params
    for {
      s <- Gen.string(Gen.element1('a', '\n'), Range.linear(min, max)).forAll
      pl <- Gen.int(Range.linear(if(padLeft == 0) 0 else 1, padLeft)).forAll
      pr <- Gen.int(Range.linear(if(padRight == 0) 0 else 1, padRight)).forAll
    } yield res(s, createTokenizer(s, cs, (pl, pr)))
  }

  def createTokenizer(s: String, cs: Charset, params: (Int, Int)): LineTokenizer = {
    val (padLeft, padRight) = params
    val bytes = s.getBytes(cs)
    val a = new Array[Byte](bytes.length+padLeft+padRight)
    Arrays.fill(a, '\n'.toByte)
    System.arraycopy(bytes, 0, a, padLeft, bytes.length)
    create(a, padLeft, bytes.length+padLeft)
  }

  def create(a: Array[Byte], off: Int, len: Int): LineTokenizer
}

abstract class ForeignSpec extends FromArraySpec {
  override val base: List[(String, Int, Int, (Int, Int), Int)] = List(
    ("small.aligned", 64, 64, (0, 0), 10000),
    ("large.aligned", 128, 128, (0, 0), 10000),
    ("small.unligned", 0, 63, (0, 0), 10000),
    ("large.unaligned", 65, 127, (0, 0), 10000),
    ("mixed", 0, 513, (0, 0), 10000),
  )
}

object ScalarLineTokenizerFromArraySpec extends FromArraySpec {
  def create(a: Array[Byte], off: Int, len: Int): LineTokenizer =
    ScalarLineTokenizer.fromArray(a, off, len)
}

object VectorizedLineTokenizerFromArraySpec extends FromArraySpec {
  def create(a: Array[Byte], off: Int, len: Int): LineTokenizer =
    VectorizedLineTokenizer.fromArray(a, off, len)
}

object ScalarForeignLineTokenizerSpec extends ForeignSpec {
  def create(a: Array[Byte], off: Int, len: Int): LineTokenizer =
    ScalarForeignLineTokenizer.fromMemorySegment(MemorySegment.ofArray(a))
}

object VectorizedForeignLineTokenizerSpec extends ForeignSpec {
  def create(a: Array[Byte], off: Int, len: Int): LineTokenizer =
    VectorizedForeignLineTokenizer.fromMemorySegment(MemorySegment.ofArray(a))
}
