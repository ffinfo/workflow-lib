package workflow

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait Inputs {
  private val _allInputs: mutable.Map[String, Input[_]] = mutable.Map()

  protected def input[T](key: String, value: Future[T]): Input[T] = {
    if (_allInputs.contains(key)) throw new IllegalArgumentException(s"Input '$key' already exists")
    else {
      val i = Input(key, value)
      _allInputs += (key -> i)
      i
    }
  }

  protected def input[T](key: String): Input[T] = {
    if (_allInputs.contains(key)) throw new IllegalArgumentException(s"Input '$key' already exists")
    else {
      val i = new Input[T](key)
      _allInputs += (key -> i)
      i
    }
  }

  protected[workflow] lazy val future: Future[Iterable[Any]] = Future.sequence(_allInputs.values.map(_.value))
}
