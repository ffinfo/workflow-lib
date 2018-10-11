package workflow

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait Workflow extends Node {

  val id: Long = Workflow.next

  protected val subNodes = new ListBuffer[Node]

  def call[T <: Node](node: T): T = {
    subNodes += node
    node
  }

  def workflow(): Unit

  lazy val start: Future[_] = {
    workflow()
    Future.sequence(subNodes.map(_.start).toList)
  }
}

object Workflow {
  private var count = 0L

  private def next: Long = {
    count += 1
    count
  }
}
