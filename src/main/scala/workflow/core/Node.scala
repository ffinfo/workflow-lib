package workflow.core

import java.io.File

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable}
import akka.dispatch.MessageDispatcher
import com.typesafe.config.{Config, ConfigFactory}
import workflow.core.backend.{Backend, Nohup}

import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag
import scala.concurrent.duration._
import scala.language.postfixOps

trait Node[Inputs <: Product] extends WfActor { node =>

  implicit val executionContext: MessageDispatcher = system.dispatchers.lookup("default-dispatcher")

  abstract class NodeOutputs {
    private val buffer: ListBuffer[Passable[_]] = ListBuffer()

    protected def file: Passable[File] = output(_ => new File(""))
    protected def string: Passable[String] = output(_ => "")
    protected def int: Passable[Int] = output(_ => 0)
    protected def long: Passable[Long] = output(_ => 10L)
    protected def double: Passable[Double] = output(_ => 0.0)
    protected def output[T](parser: Unit => T)(implicit classTag: ClassTag[T]): Passable[T] = {
      val p = Passable[T](node, parser)
      buffer += p
      p
    }

    def allOutputs: Iterator[Passable[_]] = buffer.iterator
  }

  type Outputs <: NodeOutputs

  val outputs: Outputs

  val inputs: Inputs

  private var _status: Status.Value = Status.Init
  final def status: Status.Value = _status
  final protected[core] def setStatus(s: Status.Value): Unit = _status = s

  def root: Option[Workflow[_]]

  def id: Long

  def name: String = this.getClass.getSimpleName + "-" + id

  def fullName: String = {
    path().map(_.name).mkString(",")
  }

  def path(p: List[Node[_]] = List(this)): List[Node[_]] = p.headOption.map(_.root) match {
    case Some(Some(x)) => path(x :: p)
    case _ => p
  }

  def key: String = this.getClass.getSimpleName

  def isDone: Boolean = status == Status.Done

  def passableInputs: Iterator[Passable[_]] = recursivePassable(inputs.productIterator)

  private def recursivePassable(list: Iterator[Any]): Iterator[Passable[_]] = list.flatMap { x =>
    x match {
      case x: Passable[_] => Iterator(x)
      case x: Product => recursivePassable(x.productIterator)
      case _ => Iterator()
    }
  }

  val config: Config = ConfigFactory.load()
  lazy val backend: Backend = {
    val key = if (config.hasPath("backend"))
      config.getString("backend")
    else "nohup"
    Backend.backends().get(key) match {
      case Some(b) => b
      case _ => throw new IllegalArgumentException(s"Backend '$key' does not exists")
    }
  }
}

object Node {
  trait NodeActor[T <: Node[_]] extends Actor with ActorLogging {

    val defaultDispatcher: MessageDispatcher = context.system.dispatchers.lookup("default-dispatcher")

    def node: T

    protected var cancellable: Option[Cancellable] = None

    def receive: Receive = {
      case Message.NodeInit if node.status == Status.Init || node.status == Status.Init =>
        context.system.scheduler.scheduleOnce(defaultWaitTime, self, Message.Init)(defaultDispatcher)
      case m =>
        log.error("Message not picked up: " + m + ", for: " + node.fullName + ", with status: " + node.status + ", from: " + context.sender())
    }
  }
}