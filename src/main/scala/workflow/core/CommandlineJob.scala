package workflow.core

import java.io.File

import akka.actor.{ActorRef, ActorSystem, Props}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait CommandlineJob[Inputs <: Product, Outputs <: Product] extends Job[Inputs, Outputs] {
  def cmd: String

  def run(): Future[_] = Future {
    //TODO: backends
    println(s"cmd: $cmd")
    //Future.successful("")
    //Sys.execAsyncString(cmd).get
  }

  override def createActor(system: ActorSystem): ActorRef = system.actorOf(Props(new CommandlineJob.JobActor(this)), fullName)
}

object CommandlineJob {
  private def template(cmd: String, cwd: File, stdout: File, stderr: File): String =
    s"""
      |#!/bin/bash
      |set -e -p pipefail
      |
      |mkdir -p $cwd
      |cd $cwd
      |
      |( $cmd ) > $stdout 2> $stderr
      |RC=$$?
      |
    """.stripMargin

  class JobActor[T <: Job[_ <: Product, _ <: Product]](job: T) extends Job.JobActor(job) {

  }
}