package workflow.core_untyped

import akka.actor._

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.reflect.ClassTag

trait Workflow[Inputs <: Product] extends Node[Inputs] {

  val id: Long = Workflow.next

  protected val subNodes = new ListBuffer[Node[_]]
  def allSubNodes: Iterator[Node[_]] = subNodes.iterator
  def allJobs: Iterator[Job[_ <: Product]] = subNodes.iterator.flatMap {
    _ match {
      case job: Job[Product] => Iterator(job)
      case workflow: Workflow[_] => workflow.allJobs
      case _ => Iterator()
    }
  }

  def allSubWorkflows: Iterator[Workflow[_ <: Product]] = subNodes.iterator.flatMap {
    _ match {
      case workflow: Workflow[Product] => workflow.allSubWorkflows ++ Iterator(workflow)
      case _ => Iterator()
    }
  }


  def call[T <: Node[_]](node: T)(implicit classTag: ClassTag[T]): T = {
    system.scheduler.scheduleOnce(defaultWaitTime, node.actor, Message.NodeInit)
    node.passableInputs.foreach(_.addInputNode(node))
    subNodes += node
    node
  }

  protected lazy val start: Future[Unit] = {
    require(status == Status.ReadyToStart)
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

    override def receive: Receive = {
      case Message.Status =>
        val backends = workflow.allJobs.flatMap{
          case j: CommandlineJob[Product] => Some(j)
          case _ => None
        }.toList.groupBy(_.backend)

        backends.foreach { case (backend, jobs) =>
          val status = jobs.groupBy(_.status)
          val queued: List[CommandlineJob[_ <: Product]] = status.getOrElse(Status.Queued, Nil)
          val running: List[CommandlineJob[_ <: Product]] = status.getOrElse(Status.Running, Nil)
          context.system.scheduler.scheduleOnce(50 milliseconds, backend.actor, Message.PollJobs(queued, running))(defaultDispatcher)
        }

        val counts = workflow.allJobs.toList.groupBy(_.status).map(x => x._1 -> x._2.size)
        log.info("Status: " + counts.map(x => s"${x._1}: ${x._2}").mkString(", "))
      case Message.Init =>
        workflow.setStatus(Status.ReadyToStart)
        context.system.scheduler.scheduleOnce(defaultWaitTime, self, Message.Start)(defaultDispatcher)
      case Message.SubNodeDone(node) =>
        val all = workflow.allSubNodes.size
        val done = workflow.allSubNodes.count(_.status == Status.Done)
        if (all == done) context.system.scheduler.scheduleOnce(defaultWaitTime, self, Message.Finish)(defaultDispatcher)
      case Message.Start =>
        workflow.start.map(_ => node.setStatus(Status.Running))(defaultDispatcher)
      case Message.ProcessOutputDone if node.status == Status.Running =>
        //TODO
      case Message.ProcessOutputs if node.status == Status.Running =>
        log.info("Process outputs")
        node.outputs.allOutputs.foreach(x => context.system.scheduler.scheduleOnce(defaultWaitTime, x.actor, Message.ProcessOutputs)(defaultDispatcher))
        context.system.scheduler.scheduleOnce(defaultWaitTime, self, Message.ProcessOutputDone)(defaultDispatcher)
      case Message.Finish if node.status == Status.Running =>
        if (workflow.allSubNodes.forall(_.status == Status.Done) && workflow.allSubWorkflows.forall(_.start.isCompleted)) {
          node.setStatus(Status.Done)
          log.info("Workflow done")
          workflow.root match {
            case Some(root) => context.system.scheduler.scheduleOnce(defaultWaitTime, root.actor, Message.SubNodeDone(workflow))(defaultDispatcher)
            case _ =>
              log.info("Shutdown")
              context.system.terminate()
          }
        }
      case m => super.receive(m)
    }
  }
}