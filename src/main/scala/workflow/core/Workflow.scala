package workflow.core

import akka.actor._

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.reflect.ClassTag

import scala.concurrent.ExecutionContext.Implicits.global

trait Workflow[Inputs <: Product, Outputs <: Product] extends Node[Inputs, Outputs] {

  val id: Long = Workflow.next

  protected val subNodes = new ListBuffer[Node[_, _]]

  def call[T <: Node[_, _]](node: T)(implicit classTag: ClassTag[T]): T = {
    node.loadActor(system)
    node.actor ! Message.NodeInit
    subNodes += node
    node
  }

  protected lazy val start: Future[Unit] = {
    require(status == Status.ReadyToStart)
    setStatus(Status.Running)
    Future { workflow() }
  }

  def workflow(): Unit

  def createActor(system: ActorSystem): ActorRef =
    system.actorOf(Props(new Workflow.WorkflowActor(this)), fullName)
}

object Workflow {
  private var count = 0L

  private def next: Long = {
    count += 1
    count
  }

  class WorkflowActor[T <: Workflow[_, _]](workflow: T) extends Node.NodeActor[T] {

    def node: T = workflow

    override def receive: Receive = super.receive orElse {
      case Message.Init =>
        node.setStatus(Status.ReadyToStart)
        self ! Message.Start
      case Message.Start =>
        node.start
    }
  }
}