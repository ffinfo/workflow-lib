package workflow.core

import akka.actor.{ActorRef, ActorSystem, Props}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait Job[Inputs <: Product, Outputs <: Product] extends Node[Inputs, Outputs] {

  val id: Long = Job.next

  def run(): Future[_]

  def createActor(system: ActorSystem): ActorRef =
    system.actorOf(Props(new Job.JobActor(this)), fullName)
}

object Job {
  private var count = 0L

  private def next: Long = {
    count += 1
    count
  }


  class JobActor[T <: Job[_ <: Product, _ <: Product]](job: T) extends Node.NodeActor[T] {

    def node: T = job

    override def receive: Receive = super.receive orElse {
      case Message.Init if node.status == Status.Init =>
        node.setStatus(Status.WaitingOnInputs)
        cancellable = Some(context.system.scheduler.schedule(Duration.Zero, 1 seconds, self, Message.CheckInputs))
      case Message.CheckInputs if node.status == Status.WaitingOnInputs =>
        if (node.futureInputs.forall(_.isCompleted)) {
          cancellable.foreach(_.cancel())
          cancellable = None
          node.setStatus(Status.ReadyToStart)
          self ! Message.Start
        } else {
          log.info("Wait on inputs")
        }
      case Message.Start if node.status == Status.ReadyToStart =>
        log.info("Starting")
        node.setStatus(Status.Running)
        node.run()
      case Message.Finish if node.status == Status.Running =>
        log.info("Finish")
        node.setStatus(Status.Done)
    }
  }
}
