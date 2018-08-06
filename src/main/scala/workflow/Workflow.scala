package workflow

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait Workflow extends Node {

  protected val subNodes = new ListBuffer[Node]

  def call[T <: Node](node: T): T = {
    subNodes += node
    node
  }

  def future: Future[_] = Future.sequence(outputs.future :: subNodes.map(_.future).toList)
}
