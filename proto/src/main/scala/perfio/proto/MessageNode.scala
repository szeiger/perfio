package perfio.proto

import perfio.protoapi.DescriptorProtos.DescriptorProto
import perfio.proto.Cardinality.Repeated
import perfio.proto.runtime.Runtime
import perfio.scalaapi.*
import perfio.{BufferedInput, BufferedOutput}

import java.io.PrintStream
import scala.collection.mutable.{ArrayBuffer, Buffer}
import scala.jdk.CollectionConverters.*

class MessageNode(val desc: DescriptorProto, val parent: ParentNode) extends ParentNode:
  val root: RootNode = parent.root
  val file: FileNode = parent.file
  val fields = new ArrayBuffer[FieldNode]
  val name: String = desc.getName
  def javaName: String = name
  val fqName: String = s"${parent.fqName}.$name"
  val fqJavaName: String = s"${parent.fqJavaName}.$javaName"
  val isMapEntry: Boolean = desc.getOptions.getMapEntry
  val oneOfs: Buffer[OneOfNode] = desc.getOneofDeclList.asScala.map(new OneOfNode(_, this))

  var flagCount = 0
  def flagFieldForIdx(i: Int): String = "flags"+(i/64)
  def flagBitForIdx(i: Int): String = if(flagCount <= 32) s"${1 << i}" else s"1 << ${i%64}L"

  desc.getEnumTypeList.forEach(e => enums += new EnumNode(e, this))
  desc.getFieldList.forEach(f => fields += new FieldNode(f, this))
  desc.getNestedTypeList.forEach(m => messages += new MessageNode(m, this))

  override def toString: String = s"message $name${if(isMapEntry) " (map entry)" else ""}"

  override def dump(out: PrintStream, prefix: String): Unit =
    out.println(s"${prefix}$this")
    super.dump(out, prefix)
    fields.foreach(_.dump(out, prefix + "  "))
    oneOfs.foreach(_.dump(out, prefix + "  "))

  def emit(using toc: TextOutputContext): Printed =
    pm"""public static final class ${javaName} {"""
    toc.indented:
      enums.foreach(_.emit(toc.to, toc.prefix))
      for m <- messages do
        toc.to.println()
        m.emit
    if(flagCount > 0)
      toc.to.println()
      if(flagCount <= 32)
        pm"""  private int ${flagFieldForIdx(0)};"""
      else
        for(i <- 0 until flagCount by 64)
          pm"""  private long ${flagFieldForIdx(i)};"""
    oneOfs.foreach(o => if(!o.synthetic) o.emit(toc.to, toc.prefix+"  "))
    toc.indented:
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
    pm"""public static $fqJavaName parseFrom(${classOf[BufferedInput].getName} in) throws java.io.IOException {
        |  var m = new $fqJavaName();
        |  parseFrom(in, m);
        |  return m;
        |}
        |public static void parseFrom(${classOf[BufferedInput].getName} in, $fqJavaName base) throws java.io.IOException {
        |  while(in.hasMore()) {
        |    int tag = (int)$p.parseVarint(in);
        |    switch(tag) {"""
    for f <- fields do
      pm"      case ${f.tag} -> ${f.javaParseExpr("base", p, "(in)")}"
      if(f.tpe.canBePacked)
        if(f.tpe.javaType == "int" && f.packed)
          pm"      case ${f.packedTag} -> { var in2 = in.delimitedView($p.parseLen(in)); base.${f.javaFieldName}_initMut(); while(in2.hasMore()) base.${f.javaFieldName}.add($p.parseInt32(in2)); in2.close(); }"
        else
          pm"      case ${f.packedTag} -> { var in2 = in.delimitedView($p.parseLen(in)); while(in2.hasMore()) ${f.javaParseExpr("base", p, "(in2)")}; in2.close(); }"
    pm"""      default -> parseOther(in, tag);
        |    }
        |  }
        |}
        |private static void parseOther(${classOf[BufferedInput].getName} in, int tag) throws java.io.IOException {
        |  int wt = tag & 7;
        |  int field = tag >>> 3;
        |  switch(field) {"""
    val known = fields.map(_.number)
    if(known.nonEmpty)
      pm"    case ${known.mkString(", ")} -> throw $p.invalidWireType(wt, field);"
    pm"""    default -> $p.skip(in, wt);
        |  }
        |}"""

  //TODO optimize oneof writing
  private def emitWriter(using toc: TextOutputContext): Printed =
    val p = classOf[Runtime].getName
    pm"public void writeTo(${classOf[BufferedOutput].getName} out) throws java.io.IOException {"
    for f <- fields do
      if(f.packed && f.tpe.javaType == "int")
        pm"  if(this.${f.javaHazzer}()) { ${writeVarint(f.packedTag, "out")}; $p.writePackedInt32(out, this.${f.javaFieldName}); }"
      else if(f.packed)
        pm"""  if(this.${f.javaHazzer}()) {
            |    var it = this.${f.javaFieldName}.iterator();
            |    ${writeVarint(f.packedTag, "out")};
            |    var out2 = out.defer();
            |    while(it.hasNext()) { var v = it.next(); ${f.javaWriteStatements(p, "out2", "v")} }
            |    $p.writeVarint(out, out2.totalBytesWritten());
            |    out2.close();
            |  }"""
      else if(f.cardinality == Cardinality.Repeated)
        pm"  if(this.${f.javaHazzer}()) { var it = this.${f.javaFieldName}.iterator(); while(it.hasNext()) { var v = it.next(); ${writeVarint(f.tag, "out")}; ${f.javaWriteStatements(p, "out", "v")} }}"
      else
        pm"  if(this.${f.javaHazzer}()) { ${writeVarint(f.tag, "out")}; ${f.javaWriteStatements(p, "out", s"this.${f.javaFieldName}")} }"
    pm"}"

  private def writeVarint(v: Long, bo: String): String =
    Util.encodeVarint(v).map { i => s"$bo.int8((byte)$i)" }.mkString("; ")

  private def emitEquals(using TextOutputContext): Printed =
    pm"""public boolean equals(java.lang.Object o) {
        |  if(o == this) return true;
        |  else if(o instanceof ${fqJavaName} m) {"""
    for i <- 0 until flagCount by 64 do
      pm"    if(this.${flagFieldForIdx(i)} != m.${flagFieldForIdx(i)}) return false;"
    for o <- oneOfs if !o.synthetic do
      pm"    if(this.${o.javaFieldName} != m.${o.javaFieldName}) return false;"
    for f <- fields do
      if(f.tpe.javaHasPrimitiveEquality && f.cardinality != Repeated)
        pm"    if(this.${f.javaFieldName} != m.${f.javaFieldName}) return false;"
      else if(f.oneOf.isDefined || f.flagIndex >= 0)
        pm"    if(this.${f.javaHazzer}() && !this.${f.javaFieldName}.equals(m.${f.javaFieldName})) return false;"
      else //TODO do we ever need this?
        pm"    if(!this.${f.javaFieldName}.equals(m.${f.javaFieldName})) return false;"
    pm"""    return true;
        |  } else return false;
        |}"""
