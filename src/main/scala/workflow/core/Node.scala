package workflow.core

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Cancellable}

import scala.concurrent.Future

trait Node[Inputs <: Product, Outputs <: Product] extends WfActor {

  def inputs: Inputs
  private var _outputs: Option[Outputs] = None
  final protected def setOutputs(o: Outputs): Unit = _outputs = Some(o)
  def outputs: Outputs = {
    _outputs match {
      case Some(o) => o
      case _ if status != Status.Done => throw new IllegalStateException(s"'$fullName' is not yet done")
      case _ => throw new IllegalStateException("This should not happen, please report this")
    }
  }
  private var _status: Status.Value = Status.Init
  final def status: Status.Value = _status
  final protected def setStatus(s: Status.Value): Unit = _status = s

  def root: Option[Workflow[_, _]]

  def id: Long

  def name: String = this.getClass.getSimpleName + "-" + id

  def fullName: String = {
    path().map(_.name).mkString(",")
  }

  def path(p: List[Node[_, _]] = List(this)): List[Node[_, _]] = p.headOption.map(_.root) match {
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
}

object Node {
  trait NodeActor[T <: Node[_, _]] extends Actor with ActorLogging {

    def node: T

    protected var cancellable: Option[Cancellable] = None

    def receive: Receive = {
      case Message.NodeInit if node.status == Status.Init || node.status == Status.Init =>
        self ! Message.Init
    }
  }
}