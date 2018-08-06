package workflow

trait Node {

  def key: String

  def inputs: Inputs

  def outputs: Outputs

  def isDone: Boolean = outputs.allDone
}
