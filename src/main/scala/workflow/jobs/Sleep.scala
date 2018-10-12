package workflow.jobs

import workflow.core.Implicts._
import workflow.core.{CommandlineJob, Workflow}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class Sleep(inputs: Sleep.Inputs,
                 root: Option[Workflow[_, _]]) extends CommandlineJob[Sleep.Inputs, Sleep.Outputs] {

  def cmd: String = s"sleep ${inputs.time.s}"

}
 object Sleep {
  case class Inputs(time: Future[Long])
  case class Outputs()
}

