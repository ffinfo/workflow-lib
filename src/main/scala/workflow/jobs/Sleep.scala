package workflow.jobs

import akka.actor.ActorSystem
import workflow.core.{CommandlineJob, Passable, Workflow}

class Sleep(val inputs: Sleep.Inputs,
            val root: Option[Workflow[_ <: Product]])(implicit val system: ActorSystem) extends CommandlineJob[Sleep.Inputs] {

  def cmd: String = s"sleep ${inputs.time.value}"

  class Outputs extends NodeOutputs {
    val number = long
  }

  val outputs = new Outputs
}
 object Sleep {
  case class Inputs(time: Passable[Long])
}

