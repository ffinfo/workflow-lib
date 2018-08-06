package workflow

import nl.biopet.utils.Logging

import scala.concurrent.Future

trait Node extends Logging {

  def key: String

  val inputs: Inputs

  val outputs: Outputs

  def future: Future[_]
}
