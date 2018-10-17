package workflow

import scala.concurrent.duration._

package object core {
  case class Empty()

  object Status extends Enumeration {
    val Init, WaitingOnInputs, WaitingOnJob, ReadyToStart, Queued, Running, ProcessOutputs, Failed, Done = Value
  }

  val defaultWaitTime: FiniteDuration = 50 milliseconds
}