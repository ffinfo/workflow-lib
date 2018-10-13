package workflow.core

import java.io.File

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable}

import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag

trait Node[Inputs <: Product] extends WfActor { node =>

  abstract class NodeOutputs {
    private val buffer: ListBuffer[Passable[_]] = ListBuffer()

    protected def file: Passable[File] = output(_ => new File(""))
    protected def string: Passable[String] = output(_ => "")
    protected def int: Passable[Int] = output(_ => 0)
    protected def long: Passable[Long] = output(_ => 0L)
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
  final protected def setStatus(s: Status.Value): Unit = _status = s

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
}

object Node {
  trait NodeActor[T <: Node[_]] extends Actor with ActorLogging {

    def node: T

    protected var cancellable: Option[Cancellable] = None

    def receive: Receive = {
      case Message.NodeInit if node.status == Status.Init || node.status == Status.Init =>
        self ! Message.Init
    }
  }
}