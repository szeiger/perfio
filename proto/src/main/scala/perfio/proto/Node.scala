package perfio.proto

import perfio.BufferedOutput
import perfio.proto.runtime.Runtime
import perfio.protoapi.PluginProtos.CodeGeneratorRequest

import java.io.PrintStream
import scala.collection.mutable.{ArrayBuffer, Buffer}
import scala.jdk.CollectionConverters._


trait Node {
  def dump(out: PrintStream, prefix: String): Unit
}


class RootNode(val req: CodeGeneratorRequest) extends Node {
  val packageOverride = sys.env.get("PERFIO_PACKAGE")
  val genFiles: Buffer[String] = req.getFileToGenerateList.asScala
  val files = req.getProtoFileList.asScala.map(new FileNode(_, this))
  val fileMap = files.map { fm => (fm.name, fm) }.toMap
  val allMessages = files.iterator.flatMap(_.allMessages).map(m => (m.fqName, m)).toMap
  val allEnums = files.iterator.flatMap(_.allEnums).map(e => (e.fqName, e)).toMap

  def dump(out: PrintStream, prefix: String): Unit = {
    files.foreach(_.dump(System.err, prefix))
  }
}


trait ParentNode extends Node { self =>
  val messages = new ArrayBuffer[MessageNode]
  val enums = new ArrayBuffer[EnumNode]
  def root: RootNode
  def file: FileNode
  def fqName: String
  def fqJavaName: String

  def allMessages: Iterator[MessageNode] = messages.iterator ++ messages.iterator.flatMap(_.allMessages)
  def allEnums: Iterator[EnumNode] = enums.iterator ++ allMessages.iterator.flatMap(_.allEnums)

  def dump(out: PrintStream, prefix: String): Unit = {
    messages.foreach(_.dump(out, prefix + "  "))
    enums.foreach(_.dump(out, prefix + "  "))
  }
}


object Util {
  def mkUpperJavaName(s: String): String = {
    val b = new StringBuilder(s.length)
    var up = true
    s.foreach { c =>
      if(c == '_') up = true
      else if(c >= '0' && c <= '9') { b.append(c); up = true }
      else if(up) { up = false; b.append(c.toUpper) }
      else b.append(c)
    }
    b.result()
  }

  def mkLowerJavaName(s: String): String = {
    val u = mkUpperJavaName(s)
    u.head.toLower + u.drop(1)
  }

  def encodeVarint(v: Long): Array[Byte] = {
    val bo = BufferedOutput.growing(10)
    Runtime.writeVarint(bo, v)
    bo.close()
    bo.copyToByteArray()
  }
}
