package perfio.proto

import perfio.protoapi.DescriptorProtos.DescriptorProto
import perfio.proto.Cardinality.Repeated
import perfio.proto.runtime.Runtime
import perfio.{BufferedInput, BufferedOutput, TextOutput}

import java.io.PrintStream
import scala.collection.mutable.{ArrayBuffer, Buffer}
import scala.jdk.CollectionConverters._

class MessageNode(val desc: DescriptorProto, val parent: ParentNode) extends ParentNode {
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

  override def dump(out: PrintStream, prefix: String): Unit = {
    out.println(s"${prefix}$this")
    super.dump(out, prefix)
    fields.foreach(_.dump(out, prefix + "  "))
    oneOfs.foreach(_.dump(out, prefix + "  "))
  }

  def emit(to: TextOutput, prefix: String): Unit = {
    to.println(s"${prefix}public static final class ${javaName} {")
    enums.foreach(_.emit(to, prefix + "  "))
    messages.foreach { m =>
      to.println()
      m.emit(to, prefix + "  ")
    }
    if(flagCount > 0) {
      to.println()
      if(flagCount <= 32)
        to.println(s"${prefix}  private int ${flagFieldForIdx(0)};")
      else {
        for(i <- 0 until flagCount by 64)
          to.println(s"${prefix}  private long ${flagFieldForIdx(i)};")
      }
    }
    oneOfs.foreach(o => if(!o.synthetic) o.emit(to, prefix+"  "))
    fields.foreach { f =>
      to.println()
      f.emit(to, prefix+"  ")
    }
    to.println()
    emitParser(to, prefix + "  ")
    to.println()
    emitWriter(to, prefix + "  ")
    to.println()
    emitEquals(to, prefix + "  ") //TODO hashCode
    to.println(s"${prefix}}")
  }

  private[this] def emitParser(to: TextOutput, prefix: String): Unit = {
    val p = classOf[Runtime].getName
    to.println(s"${prefix}public static $fqJavaName parseFrom(${classOf[BufferedInput].getName} in) throws java.io.IOException {")
    to.println(s"${prefix}  var m = new $fqJavaName();")
    to.println(s"${prefix}  parseFrom(in, m);")
    to.println(s"${prefix}  return m;")
    to.println(s"${prefix}}")
    to.println(s"${prefix}public static void parseFrom(${classOf[BufferedInput].getName} in, $fqJavaName base) throws java.io.IOException {")
    to.println(s"${prefix}  while(in.hasMore()) {")
    to.println(s"${prefix}    int tag = (int)$p.parseVarint(in);")
    to.println(s"${prefix}    switch(tag) {")
    fields.foreach { f =>
      to.println(s"${prefix}      case ${f.tag} -> ${f.javaParseExpr("base", p, "(in)")}")
      if(f.tpe.canBePacked)
        to.println(s"${prefix}      case ${f.packedTag} -> { var in2 = in.delimitedView($p.parseLen(in)); while(in2.hasMore()) ${f.javaParseExpr("base", p, "(in2)")}; in2.close(); }")
    }
    to.println(s"${prefix}      default -> parseOther(in, tag);")
    to.println(s"${prefix}    }")
    to.println(s"${prefix}  }")
    to.println(s"${prefix}}")
    to.println(s"${prefix}private static void parseOther(${classOf[BufferedInput].getName} in, int tag) throws java.io.IOException {")
    to.println(s"${prefix}  int wt = tag & 7;")
    to.println(s"${prefix}  int field = tag >>> 3;")
    to.println(s"${prefix}  switch(field) {")
    val known = fields.map(_.number)
    if(known.nonEmpty)
      to.println(s"${prefix}    case ${known.mkString(", ")} -> throw $p.invalidWireType(wt, field);")
    to.println(s"${prefix}    default -> $p.skip(in, wt);")
    to.println(s"${prefix}  }")
    to.println(s"${prefix}}")
  }

  //TODO optimize oneof writing
  private[this] def emitWriter(to: TextOutput, prefix: String): Unit = {
    val p = classOf[Runtime].getName
    to.println(s"${prefix}public void writeTo(${classOf[BufferedOutput].getName} out) throws java.io.IOException {")
    for(f <- fields) {
      val tag = (f.number << 3) | f.tpe.wireType
      if(f.packed) {
        to.println(s"${prefix}  if(this.${f.javaHazzer}()) {")
        to.println(s"${prefix}    var it = this.${f.javaFieldName}.iterator();")
        to.println(s"${prefix}    ${writeVarint(f.number << 3 | Runtime.LEN, "out")};")
        to.println(s"${prefix}    var out2 = out.defer();")
        to.println(s"${prefix}    while(it.hasNext()) { var v = it.next(); ${f.javaWriteStatements(p, "out2", "v")} }")
        to.println(s"${prefix}    $p.writeVarint(out, out2.totalBytesWritten());")
        to.println(s"${prefix}    out2.close();")
        to.println(s"${prefix}  }")
      } else if(f.cardinality == Cardinality.Repeated) {
        to.println(s"${prefix}  if(this.${f.javaHazzer}()) { var it = this.${f.javaFieldName}.iterator(); while(it.hasNext()) { var v = it.next(); ${writeVarint(tag, "out")}; ${f.javaWriteStatements(p, "out", "v")} }}")
      } else {
        to.println(s"${prefix}  if(this.${f.javaHazzer}()) { ${writeVarint(tag, "out")}; ${f.javaWriteStatements(p, "out", s"this.${f.javaFieldName}")} }")
      }
    }
    to.println(s"${prefix}}")
  }

  private[this] def writeVarint(v: Long, bo: String): String =
    Util.encodeVarint(v).map { i => s"$bo.int8((byte)$i)" }.mkString("; ")

  private[this] def emitEquals(to: TextOutput, prefix: String): Unit = {
    to.println(s"${prefix}public boolean equals(java.lang.Object o) {")
    to.println(s"${prefix}  if(o == this) return true;")
    to.println(s"${prefix}  else if(o instanceof ${fqJavaName} m) {")
    for(i <- 0 until flagCount by 64)
      to.println(s"${prefix}    if(this.${flagFieldForIdx(i)} != m.${flagFieldForIdx(i)}) return false;")
    for(o <- oneOfs if !o.synthetic)
      to.println(s"${prefix}    if(this.${o.javaFieldName} != m.${o.javaFieldName}) return false;")
    for(f <- fields) {
      if(f.tpe.javaHasPrimitiveEquality && f.cardinality != Repeated)
        to.println(s"${prefix}    if(this.${f.javaFieldName} != m.${f.javaFieldName}) return false;")
      else if(f.oneOf.isDefined || f.flagIndex >= 0)
        to.println(s"${prefix}    if(this.${f.javaHazzer}() && !this.${f.javaFieldName}.equals(m.${f.javaFieldName})) return false;")
      else //TODO do we ever need this?
        to.println(s"${prefix}    if(!this.${f.javaFieldName}.equals(m.${f.javaFieldName})) return false;")
    }
    to.println(s"${prefix}    return true;")
    to.println(s"${prefix}  } else return false;")
    to.println(s"${prefix}}")
  }
}
