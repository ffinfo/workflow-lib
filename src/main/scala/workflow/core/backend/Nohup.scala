package workflow.core.backend

import akka.actor.ActorSystem
import workflow.core.CommandlineJob

class Nohup(implicit val system: ActorSystem) extends DispatchSharedFileSystem {

  def submitScript(job: CommandlineJob[_ <: Product]): String =
    s"(nohup bash ${scriptFile(job)} & echo $$!)"

  def pollScript: String =
    """
      |ps | sed "s/^ *//" | sed "s/ .*//" | grep -e "^\d*$"
    """.stripMargin
}
