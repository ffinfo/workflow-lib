package workflow

import nl.biopet.utils.process.Sys

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait CommandlineJob extends Job {
  def cmd: Seq[String]

  def run(): Future[_] = {
    //TODO: backends
    Sys.execAsync(cmd).get
  }
}
