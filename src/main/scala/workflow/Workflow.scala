package workflow

import scala.collection.mutable.ListBuffer

trait Workflow extends Node {

  protected val subNodes = new ListBuffer[Node]

  def call[T <: Node](node: T): T = {
    subNodes += node
    node
  }
}
