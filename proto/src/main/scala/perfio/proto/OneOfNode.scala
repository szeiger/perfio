package perfio.proto

import perfio.protoapi.DescriptorProtos.OneofDescriptorProto
import perfio.TextOutput

import java.io.PrintStream
import scala.collection.mutable.ArrayBuffer


class OneOfNode(val desc: OneofDescriptorProto, parent: MessageNode) extends Node {
  val name = desc.getName
  val javaLowerName: String = Util.mkLowerJavaName(name)
  val javaFieldName: String = s"oneof_$javaLowerName"
  var synthetic: Boolean = false
  val fields = ArrayBuffer.empty[FieldNode]

  override def toString: String = s"oneof $name${if(synthetic) " (synthetic)" else ""}"

  override def dump(out: PrintStream, prefix: String): Unit = {
    out.println(s"${prefix}$this")
  }

  def emit(to: TextOutput, prefix: String): Unit =
    to.println(s"${prefix}private int $javaFieldName;")
}
