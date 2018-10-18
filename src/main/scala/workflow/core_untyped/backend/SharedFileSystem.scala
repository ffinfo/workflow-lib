package workflow.core_untyped.backend

import java.io.File

import akka.dispatch.MessageDispatcher
import workflow.core_untyped.CommandlineJob
import workflow.utils.Io

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

trait SharedFileSystem extends Backend {

  lazy val root: File = {
    if (config.hasPath("execution-dir")) new File(config.getString("execution-dir"))
    else new File(".execution").getAbsoluteFile
  }

  def jobDir(job: CommandlineJob[_ <: Product]): File = {
    new File(root, job.path().map(_.name).mkString(File.separator))
  }

  def executionDir(job: CommandlineJob[_ <: Product]): File = {
    new File(jobDir(job), s"try-${job.retry}")
  }

  def stdoutFile(job: CommandlineJob[_ <: Product]): File = {
    new File(executionDir(job), "stdout")
  }

  def stderrFile(job: CommandlineJob[_ <: Product]): File = {
    new File(executionDir(job), "stderr")
  }

  def scriptFile(job: CommandlineJob[_ <: Product]): File = {
    new File(executionDir(job), "script")
  }

  def rcFile(job: CommandlineJob[_ <: Product]): File = {
    new File(executionDir(job), "rc")
  }

  def createScript(job: CommandlineJob[_ <: Product]): Future[File]= Future {
    val dir = executionDir(job)
    dir.mkdirs()
    val file = scriptFile(job)
    Io.writeFile(template(job), scriptFile(job))
    file.setExecutable(true)
    file
  }(ioDispatcher)

  protected[core_untyped] def pollExitCode(job: CommandlineJob[_ <: Product]): Future[Option[String]] = Future {
    val file = rcFile(job)
    if (file.exists()) {
      val reader = Source.fromFile(file)
      val content = reader.getLines().mkString("\n")
      reader.close()
      Some(content)
    } else None
  }(ioDispatcher)

  def template(job: CommandlineJob[_ <: Product]): String = {
    val cwd = executionDir(job)
    val stdout = stdoutFile(job)
    val stderr = stderrFile(job)
    val cmd = job.cmd

    s"""
       |#!/bin/bash
       |set -p pipefail
       |
       |mkdir -p $cwd
       |cd $cwd
       |
       |(
       | echo Start: `date`
       |$cmd
       |) > $stdout 2> $stderr
       |RC=$$?
       |echo Done: `date` >> $stdout
       |echo $$RC > ${rcFile(job)}
       |
       |exit $$RC
       |
    """.stripMargin
  }
}
