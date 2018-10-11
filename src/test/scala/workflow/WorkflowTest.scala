package workflow

import nl.biopet.test.BiopetTest
import org.testng.annotations.Test

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import Implicts._
import workflow.jobs.Echo

class WorkflowTest extends BiopetTest {
  @Test
  def test(): Unit = {

    case class SubWorkflow(inputs: (String, String),
                           outputs: (String, String),
                           root: Option[Workflow] = None) extends Workflow {

      def workflow(): Unit = {
        val echo: Echo = call(Echo(Echo.Inputs("test"), Echo.Outputs(), this))
      }
    }

    case class TestWorkflow(inputs: (String, String),
                       outputs: (String, String),
                       root: Option[Workflow] = None) extends Workflow {

      def workflow(): Unit = {
        val echo: Echo = call(Echo(Echo.Inputs("test"), Echo.Outputs(), this))

        val sub = call(SubWorkflow(("", ""), ("", ""), this))
      }
    }

    val wf = TestWorkflow(("", ""), ("", ""))
    Await.result(wf.start, Duration.Inf)
  }
}
