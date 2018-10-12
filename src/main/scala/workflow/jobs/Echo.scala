package workflow.jobs

import workflow.core.{CommandlineJob, Passable, Workflow}
case class Echo(inputs: Echo.Inputs,
                root: Option[Workflow[_, _]]) extends CommandlineJob[Echo.Inputs, Echo.Outputs] {

  def cmd: String = s"echo ${inputs.text.value}"
}

object Echo {
  case class Inputs(text: Passable[String])
  case class Outputs()
}
