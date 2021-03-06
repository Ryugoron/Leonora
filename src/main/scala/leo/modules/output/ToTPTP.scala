package leo.modules.output

import leo.datastructures._
import leo.datastructures.impl.Signature
import Term._
import leo.datastructures.Type._
import scala.annotation.tailrec


/**
 * Translation module that takes internal terms or types and translates them
 * to a TPTP representation (in THF format).
 * Translation can be done directly into a string by method `output`
 * or indirect into a `Output` object by the apply method.
 *
 * @see [[Term]], [[FormulaStore]]
 * @author Alexander Steen
 * @since 07.11.2014
 */
object ToTPTP extends Function1[FormulaStore, Output] with Function3[String, Clause, Role, Output] {

  /** Return an `Output` object that contains the TPTP representation of the given
    * `FormulaStore`.*/
  def apply(f: FormulaStore): Output = apply(f.name, f.clause, f.role)


  /** Return an `Output` object that contains the TPTP representation of the given
    * information triple.*/
  def apply(name: String, c: Clause, role: Role): Output = new Output {
    val t : Term = if(role == Role_Definition) definitionToTerm(c) else c.term
    def output = toTPTP(name, t, role)
  }
  private def definitionToTerm(c : Clause) : Term = {
    if(c.lits.size != 1) return c.term
    val l = c.lits.head
    if(!l.polarity) return c.term
    if(!l.left.isAtom) {
      ===(l.right, l.left)
    } else {
      ===(l.left, l.right)
    }
  }

  def apply(name: String, t: Term, role: Role): Output = new Output {
    def output = toTPTP(name, t, role)
  }

  def apply(formulas : Seq[FormulaStore], withDefinitions : Boolean = true) : Seq[Output] = {
    var out: List[Output] = List.empty[Output]
    var defn : List[Output] = List.empty[Output]
    Signature.get.allUserConstants foreach { k =>
      val (t,d) = constantToTPTP(k)
      out = t :: out
      if(withDefinitions) d.fold(()){d1 => defn = d1 :: defn}
    }
    if(withDefinitions) out = defn ++ out           // Important, since the definition has to be made after the type declaration
    formulas foreach {formula =>
      out = ToTPTP(formula) :: out}
    out.reverse
  }

  def apply(formulas : Set[FormulaStore]) : Seq[Output] = apply(formulas.toSeq)

  def withAnnotation(f: FormulaStore): Output = new Output {
    def output = toTPTP(f.name, f.clause.term, f.role, f.annotation)
  }

  /**
   * Returns a TPTP conform definition of a constant. The type is always
   * returned and, if existant, also the definition.
   *
   * @param k is the Key of the constant
   * @return A definition for the constant.
   */
  private def constantToTPTP(k : Signature#Key) : (Output, Option[Output]) = {
    val constant = Signature.get.apply(k)
    (constant.defn) match {
      case Some(defn) => (ToTPTP(s"${constant.name}_type", k), Some(ToTPTP(s"${constant.name}_def", ===(mkAtom(k),defn), Role_Definition)))
      case (None) => (ToTPTP(s"${constant.name}_type", k), None)
    }
  }

  /**
   * Returns an Output suitable in a type definition.
   */
  def apply(name : String, key : Signature#Key) : Output = new Output {
    def output : String = {
      val constant = Signature.get.apply(key)
      val cname = {if (constant.name.startsWith("'") && constant.name.endsWith("'")) {
        "'" + constant.name.substring(1, constant.name.length-1).replaceAll("\\\\", """\\\\""").replaceAll("\\'", """\\'""") + "'"
      } else {
        constant.name
      }}
      if(constant.ty.isEmpty)
        return (s"thf($name, ${Role_Type.pretty}, ${cname}: "+"$tType).")
      else
        return s"thf(${name}, ${Role_Type.pretty}, ${cname}: ${toTPTP(constant._ty)})."
    }
  }

  /** Translate the `FormulaStore` into a TPTP String in THF format. */
  def output(f: FormulaStore) = toTPTP(f.name, f.clause.term, f.role)
  /** Translate the term information triple into a TPTP String. */
  def output(name: String, t: Clause, role: Role) = toTPTP(name, t.term, role)

  // Extra output function for types only (not sure if needed somewhere)
  /** Return an `Output` object that contains the TPTP representation of the given type.*/
  def apply(ty: Type): Output = new Output {
    def output = toTPTP(ty)
  }
  /** Translate the type to a TPTP String in THF format. */
  def output(ty: Type) = toTPTP(ty)

  ///////////////////////////////
  // Translation of other data structures
  ///////////////////////////////

  def apply(subst: Subst): Output = new Output {
    override def output: String = {
      if (subst.length == 0) {
        ""
      } else {
        val sb = new StringBuilder
        var i = 1
        var max = subst.length
        while (i < max) {
          val erg = subst.substBndIdx(i)
          sb.append(s"bind(${i}, $$thf(${erg.pretty})),")
          i = i+1
        }
        sb.append(s"bind(${max}, $$thf(${subst.substBndIdx(max).pretty}))")
        sb.toString()
      }
    }
  }

  ///////////////////////////////
  // Translation of THF formula
  ///////////////////////////////
  // TODO: Fixme write translation from clause
  private def toTPTP(name: String, t: Term, role: Role, clauseAnnotation: ClauseAnnotation = NoAnnotation): String = clauseAnnotation match {
    case NoAnnotation => s"thf($name, ${role.pretty}, (${toTPTP0(t, Seq.empty)}))."
    case other => s"thf($name, ${role.pretty}, (${toTPTP0(t, Seq.empty)}),${other.pretty})."
  }

  private def toTPTP0(t: Term, bVars: Seq[(String, Type)]): String = "("+{
    val sig = Signature.get
    t match {
      // Constant symbols
      case Symbol(id) => {
        val name = sig(id).name
        if (name.startsWith("'") && name.endsWith("'")) {
          "'" + name.substring(1, name.length-1).replaceAll("\\\\", """\\\\""").replaceAll("\\'", """\\'""") + "'"
        } else {
          name
        }
      }
      // Give Bound variables names
      case m@MetaVar(_,scope) => "sV"+scope
      case Bound(ty, scope) => bVars(scope-1)._1
      // Unary connectives
      case Not(t2) => s"${sig(Not.key).name} (${toTPTP0(t2, bVars)})"
      case Forall(_) => val (bVarTys, body) = collectForall(t)
                        val newBVars = makeBVarList(bVarTys, bVars.length)
                        s"${sig(Forall.key).name} [${newBVars.map({case (s,t) => s"$s:${toTPTP(t)}"}).mkString(",")}]: (${toTPTP0(body, newBVars.reverse ++ bVars)})"
      case Exists(_) => val (bVarTys, body) = collectExists(t)
                        val newBVars = makeBVarList(bVarTys, bVars.length)
                        s"${sig(Exists.key).name} [${newBVars.map({case (s,t) => s"$s:${toTPTP(t)}"}).mkString(",")}]: (${toTPTP0(body, newBVars.reverse ++ bVars)})"
      // Binary connectives
      case t1 ||| t2 => s"${toTPTP0(t1, bVars)} ${sig(|||.key).name} ${toTPTP0(t2, bVars)}"
      case t1 === t2 => s"${toTPTP0(t1, bVars)} ${sig(===.key).name} ${toTPTP0(t2, bVars)}"
      case t1 & t2 => s"${toTPTP0(t1, bVars)} ${sig(&.key).name} ${toTPTP0(t2, bVars)}"
      case t1 Impl t2 => s"${toTPTP0(t1, bVars)} ${sig(Impl.key).name} ${toTPTP0(t2, bVars)}"
      case t1 <= t2  => s"${toTPTP0(t1, bVars)} ${sig(<=.key).name} ${toTPTP0(t2, bVars)}"
      case t1 <=> t2 => s"${toTPTP0(t1, bVars)} ${sig(<=>.key).name} ${toTPTP0(t2, bVars)}"
      case t1 ~& t2 => s"${toTPTP0(t1, bVars)} ${sig(~&.key).name} ${toTPTP0(t2, bVars)}"
      case t1 ~||| t2 => s"${toTPTP0(t1, bVars)} ${sig(~|||.key).name} ${toTPTP0(t2, bVars)}"
      case t1 <~> t2 => s"${toTPTP0(t1, bVars)} ${sig(<~>.key).name} ${toTPTP0(t2, bVars)}"
      case t1 !=== t2 => s"${toTPTP0(t1, bVars)} ${sig(!===.key).name} ${toTPTP0(t2, bVars)}"
      // General structure
      case _ :::> _ => val (bVarTys, body) = collectLambdas(t)
                       val newBVars = makeBVarList(bVarTys, bVars.length)
                       s"^ [${newBVars.map({case (s,t) => s"$s:${toTPTP(t)}"}).mkString(",")}]: (${toTPTP0(body, newBVars.reverse ++ bVars)})"
      case f ∙ args => args.foldLeft(toTPTP0(f, bVars))({case (str, arg) => s"($str @ ${toTPTP0(arg.fold(identity, _ => throw new IllegalArgumentException), bVars)})"})
      // Others should be invalid
      case _ => throw new IllegalArgumentException("Unexpected term format during toTPTP conversion")
    }
  }+")"

  ///////////////////////////////
  // Translation of THF types
  ///////////////////////////////


  private def toTPTP(ty: Type): String = ty match {
    case BaseType(id) => Signature.get(id).name
    case BoundType(scope) => throw new IllegalArgumentException("TPTP THF backward translation of polymorphic types not supported yet")
    case t1 -> t2 => s"(${toTPTP(t1)} > ${toTPTP(t2)})"
    case t1 * t2 => s"(${toTPTP(t1)} * ${toTPTP(t2)})"
    case t1 + t2 => s"(${toTPTP(t1)} + ${toTPTP(t2)})"
    case ∀(t) => throw new IllegalArgumentException("TPTP THF backward translation of polymorphic types not supported yet")
    /**s"${Signature.get(Forall.key).name} []: ${toTPTP(t)}"*/
  }


  ///////////////////////////////
  // Utility methods
  ///////////////////////////////

  /** Gather consecutive all-quantifications (nameless). */
  private def collectForall(t: Term): (Seq[Type], Term) = {
    collectForall0(Seq.empty, t)
  }
  @tailrec
  private def collectForall0(vars: Seq[Type], t: Term): (Seq[Type], Term) = {
    t match {
      case Forall(ty :::> b) => collectForall0(vars :+ ty, b)
      case Forall(_) => throw new IllegalArgumentException("Unexcepted body term in all quantification decomposition.")
      case _ => (vars, t)
    }
  }
  /** Gather consecutive exist-quantifications (nameless). */
  private def collectExists(t: Term): (Seq[Type], Term) = {
    collectExists0(Seq.empty, t)
  }
  @tailrec
  private def collectExists0(vars: Seq[Type], t: Term): (Seq[Type], Term) = {
    t match {
      case Exists(ty :::> b) => collectExists0(vars :+ ty, b)
      case Exists(_) => throw new IllegalArgumentException("Unexcepted body term in existsl quantification decomposition.")
      case _ => (vars, t)
    }
  }

  /** Gather consecutive lambda-abstractions (nameless). */
  private def collectLambdas(t: Term): (Seq[Type], Term) = {
    collectLambdas0(Seq.empty, t)
  }
  @tailrec
  private def collectLambdas0(vars: Seq[Type], t: Term): (Seq[Type], Term) = {
    t match {
      case ty :::> b => collectLambdas0(vars :+ ty, b)
      case _ => (vars, t)
    }
  }

  private def makeBVarList(tys: Seq[Type], offset: Int): Seq[(String, Type)] = Stream.from(offset).zip(tys).map({case (i,t) => (intToName(i), t)})


  // Convert i-th variable to a variable name corresponding to ASCII transformation `intToName`
  // 0 ---> "A"
  // 1 ---> "B"
  // 25 ---> "Z"
  // 26 ---> "ZA"
  // etc.

  private val asciiA = 65
  private val asciiZ = 90
  private val range = asciiZ - asciiA // range 0,1,....

  private def intToName(i: Int): String = i match {
    case n if n <= range => s"${intToChar(i)}"
    case n if n > range => s"Z${intToName(i-range-1)}"
  }

  private def intToChar(i: Int): Char = i match {
    case n if n <= range => (n + asciiA).toChar
    case _ => throw new IllegalArgumentException
  }

}
