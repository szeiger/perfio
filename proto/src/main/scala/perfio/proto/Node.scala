package perfio.proto

import perfio.BufferedOutput
import perfio.proto.runtime.Runtime
import perfio.protoapi.PluginProtos.CodeGeneratorRequest
import perfio.scalaapi.*

import java.io.PrintStream
import scala.collection.mutable.{ArrayBuffer, Buffer}
import scala.jdk.CollectionConverters.*


trait Node:
  def dump(out: PrintStream, prefix: String): Unit


class RootNode(req: CodeGeneratorRequest) extends Node:
  val packageOverride = sys.env.get("PERFIO_PACKAGE")
  val genFiles: Buffer[String] = req.getFileToGenerateList.asScala
  val files = req.getProtoFileList.asScala.map(new FileNode(_, this))
  val fileMap = files.map { fm => (fm.pbName, fm) }.toMap
  val allMessages = files.iterator.flatMap(_.allMessages).map(m => (m.pbFQName, m)).toMap
  val allEnums = files.iterator.flatMap(_.allEnums).map(e => (e.pbFQName, e)).toMap

  def dump(out: PrintStream, prefix: String): Unit =
    files.foreach(_.dump(System.err, prefix))


trait ParentNode extends Node:
  val messages = new ArrayBuffer[MessageNode]
  val enums = new ArrayBuffer[EnumNode]
  def root: RootNode
  def file: FileNode
  def pbFQName: String

  def fqName: String

  def allMessages: Iterator[MessageNode] = messages.iterator ++ messages.iterator.flatMap(_.allMessages)
  def allEnums: Iterator[EnumNode] = enums.iterator ++ allMessages.iterator.flatMap(_.allEnums)

  def dump(out: PrintStream, prefix: String): Unit =
    messages.foreach(_.dump(out, prefix + "  "))
    enums.foreach(_.dump(out, prefix + "  "))


object Util:
  def mkUpperJavaName(s: String): String =
    val b = new StringBuilder(s.length)
    var up = true
    for c <- s do
      if(c == '_') up = true
      else if(c >= '0' && c <= '9') { b.append(c); up = true }
      else if(up) { up = false; b.append(c.toUpper) }
      else b.append(c)
    b.result()

  def mkLowerJavaName(s: String): String =
    val u = mkUpperJavaName(s)
    u.head.toLower + u.drop(1)

  def encodeVarint(v: Long): Array[Byte] =
    val bo = BufferedOutput.growing(10)
    Runtime.writeVarint(bo, v)
    bo.close()
    bo.copyToByteArray()

  val RT = classOf[Runtime].getName


inline given classPrintable: Printable[Class[?]] with
  def print(toc: TextOutputContext, v: Class[?]): Printed =
    toc.to.print(v.getName)
    Printed
