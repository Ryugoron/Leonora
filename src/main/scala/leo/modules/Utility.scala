package leo
package modules

import java.io.{PrintWriter, StringWriter}

import leo.datastructures._
import leo.datastructures.impl.Signature
import leo.modules.output._

import scala.collection.immutable.HashSet

/**
 * Stuff to do smth.
 *
 * @author Max Wisniewski
 * @since 12/1/14
 */
object Utility {


  private final def singleTermToClause(t: Term, role: Role): Clause = {
    Clause.mkClause(Seq(Literal.mkPos(t, LitTrue)), roleToClauseOrigin(role))
  }
  private final def roleToClauseOrigin(role: Role): ClauseOrigin = role match {
    case Role_Conjecture => FromConjecture
    case Role_NegConjecture => FromConjecture
    case _ => FromAxiom
  }


  def printSignature(): Unit = {
    import leo.datastructures.IsSignature.{lexStatus,multStatus}
    val s = Signature.get
    println("Name | Id | (Type) | (Def)")
    (s.allConstants).foreach { case c => {
      val c1 = s(c)
      print(c1.name + " | ")
      print(c1.key + " |")
      if (c1.status == lexStatus) print("lex " + " | ")
      if (c1.status == multStatus) print("mult" + " | ")
      c1.ty foreach { case ty => print(ty.pretty + " | ")}
      c1.defn foreach { case defn => print(defn.pretty)}
      println()
    }
    }
  }

  def printUserDefinedSignature(): Unit = {
    println(userDefinedSignatureAsString)
  }
  def userDefinedSignatureAsString: String = {
    import leo.datastructures.IsSignature.{lexStatus,multStatus}
    val s = Signature.get
    val sb = new StringBuilder()
    (s.allUserConstants).foreach { case c => {
      val c1 = s(c)
      sb.append(c1.name + " | ")
      sb.append(c1.key + " | ")
      if (c1.status == lexStatus) sb.append("lex " + " | ")
      if (c1.status == multStatus) sb.append("mult" + " | ")
      c1.ty foreach { case ty => sb.append(ty.pretty + " | ")}
      c1.defn foreach { case defn => sb.append(defn.pretty)}
      sb.append("\n")
    }
    }
    sb.toString()
  }

  def printDerivation(f : FormulaStore) : Unit = Out.output(derivationString(new HashSet[Int](), 0, f, new StringBuilder()).toString())

  private def derivationString(origin: Set[Int], indent : Int, f: FormulaStore, sb : StringBuilder) : StringBuilder = {
    f.annotation match {
      case FromFile(_, _) => sb.append(downList(origin, indent)).append(mkTPTP(f)).append("\n")
      case InferredFrom(_, fs) => fs.foldRight(sb.append(downList(origin, indent)).append(mkTPTP(f)).append("\n")){case (fs, sbu) => derivationString(origin.+(indent), indent+1,fs,sbu)}
      case _ => sb.append(downList(origin, indent)).append(mkTPTP(f)).append("\n")
    }
//    f.origin.foldRight(sb.append(downList(origin, indent)).append(mkTPTP(f)).append("\t"*6+"("+f.reason+")").append("\n")){case (fs, sbu) => derivationString(origin.+(indent), indent+1,fs,sbu)}
  }

  def printProof(f : FormulaStore) : Unit = {

    var sf : Set[FormulaStore] = new HashSet[FormulaStore]
    var proof : Seq[String] = Seq()

    def derivationProof(f: FormulaStore)
    {
      if (!sf.contains(f)) {
        sf = sf + f
        f.annotation match {
          case InferredFrom(_, fs) =>
            fs.foreach(derivationProof(_))
            proof = mkTPTP(f) +: proof
          case _ =>
            proof = mkTPTP(f) +: proof
        }
      }
    }

    derivationProof(f)
    Out.output(proof.reverse.mkString("\n"))
  }

  private def mkTPTP(f : FormulaStore) : String = {
    try{
      ToTPTP.withAnnotation(f).output
    } catch {
      case e : Throwable => f.pretty
    }
  }

  private def downList(origin: Set[Int], indent : Int) : String = {
    val m = if(origin.isEmpty) 0 else origin.max
    List.range(0, indent).map { x => origin.contains(x) match {
      case true if x < m => " | "
      case true => " |-"
      case false if m < x => "---"
      case false => "   "
    }}.foldRight(""){(a,b) => a+b}
  }


  def stackTraceAsString(e: Throwable): String = {
    val sw = new StringWriter()
    e.printStackTrace(new PrintWriter(sw))
    sw.toString
  }

}

class SZSException(val status : StatusSZS, message : String = "", val debugMessage: String = "", cause : Throwable = null) extends RuntimeException(message, cause)

case class SZSOutput(status : StatusSZS, problem: String, furtherInfo: String = "") extends Output {
  override def output: String = if (furtherInfo == "") {
    s"% SZS status ${status.output} for $problem"
  } else {
    s"% SZS status ${status.output} for $problem : $furtherInfo"
  }
}
