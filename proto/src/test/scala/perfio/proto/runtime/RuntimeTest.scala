package perfio.proto.runtime

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import perfio.{BufferedInput, BufferedOutput}
import com.example.google.Simple as GSimple
import com.example.perfio.Simple as PSimple
import perfio.protoapi.DescriptorProtos as PDescriptorProtos

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import java.nio.ByteOrder

@RunWith(classOf[JUnit4])
class RuntimeTest:

  @Test
  def testVarint(): Unit =
    val t1 = Array[Byte](0x01.toByte)
    val t2 = Array[Byte](0x96.toByte, 0x01.toByte)
    def check(exp: Long, a: Array[Byte]): Unit = assertEquals(exp, Runtime.parseVarint(BufferedInput.ofArray(a)))
    check(1, t1)
    check(150, t2)

  @Test
  def testVarintRoundtrip(): Unit =
    def check(l: Long): Unit =
      val bo = BufferedOutput.growing()
      Runtime.writeVarint(bo, l)
      bo.close()
      val in = BufferedInput.ofArray(bo.buffer(), 0, bo.length())
      val l2 = Runtime.parseVarint(in)
      assertEquals(l, l2)
    check(0L)
    check(150L)
    check(-1L)
    check(10000L)
    check(Long.MaxValue)
    check(Long.MinValue)

  @Test
  def testSimple(): Unit =
    val gs = GSimple.SimpleMessage.newBuilder()
      .setI32(100).setI64(200).setE(GSimple.SimpleMessage.E.V3).addInts(10).addInts(20).addInts(30)
      .setNested(GSimple.Nested.newBuilder().setA(Int.MaxValue).setB(Int.MinValue))
      .addRNested(GSimple.Nested.newBuilder().setA(0))
      .addRNested(GSimple.Nested.newBuilder().setB(1))
      .addFile(GSimple.SimpleMessage.File.newBuilder().setName("file 1").setContent("content 1"))
      .addFile(GSimple.SimpleMessage.File.newBuilder().setName("file 2").setContent("content 2"))
      .build()
    val in = toInput(gs)
    val ps = PSimple.SimpleMessage.parseFrom(in)
    assertEquals(100, ps.getI32)
    assertEquals(200, ps.getI64)
    assertEquals(Seq(10, 20, 30), ps.getIntsList.copyToArray().toSeq)
    assertEquals(PSimple.SimpleMessage.E.V3, ps.getE)
    val n = ps.getNested
    assertEquals(Int.MaxValue, n.getA)
    assertEquals(Int.MinValue, n.getB)
    assertEquals(java.util.List.of(new PSimple.Nested(), new PSimple.Nested().setB(1)), ps.getRNestedList)

    val out2 = BufferedOutput.growing()
    ps.writeTo(out2)
    out2.close()
    val gs2 = GSimple.SimpleMessage.parseFrom(out2.copyToByteArray())

    assertEquals(gs, gs2)

  @Test
  def testSimpleFile(): Unit =
    val gs = GSimple.SimpleMessage.newBuilder()
      .addFile(GSimple.SimpleMessage.File.newBuilder().setName("f1"))
      .build()
    val in = toInput(gs)
    val ps = PSimple.SimpleMessage.parseFrom(in)
    val out2 = BufferedOutput.growing()
    ps.writeTo(out2)
    out2.close()
    val gs2 = GSimple.SimpleMessage.parseFrom(out2.copyToByteArray())
    assertEquals(gs, gs2)

  @Test
  def testCodeGeneratorRequest(): Unit =
    val data3 = Array[Byte](18, 24, 66, 10, -94, 1, 7, 18, 5, 65, 76, 76, 79, 87, 82, 10, 106, 115, 111, 110, 70, 111, 114, 109, 97, 116)
    val in = BufferedInput.of(new LimitedInputStream(new ByteArrayInputStream(data3), 5)).order(ByteOrder.LITTLE_ENDIAN)
    PDescriptorProtos.DescriptorProto.parseFrom(in)

  def toInput(b: com.google.protobuf.GeneratedMessage): BufferedInput =
    val bout = new ByteArrayOutputStream()
    b.writeTo(bout)
    BufferedInput.ofArray(bout.toByteArray).order(ByteOrder.LITTLE_ENDIAN)

  def toBytes(b: com.google.protobuf.GeneratedMessage): Array[Byte] =
    val bout = new ByteArrayOutputStream()
    b.writeTo(bout)
    bout.toByteArray


class LimitedInputStream(in: InputStream, limit: Int) extends InputStream:
  var returned = 0L
  def read: Int =
    val i = in.read()
    if(i >= 0) returned += 1
    i
  override def read(b: Array[Byte], off: Int, len: Int): Int =
    val l = in.read(b, off, len min limit)
    if(l > 0) returned += l
    l
