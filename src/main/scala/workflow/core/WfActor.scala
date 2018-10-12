package workflow.core

import akka.actor.{ActorRef, ActorSystem}

trait WfActor {
  private var _system: Option[ActorSystem] = None
  def system: ActorSystem = {
    _system match {
      case Some(a) => a
      case _ => throw new IllegalStateException("actor does not exist yet")
    }
  }

  private var _actor: Option[ActorRef] = None
  def actor: ActorRef = {
    _actor match {
      case Some(a) => a
      case _ => throw new IllegalStateException("actor does not exist yet")
    }
  }
  protected def createActor(system: ActorSystem): ActorRef
  def loadActor(s: ActorSystem): Unit = {
    if (_actor.isDefined) throw  new IllegalStateException("Actor does already exist")
    _system = Some(s)
    _actor = Some(createActor(s))
    actor.path
  }

}
