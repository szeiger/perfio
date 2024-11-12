package perfio.proto

import perfio.protoapi.DescriptorProtos.FieldDescriptorProto
import perfio.protoapi.DescriptorProtos.FieldDescriptorProto.{Label, Type}
import perfio.TextOutput
import perfio.proto.runtime.{IntList, Runtime}
import perfio.scalaapi.*

import java.io.PrintStream

class FieldNode(val desc: FieldDescriptorProto, val parent: MessageNode) extends Node:
  def file: FileNode = parent.file
  val name: String = desc.getName
  val javaLowerName: String = Util.mkLowerJavaName(name)
  val javaUpperName: String = Util.mkUpperJavaName(name)
  val javaFieldName: String = s"_$javaLowerName"
  val number: Int = desc.getNumber
  val tpe: Tpe = Tpe.ofProtoType(desc.getType, desc.getTypeName, parent.root)
  def tag: Int = (number << 3) | tpe.wireType
  def packedTag: Int = (number << 3) | Runtime.LEN
  val isProto3Optional: Boolean = desc.getProto3Optional

  val cardinality: Cardinality = desc.getLabel match
    case Label.LABEL_OPTIONAL =>
      if(file.syntax == Syntax.Proto3 && !(isProto3Optional || desc.getType == Type.TYPE_MESSAGE)) Cardinality.Implicit
      else Cardinality.Optional
    case Label.LABEL_REPEATED => Cardinality.Repeated
    case Label.LABEL_REQUIRED => Cardinality.Required
    case _ => Cardinality.Implicit

  val packed: Boolean = cardinality == Cardinality.Repeated && (desc.getOptions.getPacked || file.syntax == Syntax.Proto3) && tpe.canBePacked
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
    if(cardinality == Cardinality.Optional && oneOf.isEmpty)
      parent.flagCount += 1
      parent.flagCount - 1
    else -1
  val defaultValue: Option[String] = if(desc.hasDefaultValue) Some(desc.getDefaultValue) else None
  def repeated: Boolean = cardinality == Cardinality.Repeated
  val javaSetter: String = if(repeated) s"set${javaUpperName}List" else s"set$javaUpperName"
  val javaGetter: String = if(repeated) s"get${javaUpperName}List" else s"get$javaUpperName"
  val javaHazzer: String = s"has$javaUpperName"
  val javaAdder: String = s"add$javaUpperName"

  override def toString: String = s"field $name $number $cardinality $tpe${defaultValue.map(s => s" ('$s')").getOrElse("")}"

  override def dump(out: PrintStream, prefix: String): Unit =
    out.println(s"${prefix}$this")

  def emit(using TextOutputContext): Printed =
    if(cardinality == Cardinality.Repeated) emitRepeated else emitNormal

  private def emitNormal(using toc: TextOutputContext): Printed =
    val dflt = if(tpe.javaNeedsExplicitDefault) s" = ${tpe.javaDefaultValue}" else ""
    pm"""private ${tpe.javaType} $javaFieldName$dflt;"""
    tpe match
      case tpe: Tpe.MessageT =>
        pm"""public ${tpe.javaType} $javaGetter() {
            |  if($javaFieldName == null) $javaFieldName = new ${tpe.javaType}();
            |  return $javaFieldName;
            |}"""
      case _ =>
        pm"""public ${tpe.javaType} $javaGetter() { return $javaFieldName; }"""
    oneOf match
      case Some((o, i)) =>
        pm"""public ${parent.fqJavaName} $javaSetter(${tpe.javaType} value) { this.$javaFieldName = value; this.${o.javaFieldName} = $i;"""
        for f <- o.fields if f ne this do
          pm"""  this.${f.javaFieldName} = ${f.tpe.javaDefaultValue};
              |  return this;"""
        pm"""}
            |public boolean $javaHazzer() { return this.${o.javaFieldName} == $i; }"""
      case _ if flagIndex >= 0 =>
        val ff = parent.flagFieldForIdx(flagIndex)
        val fb = parent.flagBitForIdx(flagIndex)
        pm"""public ${parent.fqJavaName} $javaSetter(${tpe.javaType} value) { this.$javaFieldName = value; this.$ff |= $fb; return this; }
            |public boolean $javaHazzer() { return (this.$ff & $fb) != 0; }"""
      case _ =>
        pm"""public ${parent.fqJavaName} $javaSetter(${tpe.javaType} value) { this.$javaFieldName = value; return this; }
            |public boolean $javaHazzer() { return ${tpe.javaIsSet(javaFieldName)}; }"""

  def javaParseExpr(receiver: String, rt: String, parseArgs: String): String = tpe match
    case tpe: Tpe.EnumT =>
      val m = if(cardinality == Cardinality.Repeated) javaAdder else javaSetter
      s"$receiver.$m(${tpe.javaType}.valueOf($rt.${tpe.parserMethod}$parseArgs));"
    case tpe: Tpe.MessageT =>
      if(cardinality == Cardinality.Repeated)
        s"{ var in2 = in.delimitedView($rt.${tpe.parserMethod}$parseArgs); $receiver.$javaAdder(${tpe.javaType}.parseFrom(in2)); in2.close(); }"
      else
        s"{ var in2 = in.delimitedView($rt.${tpe.parserMethod}$parseArgs); var m = $receiver.$javaGetter(); ${tpe.javaType}.parseFrom(in2, m); in2.close(); $receiver.$javaSetter(m); }"
    case _ =>
      val m = if(cardinality == Cardinality.Repeated) javaAdder else javaSetter
      s"$receiver.$m($rt.${tpe.parserMethod}$parseArgs);"

  def javaWriteStatements(rt: String, out: String, v: String)(using TextOutputContext): Printed = tpe match
    case tpe: Tpe.EnumT =>
      p"$rt.${tpe.writeMethod}($out, $v.number);"
    case tpe: Tpe.MessageT =>
      p"var out2 = $out.defer(); $v.writeTo(out2); $rt.${tpe.writeMethod}($out, out2.totalBytesWritten()); out2.close();"
    case _ =>
      p"$rt.${tpe.writeMethod}($out, $v);"

  private def emitRepeated(using toc: TextOutputContext): Printed =
    val lt: String = tpe.javaType match
      case "int" =>
        val lt = classOf[IntList].getName
        pm"""private $lt $javaFieldName = $lt.EMPTY;
            |private void ${javaFieldName}_initMut() {
            |  if(this.$javaFieldName == $lt.EMPTY) this.$javaSetter(new $lt());
            |}"""
        lt
      case _ =>
        val lt = s"java.util.List<${tpe.javaBoxedType}>"
        pm"""private $lt $javaFieldName = java.util.List.of();
            |private void ${javaFieldName}_initMut() {
            |  if((java.util.List)this.$javaFieldName == java.util.List.of()) this.$javaSetter(new java.util.ArrayList<>());
            |}"""
        lt
    pm"""public void $javaAdder(${tpe.javaType} value) { ${javaFieldName}_initMut(); this.$javaFieldName.add(value); }
        |public $lt $javaGetter() { return $javaFieldName; }
        |public void $javaSetter($lt value) { this.$javaFieldName = value; }
        |public boolean $javaHazzer() { return !${javaFieldName}.isEmpty(); }"""


enum Cardinality:
  case Optional, Implicit, Repeated, Required
