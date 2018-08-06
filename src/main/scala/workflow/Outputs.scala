package workflow

import scala.concurrent.Future
import scala.collection.mutable

trait Outputs {

  private val _allOutputs: mutable.Map[String, Future[_]] = mutable.Map()

  protected def output[T](key: String, value: Future[T]): Future[T] = {
    if (_allOutputs.contains(key)) throw new IllegalArgumentException(s"Output '$key' already exists")
    else _allOutputs += (key -> value)
    value
  }

  def allDone: Boolean = _allOutputs.values.forall(_.isCompleted)
}
