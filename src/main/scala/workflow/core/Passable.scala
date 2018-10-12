package workflow.core

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import workflow.core.Message.SetPassable

import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag

class Passable[T](val outputNode: Option[Node[_, _]])(implicit classTag: ClassTag[T]) extends WfActor {

  private var _value: Option[T] = None
  def value: T = {
    (_value, outputNode) match {
      case (Some(v), _) => v
      case (_, Some(o)) if o.status == Status.Done =>
        throw new IllegalStateException(s"Outputnode '${o.fullName}' is done but output is not set")
      case _ => throw new IllegalStateException("Value not set yet, this should not happen, please report this")
    }
  }
  def setValue(v: T): Unit = _value = Some(v)

  def isSet: Boolean = _value.isDefined

  private var inputNodes: ListBuffer[Node[_, _]] = ListBuffer()
  def addInputNode(node: Node[_, _]): Unit = inputNodes += node

  protected def createActor(system: ActorSystem): ActorRef = system.actorOf(Props(new Passable.PassableActor(this)))
}

object Passable {
  def apply[T](node: Node[_ <: Product, _ <: Product])(implicit classTag: ClassTag[T]): Passable[T] = {
    new Passable[T](Some(node))
  }

  def constant[T](value: T)(implicit classTag: ClassTag[T]): Passable[T] = {
    val p = new Passable[T](None)
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
    }
  }
}
