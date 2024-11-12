package perfio.proto

import perfio.protoapi.DescriptorProtos.OneofDescriptorProto
import perfio.scalaapi.*

import java.io.PrintStream
import scala.collection.mutable.ArrayBuffer


class OneOfNode(desc: OneofDescriptorProto, parent: MessageNode) extends Node:
  val pbName = desc.getName

  val field: String = s"oneof_${Util.mkLowerJavaName(pbName)}"
  var synthetic: Boolean = false
  val fields = ArrayBuffer.empty[FieldNode]

  override def toString: String = s"oneof $pbName${if(synthetic) " (synthetic)" else ""}"

  override def dump(out: PrintStream, prefix: String): Unit =
    out.println(s"${prefix}$this")

  def emit(using TextOutputContext): Printed =
    pm"""private int $field;"""
