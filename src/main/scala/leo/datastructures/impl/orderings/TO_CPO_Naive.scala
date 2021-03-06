package leo.datastructures.impl.orderings

import leo.Configuration
import leo.datastructures.{Term, Type}
import leo.datastructures._
import leo.datastructures.impl.Signature
import leo.modules.output.logger.Out

import scala.annotation.tailrec

/**
 * Computability path Ordering (Core CPO)
 * by Blanqui, Jouannaud, Rubio
 *
 * @author Alexander Steen <a.steen@fu-berlin.de>
 * @since 29.09.2015
 */
object TO_CPO_Naive { //} extends LeoOrdering[Term] {
  import leo.datastructures.Orderings._
  /////////////////////////////////////////////////////////////////
  /// Exported functions
  /////////////////////////////////////////////////////////////////

  /* Comparisons of types */

  // Core function for comparisons
  @inline final def gt(a: Type, b: Type): Boolean = gt0(a,b)
  @inline final def gteq(a: Type, b: Type): Boolean = ge0(a,b)

  // Defined by gt/ge
  @inline final def lt(a: Type, b: Type): Boolean = gt(b,a)
  @inline final def lteq(a: Type, b: Type): Boolean = gteq(b,a)

  @inline final def compare(a: Type, b: Type): CMP_Result = {
    if (a == b) CMP_EQ
    else if (gt(a,b)) CMP_GT
    else if (lt(a,b)) CMP_LT
    else CMP_NC
  }
  @inline final def canCompare(a: Type, b: Type): Boolean = compare(a,b) != CMP_NC


  /* Comparisons of terms */

  // Core function for comparisons
  @inline final def gt(s: Term, t: Term): Boolean = gteq(s.ty, t.ty) && gt0(s,t, Set())
  @inline final def gt(s: Term, t: Term, bound: Set[Term]): Boolean = gteq(s.ty, t.ty) && gt0(s,t, bound)
  @inline final def gtMult(s: Seq[Term], t: Seq[Term]): Boolean = gt0Mult(s,t)

  @inline final def gteq(s: Term, t: Term): Boolean = gteq(s.ty, t.ty) && ge0(s,t, Set())
  @inline final def gteq(s: Term, t: Term, bound: Set[Term]): Boolean = gteq(s.ty, t.ty) && ge0(s,t, bound)

  // Defined by gt/ge
  @inline final def lt(s: Term, t: Term): Boolean = gt(t,s)
  @inline final def lteq(s: Term, t: Term): Boolean = gteq(t,s)

  @inline final def compare(s: Term, t: Term): CMP_Result = {
    if (s == t) CMP_EQ
    else if (gt(s,t)) CMP_GT
    else if (lt(s,t)) CMP_LT
    else CMP_NC
  }
  @inline final def canCompare(s: Term, t: Term): Boolean = compare(s,t) != CMP_NC

  /* Common comparison-related operations */

  // Nothing


  /////////////////////////////////////////////////////////////////
  /// Internal functions
  /////////////////////////////////////////////////////////////////

  // ###############################################################################

  // well-founded ordering of symbols in signature
  final private def precedence(s: Signature#Key, t: Signature#Key): CMP_Result = Configuration.PRECEDENCE.compare(s,t)

  // Well-founded ordering of base types (sort)
  final private def gt_baseType(bt1: Signature#Key, bt2: Signature#Key): Boolean = bt1 > bt2
  final private def ge_baseType(bt1: Signature#Key, bt2: Signature#Key): Boolean = eq_baseType(bt1,bt2) || gt_baseType(bt1,bt2)
  final private def eq_baseType(bt1: Signature#Key, bt2: Signature#Key): Boolean = bt1 == bt2

  ////////////////////////////////////
  // Comparisons of types
  ////////////////////////////////////
  final private def gt0(a: Type, b: Type): Boolean = {
    import leo.datastructures.Type.{BaseType,->,∀}

    if (a == b) return false
    if (a.isBoundTypeVar) return false
    /* a is base type */
    if (a.isBaseType) {
      val aId = BaseType.unapply(a).get

      if (b.isBaseType) {
        val bId = BaseType.unapply(b).get
        return gt_baseType(aId, bId)
      }
      if (b.isFunType) {
        val (bI, bO) = ->.unapply(b).get
        return gt0(a, bI) && gt0(a, bO)
      }
      // TODO: Are there further meaningful cases?
    }
    /* a is function type */
    if (a.isFunType) {
      val (aI, aO) = ->.unapply(a).get

      if (ge0(aO, b)) return true

      if (b.isFunType) {
        val (bI, bO) = ->.unapply(b).get
        if (eq_type(aI,bI)) return gt0(aO,bO)
      }
      // TODO: Are there further meaningful cases?
    }
    /* adaption for quantified types */
    if (a.isPolyType) {
      val aO = ∀.unapply(a).get

      if (ge0(aO, b)) return true

      if (b.isPolyType) {
        val bO = ∀.unapply(b).get
        return gt0(aO,bO)
      }
      // TODO: Are there further meaningful cases?
    }
    /* adaption end */
    // TODO: We dont know what to do with other cases
    false
  }

  final private def ge0(a: Type, b: Type): Boolean = {
    import leo.datastructures.Type.{BaseType,->,∀}

    if (a == b) return true
    if (a.isBaseType) {
      val aId = BaseType.unapply(a).get

      if (b.isBaseType) {
        val bId = BaseType.unapply(b).get
        return ge_baseType(aId, bId)
      }
      if (b.isFunType) {
        val (bI, bO) = ->.unapply(b).get
        return gt0(a, bI) && gt0(a, bO)
      }
      // TODO: Are there further meaningful cases?
    }
    if (a.isFunType) {
      val (aI, aO) = ->.unapply(a).get

      if (ge0(aO, b)) return true

      if (b.isFunType) {
        val (bI, bO) = ->.unapply(b).get
        if (eq_type(aI,bI)) return ge0(aO,bO)
      }
      // TODO: Are there further meaningful cases?
    }
    /* adaption for quantified types */
    if (a.isPolyType) {
      val aO = ∀.unapply(a).get

      if (gt0(aO, b)) return true

      if (b.isPolyType) {
        val bO = ∀.unapply(b).get
        return gt0(aO,bO)
      }
      // TODO: Are there further meaningful cases?
    }
    /* adaption end */
    // TODO: We dont know what to do with other cases
    false
  }

  // Two types are equal wrt to the type ordering if they are
  // syntacticallly equal of they are equal base types (wrt to ordering of base types).
  @inline final private def eq_type(a: Type, b: Type): Boolean = {
    import leo.datastructures.Type.BaseType
    if (a == b) return true
    if (a.isBaseType && b.isBaseType) {
      val (aId, bId) = (BaseType.unapply(a).get, BaseType.unapply(b).get)
      return eq_baseType(aId,bId)
    }
    false
  }

  // ###############################################################################
  ////////////////////////////////////
  // Comparisons of terms
  ////////////////////////////////////

  @inline private final def gt0Stat(a: Term, s: Seq[Term], t: Seq[Term], x: Set[Term], status: Int): Boolean = {
    import leo.datastructures.IsSignature.{lexStatus,multStatus}
    if (status == lexStatus) {
      if (s.length > t.length){
        alleq(s,t,t.length)
      } else gt0Lex(a,s,t,x)
    } else if (status == multStatus) {
      gt0Mult(s,t)
    } else {
      // This should not happen
      Out.severe("[CPO_Naive] Status compare called with unknown status")
      false
    }
  }

  @tailrec
  private final def gt0Lex(a: Term, s: Seq[Term], t: Seq[Term], x: Set[Term]): Boolean = {
    if (s.nonEmpty && t.nonEmpty) {
      if (s.head == t.head) {
        gt0Lex(a,s.tail,t.tail,x)
      } else {
        gt(s.head,t.head) && t.tail.forall(gt0(a,_,x))
      }
    } else false
  }

  @tailrec
  private final def alleq(s: Seq[Term], t: Seq[Term], n: Int): Boolean = {
    if (n == 0) true
    else if (s.head == t.head) {
      alleq(s.tail,t.tail,n-1)
    }
    else false
  }

  private final def gt0Mult(s: Seq[Term], t: Seq[Term]): Boolean = {
    if (s.nonEmpty && t.isEmpty) true
    else if (s.nonEmpty && t.nonEmpty) {
      val sameElements = s.intersect(t)
      val remSameS = s.diff(sameElements)
      val remSameT = t.diff(sameElements)
      if (remSameS.isEmpty && remSameT.isEmpty) false
      else gt0Mult0(remSameS, remSameT)
    } else false
  }

  @tailrec
  private final def gt0Mult0(s: Seq[Term], t: Seq[Term]): Boolean = {
    if (t.isEmpty) true
    else if (s.nonEmpty && t.nonEmpty) {
      val sn = s.head
      val tIt = t.iterator
      var keepT: Seq[Term] = Seq()
      while (tIt.hasNext) {
        val tn = tIt.next()
        if (!gt(sn, tn)) {
          keepT = keepT :+ tn
        }
      }
      gt0Mult0(s.tail,keepT)
    } else false
  }

  final private def gt0(s: Term, t: Term, x: Set[Term]): Boolean = {
    import leo.datastructures.Term.{:::>, Bound, MetaVar, Symbol, TypeLambda, ∙,mkApp}

    if (s == t) return false
    if (s.isVariable) return false

    /* case 6+10+15: ... > y */
    if (t.isVariable) {
      return Bound.unapply(t).isDefined || x.contains(t)
    }

    if (s.isApp || s.isConstant) {

      val (f,args) = ∙.unapply(s).get
      val fargList: Seq[Term] = effectiveArgs(f.ty,args)

      f match {
        // #############
        // All f(t)-rules
        // #############
        case Symbol(idf) =>
        /* f(t) > ... cases */

          /* case 1: f(t) >= v */
          if (fargList.exists(gteq(_, t))) return true

          /* case 2+3: f(t) > g(u) and case 4: f(t) > uv*/
          if (t.isApp || t.isConstant) {

            val (g,args2) = ∙.unapply(t).get
            try {
              val gargList: Seq[Term] = effectiveArgs(g.ty, args2)
              g match {
                case Symbol(idg) =>
                  /* case 2+3 */
                  if (precedence(idf, idg) == CMP_EQ) {
                    return gt0Stat(s,fargList, gargList, x, Signature(idf).status)
                  } else if (precedence(idf, idg) == CMP_GT) {
                    return gargList.forall(gt0(s, _, x))
                  } else {
                    return false
                  }

                case _ if gargList.nonEmpty =>
                  /* case 4*/
                  return gt0(s, Term.mkApp(g, args2.init), x) && gt0(s, gargList.last, x)
              }
            } catch {
              case e:AssertionError => {
                Out.severe(e.getMessage)
                Out.output("TERM s: \t\t " + s.pretty)
                Out.output("TERM t: \t\t " + t.pretty)
                throw e
              }
              case e: Exception => {
                println(idf)
                println(f.pretty)
                throw e
              }
            }
          }
          /* case 5: f(t) > lambda yv*/
          if (t.isTermAbs) {
            val (_,tO) = :::>.unapply(t).get
            return gt0(s,tO,x)
          }

          // otherwise, fail
          return false

        // #############
        // All @-rules
        // #############
        case _ if fargList.nonEmpty => {

          if (ge0(mkApp(f,args.init),t,x) || gteq(fargList.last,t,x)) return true

          if (t.isApp) {
            val (g,args2) = ∙.unapply(t).get

            val gargList: Seq[Term] = effectiveArgs(g.ty,args2)

            if (gargList.nonEmpty) {
              val s2 = mkApp(f,args.init)
              val t2 = mkApp(g, args2.init)
              if (s2 == t2) {
                if (gt0(fargList.last, gargList.last,x)) return true
              }

              return ((gt(s2,t2,x) || gteq(fargList.last,t2,x) || gt(s,t2))
                   && (gt(s2,gargList.last,x) || gteq(fargList.last,gargList.last,x) || gt(s,gargList.last)))
            }
          }

          if (t.isTermAbs) {
            val (_, tO) = :::>.unapply(t).get
            return gt0(s, tO, x)
          }

          return false
        }
        case _ => println(s.pretty);println(f.pretty); assert(false, "CPO: should not happen, sth not in beta nf");

      }
    }
    // #############
    // All \-rules (\>, \=, \!=, \X) without \eta
    // #############
    // TODO: eta rules left out for now -- we are in eta-long form invariantly
    if (s.isTermAbs) {
      val (sInTy, sO) = :::>.unapply(s).get

      if (gteq(sO,t,x)) return true

      if (t.isTermAbs) {
        val (tInTy, tO) = :::>.unapply(t).get

        if (sInTy == tInTy) return gt0(sO, tO, x)
        else return gt0(s, tO, x)
      }

      return false
    }
    // #############
    /* adaption for type abstractions*/
    // #############
    if (s.isTypeAbs) {
      val sO = TypeLambda.unapply(s).get

      if (gteq(sO,t,x)) return true

      if (t.isTypeAbs) {
        val tO = TypeLambda.unapply(t).get

        return gt0(sO,tO,x)
      }
      return false
    }
    /* adaption end */
    Out.severe("Comparing unrecognized term. This is considered a bug! Please report.")
    Out.severe(s.pretty)
    false
  }


  @inline final private def ge0(s: Term, t: Term, x: Set[Term]): Boolean = {
    if (s == t) true
    else gt0(s,t,x)
  }




  final private def effectiveArgs(forTy: Type, args: Seq[Either[Term, Type]]): Seq[Term] = {
    assert(args.take(forTy.polyPrefixArgsCount).forall(_.isRight), s"Number of expected type arguments (${forTy.polyPrefixArgsCount}) do not match ty abstraction count: \n\t Type: ${forTy.pretty}\n\tArgs: ${args.map(_.fold(_.pretty,_.pretty))}")
    filterTermArgs(args.drop(forTy.polyPrefixArgsCount))
  }

  final private def filterTermArgs(args: Seq[Either[Term, Type]]): Seq[Term] = args match {
    case Seq() => Seq()
    case Seq(h, rest@_*) => h match {
      case Left(term) => term +: filterTermArgs(rest)
      case Right(_) => filterTermArgs(rest)
    }
  }
}
