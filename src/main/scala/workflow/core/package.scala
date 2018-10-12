package workflow

package object core {
  case class Empty()

  object Status extends Enumeration {
    val Init, WaitingOnInputs, ReadyToStart, Running, Done = Value
  }

}