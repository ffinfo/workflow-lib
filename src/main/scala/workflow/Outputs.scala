package workflow

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable

trait Outputs {

  private val _allOutputs: mutable.Map[String, Future[Any]] = mutable.Map()

  protected def output[T](key: String, value: Future[T]): Future[T] = {
    if (_allOutputs.contains(key)) throw new IllegalArgumentException(s"Output '$key' already exists")
    else _allOutputs += (key -> value)
    value
  }

  protected[workflow] lazy val future: Future[_] = Future.sequence(_allOutputs.values)
}
