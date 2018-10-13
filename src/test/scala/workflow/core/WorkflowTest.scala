package workflow.core

import akka.actor.ActorSystem
import org.testng.annotations.Test
import workflow.jobs.{Echo, Sleep}
import Implicts._

class WorkflowTest {
  @Test
  def test(): Unit = {

    implicit val system: ActorSystem = ActorSystem("workflow-lib")

    val wf = TestWorkflow(Empty(), None)
    wf.actor ! Message.NodeInit

    Thread.sleep(100000)
  }
}

case class SubWorkflow(inputs: Empty,
                       root: Option[Workflow[_]])(implicit val system: ActorSystem) extends Workflow[Empty] {

  def workflow(): Unit = {
    val echo: Echo = call(Echo(Echo.Inputs("test"), Some(this)))
    val sleep: Sleep = call(Sleep(Sleep.Inputs(echo.outputs.number), Some(this)))
  }

  class Outputs extends NodeOutputs {
    val number = long
  }
  val outputs = new Outputs

}

case class TestWorkflow(inputs: Empty,
                        root: Option[Workflow[_]])(implicit val system: ActorSystem) extends Workflow[Empty] {

  def workflow(): Unit = {
    val echo: Echo = call(Echo(Echo.Inputs("test"), Some(this)))
    val sleep: Sleep = call(Sleep(Sleep.Inputs(echo.outputs.number), Some(this)))

    val sub = call(SubWorkflow(Empty(), Some(this)))
  }

  class Outputs extends NodeOutputs {
    val number = long
  }
  val outputs = new Outputs

}
