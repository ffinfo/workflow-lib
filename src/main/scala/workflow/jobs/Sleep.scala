package workflow.jobs

import workflow.core.Implicts._
import workflow.core.{CommandlineJob, Passable, Workflow}

import scala.concurrent.Future

case class Sleep(inputs: Sleep.Inputs,
                 root: Option[Workflow[_, _]]) extends CommandlineJob[Sleep.Inputs, Sleep.Outputs] {

  def cmd: String = s"sleep ${inputs.time.value}"

}
 object Sleep {
  case class Inputs(time: Passable[Long])
  case class Outputs()
}

