package workflow

import scala.concurrent.Future

class Input[T](key: String) {
  private var _value: Option[Future[T]] = None

  def :=(v: T): Unit = {
    if (_value.isEmpty) _value = Some(Future.successful(v))
    else throw new IllegalArgumentException(s"Input '$key' is already set")
  }

  def :=(v: Future[T]): Unit = {
    if (_value.isEmpty) _value = Some(v)
    else throw new IllegalArgumentException(s"Input '$key' is already set")
  }

  def value: Future[T] = _value.getOrElse(throw new IllegalArgumentException(s"Input '$key' is not set"))
}

object Input {
  def apply[T](key: String, value: Future[T]): Input[T] = {
    val i = new Input[T](key)
    i := value
    i
  }

  def apply[T](key: String, value: T): Input[T] = {
    val i = new Input[T](key)
    i := value
    i
  }
}