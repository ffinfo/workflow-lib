package workflow.core_untyped

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.dispatch.MessageDispatcher

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

trait Job[Inputs <: Product] extends Node[Inputs] {

  val id: Long = Job.next

  def run(): Future[_]

  private var _retry = 1
  def retry: Int = _retry

  protected var runningJob: Option[Future[_]] = None

  lazy val actor: ActorRef = system.actorOf(Props(new Job.JobActor(this)), fullName)
}

object Job {
  private var count = 0L

  private def next: Long = {
    count += 1
    count
  }

  class JobActor[T <: Job[_ <: Product]](job: T) extends Node.NodeActor[T] {

    implicit val executionContext: MessageDispatcher = context.system.dispatchers.lookup("default-dispatcher")

    def node: T = job

    override def receive: Receive = {
      case Message.Init if node.status == Status.Init =>
        //log.info("Init")
        node.setStatus(Status.WaitingOnInputs)
        context.system.scheduler.scheduleOnce(Duration.Zero, self, Message.CheckInputs)
      case Message.CheckInputs if node.status == Status.WaitingOnInputs =>
        val pass = node.passableInputs.toList
        val total = pass.size
        val done = pass.count(_.isSet)
        if (total == done) {
          node.setStatus(Status.ReadyToStart)
          context.system.scheduler.scheduleOnce(defaultWaitTime, self, Message.Start)
        } else log.debug(s"Wait on inputs, $done/$total")
      case Message.Start if node.status == Status.ReadyToStart =>
        log.info("Starting")
        node.setStatus(Status.Running)
        node.runningJob = Some(node.run())
        node.runningJob.foreach(_.onComplete {
          case Success(v) =>
            log.info("Completed")
            context.system.scheduler.scheduleOnce(defaultWaitTime, self, Message.ProcessOutputs)
          case Failure(e) =>
            log.error("Failed")
            context.system.scheduler.scheduleOnce(defaultWaitTime, self, Message.Failed)
            throw e
        })
      case Message.ProcessOutputs if node.status == Status.Running || node.status == Status.WaitingOnExitCode =>
        log.debug("Process outputs")
        node.setStatus(Status.ProcessOutputs)
        node.outputs.allOutputs.foreach(x => context.system.scheduler.scheduleOnce(defaultWaitTime, x.actor, Message.ProcessOutputs)(defaultDispatcher, self))
        context.system.scheduler.scheduleOnce(defaultWaitTime, self, Message.ProcessOutputDone)(defaultDispatcher)
      case Message.ProcessOutputDone if node.status == Status.ProcessOutputs =>
        if (node.outputs.allOutputs.forall(_.isSet)) {
          context.system.scheduler.scheduleOnce(defaultWaitTime, self, Message.Finish)
        }
      case Message.Finish if node.status == Status.ProcessOutputs =>
        log.info("Job done")
        node.setStatus(Status.Done)
        node.root.foreach(x => context.system.scheduler.scheduleOnce(defaultWaitTime, x.actor, Message.SubNodeDone(node)))
      case Message.Failed =>
        ???
      case m => super.receive(m)
    }
  }
}
