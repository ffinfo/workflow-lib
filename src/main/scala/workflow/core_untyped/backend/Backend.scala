package workflow.core_untyped.backend

import java.io.File

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.dispatch.MessageDispatcher
import com.typesafe.config.{Config, ConfigFactory}
import workflow.core_untyped._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

trait Backend extends WfActor {

  implicit val system: ActorSystem

  val ioDispatcher: MessageDispatcher = system.dispatchers.lookup("io-dispatcher")

  val config: Config = ConfigFactory.load()

  val maxConcurentJobs: Int = 100
  val maxRetries: Int = 3

  private def executeJob(job: CommandlineJob[_ <: Product])(implicit ec: ExecutionContext): Future[Unit] = {
    submitJob(job).map { id =>
      job.jobId = Some(id)
      job.setStatus(Status.Running)
    }
  }

  final protected def jobDone(job: CommandlineJob[_ <: Product]): Unit = {
    job.setStatus(Status.WaitingOnExitCode)
    job.jobId = None
    system.scheduler.scheduleOnce(defaultWaitTime, job.actor, Message.CheckExitCode)(ioDispatcher)
  }

  protected def submitJob(job: CommandlineJob[_ <: Product]): Future[String]

  protected def poll(running: List[CommandlineJob[_ <: Product]]): Unit

  protected[core_untyped] def pollExitCode(job: CommandlineJob[_ <: Product]): Future[Option[String]]

  val actor: ActorRef = system.actorOf(Props(new Backend.BackendActor(this)))

  def stdoutFile(job: CommandlineJob[_ <: Product]): File

  def stderrFile(job: CommandlineJob[_ <: Product]): File
}

object Backend {
  class BackendActor(backend: Backend) extends Actor with ActorLogging {

    implicit val executionContext: MessageDispatcher = context.system.dispatchers.lookup("default-dispatcher")

    def receive: Receive = {
      case Message.PollJobs(queued, running) =>
        log.debug("poll")
        backend.poll(running)
        val submitNumber = backend.maxConcurentJobs - running.count(_.status == Status.Running)
        if (submitNumber > 0) {
          context.system.scheduler.scheduleOnce(50 milliseconds, self, Message.ExecuteJobs(queued.take(submitNumber)))
        }
      case Message.ExecuteJobs(jobs) =>
        jobs.headOption.foreach { first =>
          if (first.status == Status.Queued) backend.executeJob(first)
          context.system.scheduler.scheduleOnce(50 milliseconds, self, Message.ExecuteJobs(jobs.tail))
        }
      case Message.ExecuteJob(job) =>
        backend.executeJob(job).onComplete {
          case Success(_) =>
            job.setStatus(Status.Running)
          case Failure(exception) =>
            log.error(exception.getMessage)
            job.setStatus(Status.Failed)
        }
    }
  }

  def backends()(implicit actorSystem: ActorSystem): Map[String, Backend] = _backends match {
    case Some(m) => m
    case _ =>
      val map = Map("nohup" -> new Nohup())
      _backends = Some(map)
      map
  }
  private var _backends: Option[Map[String, Backend]] = None

}
