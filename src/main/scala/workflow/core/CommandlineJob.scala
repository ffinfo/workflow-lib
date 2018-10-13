package workflow.core

import java.io.File

import akka.actor.{ActorRef, ActorSystem, Props}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
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

  override lazy val actor: ActorRef = system.actorOf(Props(new CommandlineJob.JobActor(this)), fullName)
}

object CommandlineJob {
  private def template(cmd: String, cwd: File, stdout: File, stderr: File, rcFile: File): String =
    s"""
       |#!/bin/bash
       |set -e -p pipefail
       |
       |mkdir -p $cwd
       |cd $cwd
       |
       |( $cmd ) > $stdout 2> $stderr
       |RC=$$?
       |echo $$RC > $rcFile
       |
    """.stripMargin

  class JobActor[T <: CommandlineJob[_ <: Product]](job: T) extends Job.JobActor(job) {
    override def receive: Receive = commandReceive orElse super.receive

    private def commandReceive: Receive = {
      case Message.Start if node.status == Status.ReadyToStart =>
        log.info("Starting")
        node.setStatus(Status.Running)
        node.runningJob = Some(node.run())
        node.runningJob.foreach(_.onComplete {
          case Success(_) =>
            log.info("Submitted")
            node.runningJob = None
            cancellable = Some(context.system.scheduler.schedule(Duration.Zero, 2 seconds, self, Message.PollJob))
          case Failure(e) =>
            node.runningJob = None
            throw e
        })
      case Message.PollJob if node.status == Status.Running =>

        node.runningJob match {
          case Some(f) if f.isCompleted =>
            cancellable.foreach(_.cancel())
            f.onComplete {
              case Success(v) =>
                log.info("Completed")
                self ! Message.ProcessOutputs
                node.runningJob = None
              case Failure(e) =>
                log.error("Failed")
                node.runningJob = None
                throw e
            }
          case _ =>
      }
    }
  }
}