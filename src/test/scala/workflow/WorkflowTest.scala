package workflow

import nl.biopet.test.BiopetTest
import org.testng.annotations.Test

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class WorkflowTest extends BiopetTest {
  @Test
  def test(): Unit = {

    implicit def inputString[T](input: Input[T]): String = Await.result(input.value, Duration.Inf).toString

    class Echo extends CommandlineJob {

      def key: String = "test"

      object inputs extends Inputs {
        val text: Input[String] = input("text")
      }

      def cmd: Seq[String] = Seq("echo", inputs.text)

      object outputs extends Outputs
    }

    val workflow = new Workflow {
      def key: String = "workflow1"

      object inputs extends Inputs {
        val arg1 = input("arg1", Future.successful("test"))
      }

      val echo: Echo = call(new Echo)
      echo.inputs.text := this.inputs.arg1

      object outputs extends Outputs
    }

    Await.result(workflow.future, Duration.Inf)
  }
}
