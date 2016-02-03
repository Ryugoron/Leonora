package leo.datastructures


/**
 * Class to Store Formula Information.
 *
 * Table for status Int. Please extend as you like
 * <table>
 *   <thead>
 *    <tr><th>Bit</th><th>Descritpion</th></tr>
 *   </thead>
 *   <tbody>
 *     <tr><th>1.</th><th> Term is simplified </th></tr>
 *     <tr><th>2.</th><th> Term is def expanded </th></tr>
 *     <tr><th>3.</th><th> Term is in Negation Normal Form </th></tr>
 *     <tr><th>4.</th><th> Term is Skolemized </th></tr>
 *     <tr><th>5.</th><th> Term is in PrenexNormalform </th></tr>
 *     <tr><th>6.</th><th> Term should be proven as a conjecture in an external prover</th></tr>
 *   </tbody>
 * </table>
 *
 */
class FormulaStore(val name : String, val clause : Clause, val created : TimeStamp, val role : Role, val status : Int, val annotation : ClauseAnnotation)
  extends Pretty with Ordered[FormulaStore] with HasCongruence[FormulaStore] {

  /**
   *
   * This method returns the flag set for a fully normalized term
   *
   * (ATM only the first two, therefor 1 + 4 = 5)
   *
   * @return true, if normalized
   */
  def normalized : Boolean = (status & 3)== 3

  lazy val pretty : String = "leo("+name+","+role.pretty+",("+clause.pretty+"), status="+status+")."

  override lazy val toString : String = "leo("+name+","+role.pretty+",("+clause.pretty+"), status="+status+")."

  def compare(that: FormulaStore): Int = this.clause compare that.clause

  /** Returns `true` iff `this` is congruent to `that`. */
  override def cong(that: FormulaStore): Boolean = clause.cong(that.clause)

  override def equals(o : Any) : Boolean = o match {
    case fo : FormulaStore => (this.clause cong fo.clause) && (this.role == fo.role) && (this.status == fo.status)    // TODO: Quickfix. Prevents the blackboard from not normalizing, but should be tested somewhere else.
    case _ => false
  }

  override def hashCode() : Int = this.clause.hashCode() ^ this.role.hashCode()
}
