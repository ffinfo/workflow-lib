package workflow

package object core {
  case class Empty()

  object Status extends Enumeration {
    val Init, WaitingOnInputs, WaitingOnJob, ReadyToStart, Running, ProcessOutputs, Failed, Done = Value
  }

}