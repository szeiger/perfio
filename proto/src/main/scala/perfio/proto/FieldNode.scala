package perfio.proto

import perfio.protoapi.DescriptorProtos.FieldDescriptorProto
import perfio.protoapi.DescriptorProtos.FieldDescriptorProto.{Label, Type}
import perfio.proto.runtime.{IntList, Runtime}
import perfio.scalaapi.*
import Util.RT

import java.io.PrintStream

class FieldNode(desc: FieldDescriptorProto, val parent: MessageNode) extends Node:
  def file: FileNode = parent.file
  val pbName: String = desc.getName
  private val pbDefaultValue: Option[String] = if(desc.hasDefaultValue) Some(desc.getDefaultValue) else None

  private val lowerName: String = Util.mkLowerJavaName(pbName)
  private val upperName: String = Util.mkUpperJavaName(pbName)
  val field: String = s"_$lowerName"
  val number: Int = desc.getNumber
  val tpe: Tpe = Tpe.ofProtoType(desc.getType, desc.getTypeName, parent.root)
  def tag: Int = (number << 3) | tpe.wireType
  def packedTag: Int = (number << 3) | Runtime.LEN
  private val isProto3Optional: Boolean = desc.getProto3Optional

  val card: Cardinality = desc.getLabel match
    case Label.LABEL_OPTIONAL =>
      if(file.syntax == Syntax.Proto3 && !(isProto3Optional || desc.getType == Type.TYPE_MESSAGE)) Cardinality.Implicit
      else Cardinality.Optional
    case Label.LABEL_REPEATED => Cardinality.Repeated
    case Label.LABEL_REQUIRED => Cardinality.Required
    case _ => Cardinality.Implicit
  def isRepeated: Boolean = card == Cardinality.Repeated

  val packed: Boolean = isRepeated && (desc.getOptions.getPacked || file.syntax == Syntax.Proto3) && tpe.canBePacked
  val oneOf: Option[(OneOfNode, Int)] =
    if(desc.hasOneofIndex)
      val o = parent.oneOfs(desc.getOneofIndex)
      o.fields += this
      if(isProto3Optional)
        o.synthetic = true
        None
      else Some((o, o.fields.length))
    else None
  val flagIndex: Int =
    if(card == Cardinality.Optional && oneOf.isEmpty)
      parent.flagCount += 1
      parent.flagCount - 1
    else -1
  val set: String = if(isRepeated) s"set${upperName}List" else s"set$upperName"
  val get: String = if(isRepeated) s"get${upperName}List" else s"get$upperName"
  val has: String = s"has$upperName"
  val add: String = s"add$upperName"

  override def toString: String = s"field $pbName $number $card $tpe${pbDefaultValue.map(s => s" ('$s')").getOrElse("")}"

  override def dump(out: PrintStream, prefix: String): Unit =
    out.println(s"${prefix}$this")

  def emit(using TextOutputContext): Printed =
    if(isRepeated) emitRepeated else emitNormal

  private def emitNormal(using toc: TextOutputContext): Printed =
    val dflt = if(tpe.needsExplicitDefault) s" = ${tpe.defaultExpr}" else ""
    pm"""
        |private ${tpe.fieldType} $field$dflt;"""
    tpe match
      case tpe: Tpe.MessageT =>
        pm"""public ${tpe.fieldType} $get() {
            |  if($field == null) $field = new ${tpe.fieldType}();
            |  return $field;
            |}"""
      case _ =>
        pm"""public ${tpe.fieldType} $get() { return $field; }"""
    oneOf match
      case Some((o, i)) =>
        pm"""public ${parent.fqName} $set(${tpe.fieldType} value) { this.$field = value; this.${o.field} = $i;"""
        for f <- o.fields if f ne this do
          pm"""  this.${f.field} = ${f.tpe.defaultExpr};
              |  return this;"""
        pm"""}
            |public boolean $has() { return this.${o.field} == $i; }"""
      case _ if flagIndex >= 0 =>
        val ff = parent.flagFieldForIdx(flagIndex)
        val fb = parent.flagBitForIdx(flagIndex)
        pm"""public ${parent.fqName} $set(${tpe.fieldType} value) { this.$field = value; this.$ff |= $fb; return this; }
            |public boolean $has() { return (this.$ff & $fb) != 0; }"""
      case _ =>
        pm"""public ${parent.fqName} $set(${tpe.fieldType} value) { this.$field = value; return this; }
            |public boolean $has() { return ${tpe.isSet(field)}; }"""

  def javaParseExpr(receiver: String, rt: String, parseArgs: String): String = tpe match
    case tpe: Tpe.EnumT =>
      val m = if(isRepeated) add else set
      s"$receiver.$m(${tpe.fieldType}.valueOf($rt.${tpe.parseMethod}$parseArgs));"
    case tpe: Tpe.MessageT =>
      if(isRepeated)
        s"{ var in2 = in.limitedView($rt.${tpe.parseMethod}$parseArgs); $receiver.$add(${tpe.fieldType}.parseFrom(in2)); in2.close(); }"
      else
        s"{ var in2 = in.limitedView($rt.${tpe.parseMethod}$parseArgs); var m = $receiver.$get(); ${tpe.fieldType}.parseFrom(in2, m); in2.close(); $receiver.$set(m); }"
    case _ =>
      val m = if(isRepeated) add else set
      s"$receiver.$m($rt.${tpe.parseMethod}$parseArgs);"

  def javaWriteStatements(out: String, v: String)(using TextOutputContext): Printed = tpe match
    case tpe: Tpe.EnumT =>
      p"$RT.${tpe.writeMethod}($out, $v.number);"
    case tpe: Tpe.MessageT =>
      p"var out2 = $out.defer(); $v.writeTo(out2); $RT.${tpe.writeMethod}($out, out2.totalBytesWritten()); out2.close();"
    case _ =>
      p"$RT.${tpe.writeMethod}($out, $v);"

  private def emitRepeated(using toc: TextOutputContext): Printed =
    val (lt, init, initMut) = tpe.fieldType match
      case "int" =>
        val lt = classOf[IntList].getName
        (lt, s"$lt.EMPTY", s"if(this.$field == $lt.EMPTY) this.$set(new $lt());")
      case _ =>
        val lt = s"java.util.List<${tpe.boxedType}>"
        (lt, "java.util.List.of()", s"if((java.util.List)this.$field == java.util.List.of()) this.$set(new java.util.ArrayList<>());")
    pm"""
        |private $lt $field = $init;
        |private void ${field}_initMut() {
        |  $initMut
        |}
        |public void $add(${tpe.fieldType} value) { ${field}_initMut(); this.$field.add(value); }
        |public $lt $get() { return $field; }
        |public void $set($lt value) { this.$field = value; }
        |public boolean $has() { return !${field}.isEmpty(); }"""

  def emitParser(using toc: TextOutputContext): Printed =
    pm"case $tag -> ${javaParseExpr("base", RT, "(in)")}"
    if(tpe.canBePacked)
      if(tpe.fieldType == "int" && packed)
        pm"case ${packedTag} -> { var in2 = in.limitedView($RT.parseLen(in)); base.${field}_initMut(); while(in2.hasMore()) base.${field}.add($RT.parseInt32(in2)); in2.close(); }"
      else
        pm"case ${packedTag} -> { var in2 = in.limitedView($RT.parseLen(in)); while(in2.hasMore()) ${javaParseExpr("base", RT, "(in2)")}; in2.close(); }"
    Printed

  def emitWriter(using TextOutputContext): Printed =
    if(packed && tpe.fieldType == "int")
      pm"if(this.${has}()) { ${writeVarint(packedTag, "out")}; $RT.writePackedInt32(out, this.${field}); }"
    else if(packed)
      pm"""if(this.${has}()) {
          |  var it = this.${field}.iterator();
          |  ${writeVarint(packedTag, "out")};
          |  var out2 = out.defer();
          |  while(it.hasNext()) { var v = it.next(); ${javaWriteStatements("out2", "v")} }
          |  $RT.writeVarint(out, out2.totalBytesWritten());
          |  out2.close();
          |}"""
    else if(isRepeated)
      pm"if(this.${has}()) { var it = this.${field}.iterator(); while(it.hasNext()) { var v = it.next(); ${writeVarint(tag, "out")}; ${javaWriteStatements("out", "v")} }}"
    else
      pm"if(this.${has}()) { ${writeVarint(tag, "out")}; ${javaWriteStatements("out", s"this.${field}")} }"

  private def writeVarint(v: Long, bo: String): String =
    Util.encodeVarint(v).map { i => s"$bo.int8((byte)$i)" }.mkString("; ")

  def emitEquals(using TextOutputContext): Printed =
    if(tpe.hasPrimitiveEquality && !isRepeated)
      pm"if(this.$field != m.$field) return false;"
    else if(oneOf.isDefined || flagIndex >= 0)
      pm"if(this.$has() && !this.$field.equals(m.$field)) return false;"
    else //TODO do we ever need this?
      pm"if(!this.$field.equals(m.$field)) return false;"
    

enum Cardinality:
  case Optional, Implicit, Repeated, Required
