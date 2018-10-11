package workflow

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.language.implicitConversions

object Implicts {
  implicit def inputString[T](input: Input[T]): String = Await.result(input.value, Duration.Inf).toString
  implicit def toFuture[T](input: T): Future[T] = Future.successful(input)
  implicit def toOption[T](input: T): Option[T] = Some(input)
  implicit def toOptionFuture[T](input: T): Option[Future[T]] = Some(input)
}
