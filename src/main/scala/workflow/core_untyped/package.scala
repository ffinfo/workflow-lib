package workflow

import scala.concurrent.duration._

package object core_untyped {
  case class Empty()

  object Status extends Enumeration {
    val Init, WaitingOnInputs, WaitingOnJob, ReadyToStart, Queued, Running, WaitingOnExitCode, ProcessOutputs, Failed, Done = Value
  }

  val defaultWaitTime: FiniteDuration = 50 milliseconds
}