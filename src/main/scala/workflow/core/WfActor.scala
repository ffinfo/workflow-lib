package workflow.core

import akka.actor.{ActorRef, ActorSystem}

trait WfActor {
  implicit val system: ActorSystem

  private var _actor: Option[ActorRef] = None
  val actor: ActorRef

}
