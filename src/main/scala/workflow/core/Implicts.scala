package workflow.core

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.reflect.ClassTag

object Implicts {
//  implicit def inputString[T](input: Input[T]): String = Await.result(input.value, Duration.Inf).toString
  implicit def toPassable[T](input: T)(implicit classTag: ClassTag[T]): Passable[T] = Passable.constant(input)
  implicit def toFuture[T](input: T): Future[T] = Future.successful(input)
  implicit def toOption[T](input: T): Option[T] = Some(input)
  implicit def toOptionFuture[T](input: T): Option[Future[T]] = Some(input)
  implicit class FuturesS[T](f: Future[T]) {
    def s = Await.result(f, Duration.Inf)
  }
}
