package perfio.proto

import perfio.protoapi.DescriptorProtos.EnumDescriptorProto
import perfio.TextOutput
import perfio.scalaapi.*

import java.io.PrintStream
import scala.collection.mutable
import scala.collection.mutable.Buffer
import scala.jdk.CollectionConverters.*


class EnumNode(desc: EnumDescriptorProto, val parent: ParentNode) extends Node:
  val pbName: String = desc.getName
  val pbFQName: String = s"${parent.pbFQName}.$pbName"

  def enumName: String = pbName
  val fqName: String = s"${parent.fqName}.$enumName"
  val values: Buffer[(String, Int)] = desc.getValueList.asScala.map { v => (v.getName, v.getNumber) }
  private val uniqueValues: Buffer[(String, Int)] =
    val map = mutable.HashMap.empty[Int, String]
    values.foreach { case (n, i) => map.put(i, n) }
    values.map { case (n, i) => (map(i), i) }

  override def toString: String = s"enum $pbName"

  override def dump(out: PrintStream, prefix: String): Unit =
    out.println(s"${prefix}$this")
    values.foreach { case (n, i) => s"$prefix  $n $i"}

  def emit(using TextOutputContext): Printed =
    pm"""public enum $enumName {"""
    for (n, i) <- values do
      pm"""  $n($i),"""
    pm"""  UNRECOGNIZED(-1);
        |  public final int number;
        |  $enumName(int number) { this.number = number; }
        |  public static $enumName valueOf(int number) {
        |    return switch(number) {"""
    for (n, i) <- uniqueValues do
      pm"""      case $i -> $n;"""
    pm"""      default -> UNRECOGNIZED;
        |    };
        |  }
        |}"""
