package workflow.core

import akka.actor._

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.concurrent.ExecutionContext.Implicits.global

trait Workflow[Inputs <: Product] extends Node[Inputs] {

  val id: Long = Workflow.next

  protected val subNodes = new ListBuffer[Node[_]]
  def allSubNodes: Iterator[Node[_]] = subNodes.iterator

  def call[T <: Node[_]](node: T)(implicit classTag: ClassTag[T]): T = {
    node.actor ! Message.NodeInit
    node.passableInputs.foreach(_.addInputNode(node))
    subNodes += node
    node
  }

  protected lazy val start: Future[Unit] = {
    require(status == Status.ReadyToStart)
    setStatus(Status.Running)
    Future { workflow() }
  }

  def workflow(): Unit

  lazy val actor: ActorRef = system.actorOf(Props(new Workflow.WorkflowActor(this)), fullName)
}

object Workflow {
  private var count = 0L

  private def next: Long = {
    count += 1
    count
  }

  class WorkflowActor[T <: Workflow[_ <: Product]](workflow: T) extends Node.NodeActor[T] {

    def node: T = workflow

    override def receive: Receive = super.receive orElse {
      case Message.Init =>
        workflow.setStatus(Status.ReadyToStart)
        self ! Message.Start
      case Message.SubNodeDone(node) =>
        val all = workflow.allSubNodes.size
        val done = workflow.allSubNodes.count(_.status == Status.Done)
        log.info(s"Status: $done/$all")
        if (all == done) self ! Message.Finish
      case Message.Start =>
        workflow.start
      case Message.Finish =>
        if (workflow.allSubNodes.forall(_.status == Status.Done)) {
          workflow.setStatus(Status.Done)
          workflow.root match {
            case Some(root) => root.actor ! Message.SubNodeDone(workflow)
            case _ =>
              log.info("Root workflow done")
              context.system.terminate()
          }
        }
    }
  }
}