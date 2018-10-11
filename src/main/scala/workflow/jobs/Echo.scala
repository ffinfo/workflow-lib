package workflow.jobs

import workflow.{CommandlineJob, Workflow}

case class Echo(inputs: Echo.Inputs,
                outputs: Echo.Outputs,
                root: Option[Workflow] = None) extends CommandlineJob {

  def cmd: String = s"echo ${inputs.text}"
}

object Echo {
  case class Inputs(text: String)
  case class Outputs()
}
