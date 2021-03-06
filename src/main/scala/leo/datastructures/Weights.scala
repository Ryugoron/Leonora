package leo.datastructures

import leo.{LiteralWeight, ClauseWeight}


/**
 * Interface for weighting objects such as clauses or literals.
 * A smaller weight means that the object should have "more priority" depending
 * on the current context.
 * Every weight defines an ordering by `x <= y :<=> x.weight <= y.weight`,
 * this can be obtained by using the `SimpleOrdering`.
 *
 * @author Alexander Steen
 * @since 25.11.2014
 */
trait Weight[What] {
  def weightOf[A <: What](w: A): Int
}

///////////////////////////////
// Literal weights
///////////////////////////////

/** Simple weighting function that gives every literal the same weight. */
object LitWeight_Constant extends LiteralWeight {
  def weightOf[A <: Literal](lit: A) = 1
}

/** Literal weighting that gives preference (i.e. gives lower weight) to older literals. */
object LitWeight_FIFO extends LiteralWeight {
  def weightOf[A <: Literal](lit: A) = lit.id
}

/** Literal weighting that uses the enclosed term's size as weight. */
object LitWeight_TermSize extends LiteralWeight {
  def weightOf[A <: Literal](lit: A) = lit.term.size
}

// more to come ...

/////////////////////////////////
// Clause weights
/////////////////////////////////

/** Weighting that gives a higher ('worse') weight for newer clauses. */
object CLWeight_FIFO extends ClauseWeight {
  def weightOf[A <: Clause](cl: A) = cl.id
}

/** Clause weighting that assigns the number of literals in the clause as weight. */
object ClWeight_LitCount extends ClauseWeight {
  def weightOf[A <: Clause](cl: A) = cl.lits.size
}

/** Clause weighting that assigns the maximum of all literals weights as weight. */
object ClWeight_MaxLitWeight extends ClauseWeight {
  def weightOf[A <: Clause](cl: A) = cl.lits.map(_.weight).max
}

/** Clause weighting that assigns the sum of all literals weights as weight. */
object CLWeight_LitWeightSum extends ClauseWeight {
  def weightOf[A <: Clause](cl: A) = cl.lits.map(_.weight).sum
}

// more to come ...
