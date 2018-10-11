package workflow

import nl.biopet.utils.Logging

import scala.concurrent.Future

trait Node extends Logging {

  type Inputs >: Product
  type Outputs >: Product


  def inputs: Inputs
  def outputs: Outputs

  val root: Option[Workflow]

  def key: String = this.getClass.getSimpleName

  def start: Future[_]
}
