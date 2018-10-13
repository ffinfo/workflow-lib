package workflow.core

object Message extends Enumeration {
  val NodeInit, Init, CheckInputs, Start, PollJob, ProcessOutputs, ProcessOutputDone, Failed, Finish = Value

  case class SetPassable[T](value: T)
  case class SubNodeDone(node: Node[_ <: Product])
}
