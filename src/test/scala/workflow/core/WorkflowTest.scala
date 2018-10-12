package workflow.core

import akka.actor.ActorSystem
import org.testng.annotations.Test
import workflow.jobs.{Echo, Sleep}
import Implicts._

class WorkflowTest {
  @Test
  def test(): Unit = {

    val system: ActorSystem = ActorSystem("workflow-lib")

    val wf = TestWorkflow(Empty(), None)
    wf.loadActor(system)
    wf.actor ! Message.NodeInit

    Thread.sleep(10000)
  }
}

case class SubWorkflow(inputs: Empty,
                       root: Option[Workflow[_, _]]) extends Workflow[Empty, Empty] {

  def workflow(): Unit = {
    val echo: Echo = call(Echo(Echo.Inputs("test"), Some(this)))
    val sleep: Sleep = call(Sleep(Sleep.Inputs(3L), Some(this)))
  }
}

case class TestWorkflow(inputs: Empty,
                        root: Option[Workflow[_, _]]) extends Workflow[Empty, Empty] {

  def workflow(): Unit = {
    val echo: Echo = call(Echo(Echo.Inputs("test"), Some(this)))
    val sleep: Sleep = call(Sleep(Sleep.Inputs(3L), Some(this)))

    val sub = call(SubWorkflow(Empty(), Some(this)))
  }
}
