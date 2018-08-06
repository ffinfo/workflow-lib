package workflow

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait Job extends Node {
  def run(): Future[_]

  def readyToRun: Boolean = inputs.allDone

  val future: Future[_] = inputs.totalFuture.flatMap { _ =>
    run()
  }
}
