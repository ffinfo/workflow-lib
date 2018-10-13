package workflow.core

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import workflow.core.Message.SetPassable

import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag

class Passable[T](val outputNode: Option[Node[_]], parser: Option[Unit => T])(implicit classTag: ClassTag[T], val system: ActorSystem) extends WfActor {

  private var _value: Option[T] = None
  def value: T = {
    (_value, outputNode) match {
      case (Some(v), _) => v
      case (_, Some(o)) if o.status == Status.Done =>
        throw new IllegalStateException(s"Outputnode '${o.fullName}' is done but output is not set")
      case _ => throw new IllegalStateException("Value not set yet, this should not happen, please report this")
    }
  }
  protected def setValue(v: T): Unit = {
    outputNode.foreach(_.actor ! Message.ProcessOutputDone)
    _value = Some(v)
  }

  def isSet: Boolean = _value.isDefined

  private var inputNodes: ListBuffer[Node[_]] = ListBuffer()
  def addInputNode(node: Node[_]): Unit = inputNodes += node

  private def parse(): T = {
    parser match {
      case Some(p) => p()
      case _ => throw new IllegalStateException("No parser defined, is this a constant?")
    }
  }

  val actor: ActorRef = system.actorOf(Props(new Passable.PassableActor(this)))
}

object Passable {
  def apply[T](node: Node[_  <: Product], parser: Unit => T)(implicit classTag: ClassTag[T], actorSystem: ActorSystem): Passable[T] = {
    new Passable[T](Some(node), Some(parser))
  }

  def constant[T](value: T)(implicit classTag: ClassTag[T], actorSystem: ActorSystem): Passable[T] = {
    val p = new Passable[T](None, None)
    p.setValue(value)
    p
  }

  class PassableActor[T](passable: Passable[T])(implicit classTag: ClassTag[T]) extends Actor with ActorLogging {
    def receive: Receive = {
      case SetPassable(value) =>
        value match {
          case classTag(v) => passable.setValue(v)
          case _ => throw new IllegalStateException("Wrong type")
        }
        passable.inputNodes.foreach(_.actor ! Message.CheckInputs)
      case Message.ProcessOutputs if !passable.isSet =>
        self ! SetPassable(passable.parse())
    }
  }
}
