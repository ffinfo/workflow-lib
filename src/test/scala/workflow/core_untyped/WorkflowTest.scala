package workflow.core_untyped

import akka.actor.ActorSystem
import org.testng.annotations.Test
import workflow.jobs.{Echo, Sleep}
import Implicts._
import akka.dispatch.MessageDispatcher

import scala.concurrent.duration._
import scala.language.postfixOps

class WorkflowTest {
  @Test
  def test(): Unit = {

    implicit val system: ActorSystem = ActorSystem("workflow-lib")
    implicit val executionContext: MessageDispatcher = system.dispatchers.lookup("default-dispatcher")

    val wf = new TestWorkflow(Empty(), None)
    wf.actor ! Message.NodeInit

  }
}

object WorkflowApp extends App {
  implicit val system: ActorSystem = ActorSystem("workflow-lib")
  implicit val executionContext: MessageDispatcher = system.dispatchers.lookup("default-dispatcher")

  val wf = new TestWorkflow(Empty(), None)
  wf.actor ! Message.NodeInit

  system.scheduler.schedule(1 second, 5 seconds, wf.actor, Message.Status)

  val ioDispatcher: MessageDispatcher = system.dispatchers.lookup("io-dispatcher")
  val defaultDispatcher: MessageDispatcher = system.dispatchers.lookup("default-dispatcher")

}

class SubWorkflow(val inputs: Empty,
                  val root: Option[Workflow[_]])(implicit val system: ActorSystem) extends Workflow[Empty] {

  def workflow(): Unit = {
    val echo: Echo = call(new Echo(Echo.Inputs("test"), Some(this)))
    val sleep: Sleep = call(new Sleep(Sleep.Inputs(echo.outputs.number), Some(this)))
  }

  class Outputs extends NodeOutputs
  val outputs = new Outputs

}

class TestWorkflow(val inputs: Empty,
                   val root: Option[Workflow[_]])(implicit val system: ActorSystem) extends Workflow[Empty] {

  def workflow(): Unit = {
    val echo: Echo = call(new Echo(Echo.Inputs("test"), Some(this)))
    val sleep: Sleep = call(new Sleep(Sleep.Inputs(echo.outputs.number), Some(this)))

    (1 until 10).foreach(x => call(new Sleep(Sleep.Inputs(echo.outputs.number), Some(this))))

    val sub = call(new SubWorkflow(Empty(), Some(this)))
  }

  class Outputs extends NodeOutputs
  val outputs = new Outputs

}
