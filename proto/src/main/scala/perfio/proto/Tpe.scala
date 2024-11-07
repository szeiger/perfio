package perfio.proto

import perfio.protoapi.DescriptorProtos.FieldDescriptorProto.Type
import perfio.proto.runtime.Runtime

import scala.reflect.ClassTag

sealed abstract class Tpe {
  def javaType: String
  def javaDefaultValue: String
  def javaNeedsExplicitDefault: Boolean
  def javaIsSet(v: String): String
  def javaBoxedType: String = javaType
  def parserMethod: String
  def writeMethod: String
  def wireType: Int
  def canBePacked: Boolean = true
  def javaHasPrimitiveEquality: Boolean = true
}

sealed abstract class SimpleTpe[T](boxed: Class[_], val parserMethod: String, val writeMethod: String, val wireType: Int,
  val javaDefaultValue: String, val javaNeedsExplicitDefault: Boolean = false)(implicit ct: ClassTag[T]) extends Tpe {
  def javaType = ct.runtimeClass.getCanonicalName
  def javaIsSet(v: String): String = s"$v != 0"
  override def javaBoxedType: String = boxed.getCanonicalName
}

object Tpe {
  case object Int64T    extends SimpleTpe[Long   ](classOf[java.lang.Long   ], "parseInt64",   "writeInt64",   Runtime.VARINT, "0L")
  case object UInt64T   extends SimpleTpe[Long   ](classOf[java.lang.Long   ], "parseInt64",   "writeInt64",   Runtime.VARINT, "0L")
  case object Int32T    extends SimpleTpe[Int    ](classOf[java.lang.Integer], "parseInt32",   "writeInt32",   Runtime.VARINT, "0")
  case object Fixed64T  extends SimpleTpe[Long   ](classOf[java.lang.Long   ], "parseFixed64", "writeFixed64", Runtime.I64,    "0L")
  case object Fixed32T  extends SimpleTpe[Int    ](classOf[java.lang.Integer], "parseFixed32", "writeFixed32", Runtime.I32,    "0")
  case object UInt32T   extends SimpleTpe[Int    ](classOf[java.lang.Integer], "parseInt32",   "writeInt32",   Runtime.VARINT, "0")
  case object SFixed32T extends SimpleTpe[Int    ](classOf[java.lang.Integer], "parseFixed32", "writeFixed32", Runtime.I32,    "0")
  case object SFixed64T extends SimpleTpe[Long   ](classOf[java.lang.Long   ], "parseFixed64", "writeFixed64", Runtime.I64,    "0L")
  case object SInt32T   extends SimpleTpe[Int    ](classOf[java.lang.Integer], "parseSInt32",  "writeSInt32",  Runtime.VARINT, "0")
  case object SInt64T   extends SimpleTpe[Long   ](classOf[java.lang.Long   ], "parseSInt64",  "writeSInt64",  Runtime.VARINT, "0L")
  case object DoubleT   extends SimpleTpe[Double ](classOf[java.lang.Double ], "parseDouble",  "writeDouble",  Runtime.I64,    "0.0d")
  case object FloatT    extends SimpleTpe[Float  ](classOf[java.lang.Float  ], "parseFloat",   "writeFloat",   Runtime.I32,    "0.0")
  case object BoolT     extends SimpleTpe[Boolean](classOf[java.lang.Boolean], "parseBoolean", "writeBoolean", Runtime.VARINT, "false") {
    override def javaIsSet(v: String): String = v
  }
  case object BytesT extends SimpleTpe[Array[Byte]](classOf[Array[Byte]], "parseBytes", "writeBytes", Runtime.LEN, "new byte[0]", true) {
    override def javaIsSet(v: String): String = s"${v}.length != 0"
    override def canBePacked: Boolean = false
    override def javaHasPrimitiveEquality: Boolean = false
  }
  case object StringT extends SimpleTpe[String](classOf[String], "parseString", "writeString", Runtime.LEN, "\"\"", true) {
    override def javaIsSet(v: String): String = s"!${v}.isEmpty()"
    override def canBePacked: Boolean = false
    override def javaHasPrimitiveEquality: Boolean = false
  }
  case class GroupT(name: String) extends Tpe {
    def javaType = ???
    def javaDefaultValue: String = ???
    def javaNeedsExplicitDefault: Boolean = ???
    def javaIsSet(v: String): String = ???
    def parserMethod: String = ???
    def writeMethod: String = ???
    def wireType: Int = ???
    override def canBePacked: Boolean = ???
    override def javaHasPrimitiveEquality: Boolean = ???
  }
  case class MessageT(name: String)(root: RootNode) extends Tpe {
    lazy val javaType = root.allMessages(name).fqJavaName
    def javaDefaultValue: String = "null"
    def javaNeedsExplicitDefault = false // special handling  in FieldNode
    def javaIsSet(v: String): String = s"$v != null"
    def parserMethod: String = "parseLen" // special handling in FieldNode
    def writeMethod: String = "writeLen" // special handling in FieldNode
    def wireType: Int = Runtime.LEN
    override def canBePacked: Boolean = false
    override def javaHasPrimitiveEquality: Boolean = false
  }
  case class EnumT(name: String)(root: RootNode) extends Tpe {
    lazy val en = root.allEnums(name)
    lazy val javaType = en.fqJavaName
    def javaDefaultValue: String = s"${javaType}.${en.values.head._1}"
    def javaNeedsExplicitDefault = true
    def javaIsSet(v: String): String = s"$v.number != 0"
    def parserMethod: String = "parseInt32" // special handling in FieldNode
    def writeMethod: String = "writeInt32" // special handling in FieldNode
    def wireType: Int = Runtime.VARINT
    override def canBePacked: Boolean = false //TODO is this right? the spec says so
  }

  def ofProtoType(t: Type, n: String, root: RootNode): Tpe = t match {
    case Type.TYPE_DOUBLE => DoubleT
    case Type.TYPE_FLOAT => FloatT
    case Type.TYPE_INT64 => Int64T
    case Type.TYPE_UINT64 => UInt64T
    case Type.TYPE_INT32 => Int32T
    case Type.TYPE_FIXED64 => Fixed64T
    case Type.TYPE_FIXED32 => Fixed32T
    case Type.TYPE_BOOL => BoolT
    case Type.TYPE_STRING => StringT
    case Type.TYPE_GROUP => GroupT(n)
    case Type.TYPE_MESSAGE => MessageT(n)(root)
    case Type.TYPE_BYTES => BytesT
    case Type.TYPE_UINT32 => UInt32T
    case Type.TYPE_ENUM => EnumT(n)(root)
    case Type.TYPE_SFIXED32 => SFixed32T
    case Type.TYPE_SFIXED64 => SFixed64T
    case Type.TYPE_SINT32 => SInt32T
    case Type.TYPE_SINT64 => SInt64T
  }
}
