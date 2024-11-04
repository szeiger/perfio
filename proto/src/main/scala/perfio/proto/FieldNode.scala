package perfio.proto

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.{Label, Type}
import perfio.TextOutput
import perfio.proto.runtime.Runtime

import java.io.PrintStream

class FieldNode(val desc: FieldDescriptorProto, val parent: MessageNode) extends Node {
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
  val cardinality: Cardinality = desc.getLabel match {
    case Label.LABEL_OPTIONAL =>
      if(file.syntax == Syntax.Proto3 && !(isProto3Optional || desc.getType == Type.TYPE_MESSAGE)) Cardinality.Implicit
      else Cardinality.Optional
    case Label.LABEL_REPEATED => Cardinality.Repeated
    case Label.LABEL_REQUIRED => Cardinality.Required
    case _ => Cardinality.Implicit
  }
  val packed: Boolean = cardinality == Cardinality.Repeated && (desc.getOptions.getPacked || file.syntax == Syntax.Proto3) && tpe.canBePacked
  val oneOf: Option[(OneOfNode, Int)] =
    if(desc.hasOneofIndex) {
      val o = parent.oneOfs(desc.getOneofIndex)
      o.fields += this
      if(isProto3Optional) {
        o.synthetic = true
        None
      } else Some((o, o.fields.length))
    } else None
  val flagIndex: Int = if(cardinality == Cardinality.Optional && oneOf.isEmpty) {
    parent.flagCount += 1
    parent.flagCount - 1
  } else -1
  val defaultValue: Option[String] = if(desc.hasDefaultValue) Some(desc.getDefaultValue) else None
  def repeated: Boolean = cardinality == Cardinality.Repeated
  val javaSetter: String = if(repeated) s"set${javaUpperName}List" else s"set$javaUpperName"
  val javaGetter: String = if(repeated) s"get${javaUpperName}List" else s"get$javaUpperName"
  val javaHazzer: String = s"has$javaUpperName"
  val javaAdder: String = s"add$javaUpperName"

  override def toString: String = s"field $name $number $cardinality $tpe${defaultValue.map(s => s" ('$s')").getOrElse("")}"

  override def dump(out: PrintStream, prefix: String): Unit = {
    out.println(s"${prefix}$this")
  }

  def emit(to: TextOutput, prefix: String): Unit = {
    if(cardinality == Cardinality.Repeated) emitRepeated(to, prefix)
    else emitNormal(to, prefix)
  }

  private def emitNormal(to: TextOutput, prefix: String): Unit = {
    val dflt = if(tpe.javaNeedsExplicitDefault)s" = ${tpe.javaDefaultValue}" else ""
    to.println(s"${prefix}private ${tpe.javaType} $javaFieldName$dflt;")
    tpe match {
      case tpe: Tpe.MessageT =>
        to.println(s"${prefix}public ${tpe.javaType} $javaGetter() {")
        to.println(s"${prefix}  if($javaFieldName == null) $javaFieldName = new ${tpe.javaType}();")
        to.println(s"${prefix}  return $javaFieldName;")
        to.println(s"${prefix}}")
      case _ =>
        to.println(s"${prefix}public ${tpe.javaType} $javaGetter() { return $javaFieldName; }")
    }
    oneOf match {
      case Some((o, i)) =>
        to.println(s"${prefix}public ${parent.fqJavaName} $javaSetter(${tpe.javaType} value) { this.$javaFieldName = value; this.${o.javaFieldName} = $i;")
        for(f <- o.fields if f ne this) {
          to.println(s"${prefix}  this.${f.javaFieldName} = ${f.tpe.javaDefaultValue};")
          to.println(s"${prefix}  return this;")
        }
        to.println(s"${prefix}}")
        to.println(s"${prefix}public boolean $javaHazzer() { return this.${o.javaFieldName} == $i; }")
      case _ if flagIndex >= 0 =>
        val ff = parent.flagFieldForIdx(flagIndex)
        val fb = parent.flagBitForIdx(flagIndex)
        to.println(s"${prefix}public ${parent.fqJavaName} $javaSetter(${tpe.javaType} value) { this.$javaFieldName = value; this.$ff |= $fb; return this; }")
        to.println(s"${prefix}public boolean $javaHazzer() { return (this.$ff & $fb) != 0; }")
      case _ =>
        to.println(s"${prefix}public ${parent.fqJavaName} $javaSetter(${tpe.javaType} value) { this.$javaFieldName = value; return this; }")
        to.println(s"${prefix}public boolean $javaHazzer() { return ${tpe.javaIsSet(javaFieldName)}; }")
    }
  }

  def javaParseExpr(receiver: String, rt: String, parseArgs: String): String = tpe match {
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
  }

  def javaWriteStatements(rt: String, out: String, v: String): String = tpe match {
    case tpe: Tpe.EnumT =>
      s"$rt.${tpe.writeMethod}($out, $v.number);"
    case tpe: Tpe.MessageT =>
      s"var out2 = $out.defer(); $v.writeTo(out2); $rt.${tpe.writeMethod}($out, out2.totalBytesWritten()); out2.close();"
    case _ =>
      s"$rt.${tpe.writeMethod}($out, $v);"
  }

  private def emitRepeated(to: TextOutput, prefix: String): Unit = {
    to.println(s"${prefix}private java.util.List<${tpe.javaBoxedType}> $javaFieldName = java.util.List.of();")
    to.println(s"${prefix}public java.util.List<${tpe.javaBoxedType}> $javaGetter() { return $javaFieldName; }")
    to.println(s"${prefix}public void $javaSetter(java.util.List<${tpe.javaBoxedType}> value) { this.$javaFieldName = value; }")
    to.println(s"${prefix}public void $javaAdder(${tpe.javaType} value) {")
    to.println(s"${prefix}  if(this.$javaFieldName == null || (java.util.List)this.$javaFieldName == java.util.List.of()) this.$javaSetter(new java.util.ArrayList<>());")
    to.println(s"${prefix}  this.$javaFieldName.add(value);")
    to.println(s"${prefix}}")
    to.println(s"${prefix}public boolean $javaHazzer() { return !${javaFieldName}.isEmpty(); }")
  }
}


sealed abstract class Cardinality
object Cardinality {
  case object Optional extends Cardinality
  case object Implicit extends Cardinality
  case object Repeated extends Cardinality
  case object Required extends Cardinality
}
