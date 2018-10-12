package workflow.core

object Message extends Enumeration {
  val NodeInit, Init, Start, CheckInputs, CheckSubNodes, Finish = Value

  case class SetPassable[T](value: T)
}
