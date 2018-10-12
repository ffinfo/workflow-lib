package workflow.jobs

import workflow.core.{CommandlineJob, Workflow}

import workflow.core.Implicts._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class Echo(inputs: Echo.Inputs,
                root: Option[Workflow[_, _]]) extends CommandlineJob[Echo.Inputs, Echo.Outputs] {

  def cmd: String = s"echo ${inputs.text.s}"
}

object Echo {
  case class Inputs(text: Future[String])
  case class Outputs()
}
