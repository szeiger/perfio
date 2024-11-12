package perfio.proto

import perfio.protoapi.DescriptorProtos.FileDescriptorProto
import perfio.TextOutput
import perfio.scalaapi.*

import java.io.PrintStream


class FileNode(desc: FileDescriptorProto, val root: RootNode) extends ParentNode:
  def file: FileNode = this
  val pbName: String = desc.getName
  private val pbPackage: String = desc.getPackage
  val pbFQName: String = s".$pbPackage"

  private val multipleFiles: Boolean = desc.getOptions.getJavaMultipleFiles

  private val outerClassName: String =
    if(desc.getOptions.hasJavaOuterClassname) desc.getOptions.getJavaOuterClassname
    else
      val n = pbName.split('/').last
      Util.mkUpperJavaName(if(n.endsWith(".proto")) n.dropRight(6) else n)

  private val packageName: String =
    root.packageOverride.getOrElse:
      if(desc.getOptions.hasJavaPackage) desc.getOptions.getJavaPackage else pbPackage

  val outputFileName =
    val s = s"${outerClassName}.java"
    if(packageName.isEmpty) s else packageName.replace('.', '/') + "/" + s

  private val fqOuterClass: String = if(packageName.isEmpty) outerClassName else s"${packageName}.$outerClassName"

  val fqName: String = if(multipleFiles) packageName else fqOuterClass

  val syntax: Syntax = desc.getSyntax match
    case "proto2" => Syntax.Proto2
    case "proto3" => Syntax.Proto3
    case _ => Syntax.Unknown

  desc.getEnumTypeList.forEach(e => enums += new EnumNode(e, this))
  desc.getMessageTypeList.forEach(m => messages += new MessageNode(m, this))

  override def toString: String = s"file $pbPackage/$pbName (Java: $fqOuterClass) $syntax"

  override def dump(out: PrintStream, prefix: String): Unit =
    out.println(s"${prefix}$this")
    super.dump(out, prefix)

  def emit(using toc: TextOutputContext): Printed =
    pm"""// Generated by perfio-proto. Do not edit!
        |// source: ${pbName}
        |"""
    if(packageName.nonEmpty)
      pm"""package $packageName;
          |"""
    pm"""public final class $outerClassName {
        |  private $outerClassName() {}"""
    toc.indented1:
      for e <- enums do
        toc.to.println
        e.emit
      for m <- messages do
        toc.to.println
        m.emit
    pm"""}"""


enum Syntax:
  case Proto2, Proto3, Unknown
