package perfio.scalaapi

import perfio.TextOutput

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer
import scala.quoted.*

class TextOutputContext(val to: TextOutput):
  var prefix: String = ""
  inline def indented[T](inline n: Int)(inline f: TextOutputContext ?=> T): T = {
    val p = prefix
    prefix += " " * (n*2)
    val r = f(using this)
    prefix = p
    r
  }
  inline def indented1[T](inline f: TextOutputContext ?=> T): T = indented(1)(f)
  inline def indented2[T](inline f: TextOutputContext ?=> T): T = indented(2)(f)
  inline def indented3[T](inline f: TextOutputContext ?=> T): T = indented(3)(f)

sealed trait Printed
object Printed extends Printed

extension (inline sc: StringContext)
  inline def p(inline args: Any*)(using inline toc: TextOutputContext): Printed =
    ${ TextOutputMacros.macroImpl('{toc}, '{sc}, '{args}, false) }
  inline def pm(inline args: Any*)(using inline toc: TextOutputContext): Printed =
    ${ TextOutputMacros.macroImpl('{toc}, '{sc}, '{args}, true) }

object TextOutputMacros:
  def macroImpl(toc: Expr[TextOutputContext], sc: Expr[StringContext], args: Expr[Seq[Any]], multiLine: Boolean)(using quotes: Quotes): Expr[Printed] =
    (sc, args) match
      case ('{ StringContext(${Varargs(Exprs(vargs))}*) }, Varargs(vs)) =>
        val parts = ArrayBuffer.empty[Part]
        if(multiLine) collect(parts, decode(vargs.head), Some(Part.FirstPrefix), Some(Part.Prefix)) else addPart(parts, Part.Str(decode(vargs.head)))
        for (v, p) <- vs.zip(vargs.tail) do
          val pv = v match
            case '{ ${Expr(v)}: Int     } => Part.Str(String.valueOf(v))
            case '{ ${Expr(v)}: Long    } => Part.Str(String.valueOf(v))
            case '{ ${Expr(v)}: Boolean } => Part.Str(String.valueOf(v))
            case '{ ${Expr(v)}: Float   } => Part.Str(String.valueOf(v))
            case '{ ${Expr(v)}: Double  } => Part.Str(String.valueOf(v))
            case '{ ${Expr(v)}: String  } => Part.Str(String.valueOf(v))
            case '{ ${Expr(v)}: Char    } => Part.Str(String.valueOf(v))
            case _ => Part.Arg(v)
          addPart(parts, pv)
          if(multiLine) collect(parts, decode(p), None, Some(Part.Prefix)) else addPart(parts, Part.Str(decode(p)))
        if(multiLine) addPart(parts, Part.LF)
        def blk(toc: Expr[TextOutputContext], to: Expr[TextOutput], pr: Expr[String]): Expr[?] =
          val b = ArrayBuffer.empty[Expr[?]]
          var i = 0
          while i < parts.length do
            val p = parts(i)
            if(p == Part.LF) b += '{ $to.println() }
            else
              val lf = i+1 < parts.length && parts(i+1) == Part.LF
              if(lf) i += 1
              p match
                case Part.Str(s) =>
                  s.length match
                    case 0 => if(lf) b += '{ $to.println() }
                    case 1 => b += (if(lf) '{ $to.println(${Expr(s.charAt(0))}) } else '{ $to.print(${Expr(s.charAt(0))}) })
                    case _ => b += (if(lf) '{ $to.println(${Expr(s)}) } else '{ $to.print(${Expr(s)}) })
                case Part.Arg('{ ${v}: Printed }) =>
                  b += v
                  if(lf) i -= 1
                case Part.Arg('{ ${v}: Null        }) => b += (if(lf) '{ $to.println(null: AnyRef) } else '{ $to.print(null: AnyRef) })
                case Part.Arg('{ ${v}: Int         }) => b += (if(lf) '{ $to.println(${v}) } else '{ $to.print(${v}) })
                case Part.Arg('{ ${v}: Long        }) => b += (if(lf) '{ $to.println(${v}) } else '{ $to.print(${v}) })
                case Part.Arg('{ ${v}: Boolean     }) => b += (if(lf) '{ $to.println(${v}) } else '{ $to.print(${v}) })
                case Part.Arg('{ ${v}: Float       }) => b += (if(lf) '{ $to.println(${v}) } else '{ $to.print(${v}) })
                case Part.Arg('{ ${v}: Double      }) => b += (if(lf) '{ $to.println(${v}) } else '{ $to.print(${v}) })
                case Part.Arg('{ ${v}: String      }) => b += (if(lf) '{ $to.println(${v}) } else '{ $to.print(${v}) })
                case Part.Arg('{ ${v}: Char        }) => b += (if(lf) '{ $to.println(${v}) } else '{ $to.print(${v}) })
                case Part.Arg('{ ${v}: Array[Char] }) => b += (if(lf) '{ $to.println(${v}) } else '{ $to.print(${v}) })
                //case Part.Arg('{ ${v}              }) => b += (if(lf) '{ $to.println(${v}) } else '{ $to.print(${v}) })
                case Part.Arg('{ $v: tpe }) =>
                  quotes.reflect.report.errorAndAbort(
                    s"""Unsupported type '${Type.show[tpe]}' of spliced expression '${v.show}'.
                       |Supported types are String, Char, Int, Long, Boolean, Null, Float, Double, Array[Char], Printed""".stripMargin)
                case Part.Prefix | Part.FirstPrefix => b += (if(lf) '{ $to.println(${pr}) } else '{ $to.print(${pr}) })
                case _ => if(lf) b += '{ $to.println() }
            i += 1
          if(b.length == 1) b.head
          else Expr.block(b.init.toList, b.last)
        '{ val toc2 = $toc; ${blk('{toc2}, '{toc2.to}, '{toc2.prefix})}; Printed }
      case _ =>
        quotes.reflect.report.errorAndAbort(
          """TextOutput p and pm interpolators require standard interpolation syntax.
            |To avoid performance surprises there is no fallback to a dynamic implementation.""".stripMargin)

  private def addPart(parts: ArrayBuffer[Part], p: Part): Unit =
    (parts.lastOption, p) match
      case (Some(Part.Str(s1)), Part.Str(s2)) => parts.dropRightInPlace(1) += Part.Str(s1 + s2)
      case _ => parts += p

  private def decode(s: String)(using Quotes): String =
    val b = new StringBuilder()
    var i = 0
    while i < s.length do
      s.charAt(i) match
        case '\\' if i+1 < s.length =>
          i += 1
          s.charAt(i) match
            case 'n' => b.append('\n')
            case 'r' => b.append('\r')
            case '"' => b.append('"')
            case '\'' => b.append('\'')
            case '\\' => b.append('\\')
            case c => quotes.reflect.report.errorAndAbort(s"""Invalid escape \\$c - Only \\r \\n \\" \\' \\\\ are supported.""")
        case c => b.append(c)
      i += 1
    b.result()

  private def collect(b: ArrayBuffer[Part], s: String, firstPrefix: Option[Part], prefix: Option[Part]): Unit =
    var i, start = 0
    var first = true
    def strip(s: String): String =
      val p = s.indexOf('|')
      if(p >= 0 && s.substring(0, p).isBlank) s.substring(p+1) else s
    def emit(s: String): Unit =
      val pre = if(first) firstPrefix else prefix
      pre.foreach(addPart(b, _))
      val s2 = if(!first) strip(s) else s
      if(s2.nonEmpty) addPart(b, Part.Str(s2))
    while i < s.length do
      s.charAt(i) match
        case '\n' =>
          val j = if(i > 0 && s.charAt(i-1) == '\r') i-1 else i
          emit(s.substring(start, j))
          addPart(b, Part.LF)
          start = i+1
          first = false
        case _ =>
      i += 1
    if(i > start) emit(s.substring(start, i))

  private enum Part:
    case LF, Prefix, FirstPrefix
    case Str(s: String)
    case Arg(e: Expr[Any])
