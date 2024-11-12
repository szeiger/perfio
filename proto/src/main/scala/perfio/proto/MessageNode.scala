package perfio.proto

import perfio.protoapi.DescriptorProtos.DescriptorProto
import perfio.proto.Cardinality.Repeated
import perfio.proto.runtime.Runtime
import perfio.scalaapi.*
import perfio.{BufferedInput, BufferedOutput}

import java.io.PrintStream
import scala.collection.mutable.{ArrayBuffer, Buffer}
import scala.jdk.CollectionConverters.*

class MessageNode(desc: DescriptorProto, val parent: ParentNode) extends ParentNode:
  private val pbName: String = desc.getName
  val pbFQName: String = s"${parent.pbFQName}.$pbName"

  val root: RootNode = parent.root
  val file: FileNode = parent.file
  private val fields = new ArrayBuffer[FieldNode]
  private def className: String = pbName
  val fqName: String = s"${parent.fqName}.$className"
  private val isMapEntry: Boolean = desc.getOptions.getMapEntry
  val oneOfs: Buffer[OneOfNode] = desc.getOneofDeclList.asScala.map(new OneOfNode(_, this))

  var flagCount = 0
  def flagFieldForIdx(i: Int): String = "flags"+(i/64)
  def flagBitForIdx(i: Int): String = if(flagCount <= 32) s"${1 << i}" else s"1 << ${i%64}L"
  def flagFields = for i <- (0 until flagCount by 64).iterator yield flagFieldForIdx(i)

  desc.getEnumTypeList.forEach(e => enums += new EnumNode(e, this))
  desc.getFieldList.forEach(f => fields += new FieldNode(f, this))
  desc.getNestedTypeList.forEach(m => messages += new MessageNode(m, this))

  override def toString: String = s"message $pbName${if(isMapEntry) " (map entry)" else ""}"

  override def dump(out: PrintStream, prefix: String): Unit =
    out.println(s"${prefix}$this")
    super.dump(out, prefix)
    fields.foreach(_.dump(out, prefix + "  "))
    oneOfs.foreach(_.dump(out, prefix + "  "))

  def emit(using toc: TextOutputContext): Printed =
    pm"""public static final class ${className} {"""
    toc.indented1:
      enums.foreach(_.emit)
      for m <- messages do
        toc.to.println()
        m.emit
    if(flagCount > 0)
      toc.to.println()
      if(flagCount <= 32)
        pm"""  private int ${flagFieldForIdx(0)};"""
      else for(f <- flagFields)
        pm"""  private long $f;"""
    toc.indented1:
      oneOfs.foreach(o => if(!o.synthetic) o.emit)
      for f <- fields do
        toc.to.println()
        f.emit
      toc.to.println()
      emitParser
      toc.to.println()
      emitWriter
      toc.to.println()
      emitEquals //TODO hashCode
    pm"""}"""

  private def emitParser(using toc: TextOutputContext): Printed =
    val p = classOf[Runtime].getName
    pm"""public static $fqName parseFrom(${classOf[BufferedInput].getName} in) throws java.io.IOException {
        |  var m = new $fqName();
        |  parseFrom(in, m);
        |  return m;
        |}
        |public static void parseFrom(${classOf[BufferedInput].getName} in, $fqName base) throws java.io.IOException {
        |  while(in.hasMore()) {
        |    int tag = (int)$p.parseVarint(in);
        |    switch(tag) {"""
    toc.indented3(for f <- fields do f.emitParser)
    pm"""      default -> parseOther(in, tag);
        |    }
        |  }
        |}
        |private static void parseOther(${classOf[BufferedInput].getName} in, int tag) throws java.io.IOException {
        |  int wt = tag & 7;
        |  int field = tag >>> 3;
        |  switch(field) {"""
    if(fields.nonEmpty)
      pm"    case ${fields.map(_.number).mkString(", ")} -> throw $p.invalidWireType(wt, field);"
    pm"""    default -> $p.skip(in, wt);
        |  }
        |}"""

  //TODO optimize oneof writing
  private def emitWriter(using toc: TextOutputContext): Printed =
    val p = classOf[Runtime].getName
    pm"public void writeTo(${classOf[BufferedOutput].getName} out) throws java.io.IOException {"
    toc.indented1(for f <- fields do f.emitWriter)
    pm"}"

  private def emitEquals(using toc: TextOutputContext): Printed =
    pm"""public boolean equals(java.lang.Object o) {
        |  if(o == this) return true;
        |  else if(o instanceof ${fqName} m) {"""
    for f <- flagFields do
      pm"    if(this.$f != m.$f) return false;"
    for o <- oneOfs if !o.synthetic do
      pm"    if(this.${o.field} != m.${o.field}) return false;"
    toc.indented2(for f <- fields do f.emitEquals)
    pm"""    return true;
        |  } else return false;
        |}"""
