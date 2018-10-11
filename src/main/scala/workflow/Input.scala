package workflow

import Implicts._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

case class Input[T](private var _value: Option[Future[T]] = None,
               configKey: Option[String] = None,
               default: Option[T] = None) {

  def :=(v: T): Unit = {
    if (_value.isEmpty) _value = Some(Future.successful(v))
    else throw new IllegalArgumentException(s"Input is already set")
  }

  def :=(v: Future[T]): Unit = {
    if (_value.isEmpty) _value = Some(v)
    else throw new IllegalArgumentException(s"Input is already set")
  }

  def value: Future[T] = (_value, configKey) match {
    case (Some(v), _) => v
    case (_, Some(key)) => ??? //TODO: add config
    case _ => throw new IllegalArgumentException(s"Input is not set")
  }

  def s: T = Await.result(value, Duration.Inf)
}

object Input {
  def apply[T](value: Future[T], configKey: String): Input[T] = {
    new Input[T](value, configKey)
  }

  def apply[T](value: T, configKey: String): Input[T] = {
    new Input[T](value, configKey)
  }

  def apply[T](value: Future[T]): Input[T] = {
    new Input[T](value)
  }

  def apply[T](value: T): Input[T] = {
    new Input[T](value)
  }
}