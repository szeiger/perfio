package perfio.proto

import perfio.protoapi.DescriptorProtos.EnumDescriptorProto
import perfio.TextOutput

import java.io.PrintStream
import scala.collection.mutable
import scala.collection.mutable.Buffer
import scala.jdk.CollectionConverters.*


class EnumNode(val desc: EnumDescriptorProto, val parent: ParentNode) extends Node:
  val name: String = desc.getName
  def javaName: String = name
  val fqName: String = s"${parent.fqName}.$name"
  val fqJavaName: String = s"${parent.fqJavaName}.$javaName"
  val values: Buffer[(String, Int)] = desc.getValueList.asScala.map { v => (v.getName, v.getNumber) }
  val uniqueValues: Buffer[(String, Int)] =
    val map = mutable.HashMap.empty[Int, String]
    values.foreach { case (n, i) => map.put(i, n) }
    values.map { case (n, i) => (map(i), i) }

  override def toString: String = s"enum $name"

  override def dump(out: PrintStream, prefix: String): Unit =
    out.println(s"${prefix}$this")
    values.foreach { case (n, i) => s"$prefix  $n $i"}

  def emit(to: TextOutput, prefix: String): Unit =
    to.println(s"${prefix}public enum $javaName {")
    for (n, i) <- values do
      to.println(s"${prefix}  $n($i),")
    to.println(s"${prefix}  UNRECOGNIZED(-1);")
    to.println(s"${prefix}  public final int number;")
    to.println(s"${prefix}  $javaName(int number) { this.number = number; }")
    to.println(s"${prefix}  public static $javaName valueOf(int number) {")
    to.println(s"${prefix}    return switch(number) {")
    for (n, i) <- uniqueValues do
      to.println(s"${prefix}      case $i -> $n;")
    to.println(s"${prefix}      default -> UNRECOGNIZED;")
    to.println(s"${prefix}    };")
    to.println(s"${prefix}  }")
    to.println(s"${prefix}}")
