package workflow.core.backend

import java.io.File

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Cancellable, Props}
import akka.dispatch.MessageDispatcher
import akka.dispatch.sysmsg.Failed
import com.typesafe.config.{Config, ConfigFactory}
import workflow.core.{CommandlineJob, Message, Status, WfActor, defaultWaitTime}

import scala.collection.parallel.{ParIterable, Splitter}
import scala.collection.parallel.mutable.{ParMap, ParSeq, ParSet}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

trait Backend extends WfActor {

  implicit val system: ActorSystem

  val ioDispatcher: MessageDispatcher = system.dispatchers.lookup("io-dispatcher")

  val config: Config = ConfigFactory.load()

  val maxConcurentJobs: Int = 100

  private def executeJob(job: CommandlineJob[_ <: Product])(implicit ec: ExecutionContext): Future[Unit] = {
    submitJob(job).map { id =>
      job.jobId = Some(id)
      job.setStatus(Status.Running)
    }
  }

  final protected def jobDone(job: CommandlineJob[_ <: Product]): Unit = {
    system.scheduler.scheduleOnce(defaultWaitTime, job.actor, Message.ProcessOutputs)(ioDispatcher)
  }

  protected def submitJob(job: CommandlineJob[_ <: Product]): Future[String]

  protected def poll(running: List[CommandlineJob[_ <: Product]]): Unit

  val actor: ActorRef = system.actorOf(Props(new Backend.BackendActor(this)))
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
