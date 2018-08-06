package workflow

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait Job extends Node {
  def run(): Future[_]

  def readyToRun: Boolean = inputs.future.isCompleted

  lazy val future: Future[_] = inputs.future.flatMap { _ =>
    logger.info(s"Start job '$key'")
    run()
  }.flatMap{_ => outputs.future}
}
