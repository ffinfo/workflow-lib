package workflow

import java.io.File

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait Job extends Node {

  val id: Long = Job.next

  def run(): Future[_]

  def readyToRun: Boolean = ???

  lazy val start: Future[_] = run()
}

object Job {
  private var count = 0L

  private def next: Long = {
    count += 1
    count
  }
}
