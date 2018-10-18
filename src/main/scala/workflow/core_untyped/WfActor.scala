package workflow.core_untyped

import akka.actor.{ActorRef, ActorSystem}
import akka.dispatch.MessageDispatcher

trait WfActor {
  implicit val system: ActorSystem

  private var _actor: Option[ActorRef] = None
  val actor: ActorRef

  val defaultDispatcher: MessageDispatcher = system.dispatchers.lookup("default-dispatcher")

}
