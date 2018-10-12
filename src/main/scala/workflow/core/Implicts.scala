package workflow.core

import scala.concurrent.duration.Duration
import scala.concurrent.java8.FuturesConvertersImpl
import scala.concurrent.{Await, Future}

object Implicts {
//  implicit def inputString[T](input: Input[T]): String = Await.result(input.value, Duration.Inf).toString
  implicit def toFuture[T](input: T): Future[T] = Future.successful(input)
  implicit def toOption[T](input: T): Option[T] = Some(input)
  implicit def toOptionFuture[T](input: T): Option[Future[T]] = Some(input)
  implicit class FuturesS[T](f: Future[T]) {
    def s = Await.result(f, Duration.Inf)
  }
}
