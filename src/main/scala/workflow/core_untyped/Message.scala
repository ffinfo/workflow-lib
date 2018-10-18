package workflow.core_untyped

object Message extends Enumeration {
  val NodeInit, Status, Init, CheckInputs, Start, CheckExitCode, ProcessOutputs, ProcessOutputDone, Failed, Finish = Value

  case class SetPassable[T](value: T)
  case class SubNodeDone(node: Node[_ <: Product])
  case class ExecuteJob(job: CommandlineJob[_ <: Product])
  case class PollJobs(queued: List[CommandlineJob[_ <: Product]], running: List[CommandlineJob[_ <: Product]])
  case class ExecuteJobs(jobs: List[CommandlineJob[_ <: Product]])
}
