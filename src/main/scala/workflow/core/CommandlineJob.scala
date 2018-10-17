package workflow.core

import java.io.File

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.dispatch.MessageDispatcher
import com.typesafe.config.ConfigFactory
import workflow.core.backend.Backend

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

trait CommandlineJob[Inputs <: Product] extends Job[Inputs] {
  def cmd: String

  def run(): Future[_] = Future {
    //TODO: backends
    println(s"cmd: $cmd")
    //Future.successful("")
    //Sys.execAsyncString(cmd).get
  }

  protected[core] var jobId: Option[String] = None

  override lazy val actor: ActorRef = system.actorOf(Props(new CommandlineJob.JobActor(this)), fullName)
}

object CommandlineJob {

  class JobActor[T <: CommandlineJob[_ <: Product]](job: T) extends Job.JobActor(job) {

    override def receive: Receive = {
      case Message.Start if node.status == Status.ReadyToStart =>
        node.setStatus(Status.Queued)
      case m => super.receive(m)
    }
  }
}