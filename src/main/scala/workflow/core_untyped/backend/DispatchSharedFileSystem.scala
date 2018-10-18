package workflow.core_untyped.backend

import java.io.File

import akka.dispatch.MessageDispatcher
import workflow.core_untyped.CommandlineJob
import workflow.utils.Io

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.sys.process.Process

trait DispatchSharedFileSystem extends SharedFileSystem {

  protected def submitJob(job: CommandlineJob[_ <: Product]): Future[String] = {
    implicit def c: MessageDispatcher = ioDispatcher
    //println("Start")
    createScript(job).flatMap(_ => createSubmitScript(job))
      .map { _ =>
        val bla = Process(Seq("bash", submitScriptFile(job).getAbsolutePath))
        bla.lineStream.mkString("\n")
      }
  }

  def submitScriptFile(job: CommandlineJob[_ <: Product]): File = {
    new File(executionDir(job), "submit.script")
  }

  def submitScript(job: CommandlineJob[_ <: Product]): String

  def createSubmitScript(job: CommandlineJob[_ <: Product]): Future[File] = Future {
    val file = submitScriptFile(job)
    Io.writeFile(submitScript(job), file)
    file
  }(ioDispatcher)

  def pollScript: String

  lazy val pollScriptFile: File = {
    val script = File.createTempFile("poll.", ".script")
    Io.writeFile(pollScript, script)
    script
  }

  def poll(running: List[CommandlineJob[_ <: Product]]): Unit = {
    val p = Process(Seq("bash", pollScriptFile.getAbsolutePath))
    val ids = p.lineStream.toSet
    running.foreach { job =>
      job.jobId match {
        case Some(id) =>
          if (!ids.contains(id)) {
          //println(s"$id = Done")
          jobDone(job)
        }

        case _ => throw new IllegalStateException(s"No jobid set for '${job.fullName}'")
      }
    }
  }



}
