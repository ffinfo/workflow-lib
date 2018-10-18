package workflow.core_untyped

import akka.actor.{ActorRef, Props}

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success}
import scala.concurrent.duration._

trait CommandlineJob[Inputs <: Product] extends Job[Inputs] { job =>
  def cmd: String

  trait JobOutputs extends NodeOutputs {
    val stdout = file(backend.stdoutFile(job))
    val stderr = file(backend.stderrFile(job))
  }

  type Outputs <: JobOutputs

  def run(): Future[_] = Future {
    //TODO: backends
    println(s"cmd: $cmd")
    //Future.successful("")
    //Sys.execAsyncString(cmd).get
  }

  protected[core_untyped] var jobId: Option[String] = None

  override lazy val actor: ActorRef = system.actorOf(Props(new CommandlineJob.JobActor(this)), fullName)
}

object CommandlineJob {

  class JobActor[T <: CommandlineJob[_ <: Product]](job: T) extends Job.JobActor(job) {

    override def receive: Receive = {
      case Message.Start if node.status == Status.ReadyToStart =>
        node.setStatus(Status.Queued)
      case Message.CheckExitCode =>
        job.backend.pollExitCode(job).onComplete {
          case Success(Some("0")) =>
            log.debug("Succeed")
            context.system.scheduler.scheduleOnce(defaultWaitTime, self, Message.ProcessOutputs)
          case Success(Some(exitCode)) =>
            log.error(s"Failed with exit code '$exitCode'")
            context.system.scheduler.scheduleOnce(defaultWaitTime, self, Message.Failed)
          case Success(_) =>
            context.system.scheduler.scheduleOnce(2 second, self, Message.CheckExitCode)
          case Failure(exception) =>
            log.error("Failed while fetching exit code")
            context.system.scheduler.scheduleOnce(defaultWaitTime, self, Message.Failed)
            throw exception
        }
      case m => super.receive(m)
    }
  }
}