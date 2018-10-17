package workflow.jobs

import akka.actor.ActorSystem
import workflow.core.{CommandlineJob, Passable, Workflow}
class Echo(val inputs: Echo.Inputs,
           val root: Option[Workflow[_]])(implicit val system: ActorSystem) extends CommandlineJob[Echo.Inputs] {

  def cmd: String = s"echo ${inputs.text.value}"

  class Outputs extends NodeOutputs {
    val number = long
  }

  val outputs = new Outputs

}

object Echo {
  case class Inputs(text: Passable[String])
}