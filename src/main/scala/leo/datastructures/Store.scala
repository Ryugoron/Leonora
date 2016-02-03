package leo.datastructures

import java.util.concurrent.atomic.AtomicInteger

/**
  * Created by mwisnie on 2/3/16.
  */
object Store {
  var unnamedFormulas : AtomicInteger = new AtomicInteger(0)

  def apply(cl: Clause, role: Role, status: Int, annotation: ClauseAnnotation = NoAnnotation): FormulaStore
  = new FormulaStore("gen_formula_"+unnamedFormulas.incrementAndGet(), cl, TimeStamp(), role, status, annotation)

  def apply(name: String, cl: Clause, role: Role, status: Int, annotation: ClauseAnnotation): FormulaStore
  = new FormulaStore(name, cl, TimeStamp(), role, status, annotation)

  def apply(cl: Clause, created : TimeStamp, role: Role, status: Int, annotation: ClauseAnnotation): FormulaStore
  = new FormulaStore("gen_formula_"+unnamedFormulas.incrementAndGet(), cl, created, role, status, annotation)

}
