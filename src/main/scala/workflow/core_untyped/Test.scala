package workflow.core_untyped

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior, DispatcherSelector, Terminated }

object Test extends App {
  val system: ActorSystem[HelloWorldMain.Start] =
    ActorSystem(HelloWorldMain.main, "hello")

  system ! HelloWorldMain.Start("World")
  //system ! HelloWorldMain.Start("Akka")
}

object HelloWorld {
  final case class Greet(whom: String, replyTo: ActorRef[Greeted])
  final case class Greeted(whom: String, from: ActorRef[Greet])

  val greeter: Behavior[Greet] = Behaviors.receive { (ctx, msg) ⇒
    println(s"Hello ${msg.whom}!")
    msg.replyTo ! Greeted(msg.whom, ctx.self)
    Behaviors.same
  }
}

object HelloWorldBot {

  def bot(greetingCounter: Int, max: Int): Behavior[HelloWorld.Greeted] =
    Behaviors.receive { (ctx, msg) ⇒
      val n = greetingCounter + 1
      println(s"Greeting ${n} for ${msg.whom}")
      if (n == max) {
        Behaviors.stopped
      } else {
        msg.from ! HelloWorld.Greet(msg.whom, ctx.self)
        bot(n, max)
      }
    }
}

object HelloWorldMain {

  case class Start(name: String)

  val main: Behavior[Start] =
    Behaviors.setup { context ⇒
      val greeter = context.spawn(HelloWorld.greeter, "greeter")

      Behaviors.receiveMessage { msg ⇒
        val replyTo = context.spawn(HelloWorldBot.bot(greetingCounter = 0, max = 2), msg.name)
        greeter ! HelloWorld.Greet(msg.name, replyTo)
        Behaviors.same
      }
    }
}
