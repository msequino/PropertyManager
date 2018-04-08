package utils

import scala.concurrent.Future

/**
  * created by lukaci on 29/11/2017.
  * copyright by atscom.it, all rights reserved.
  */

object FutureUtils {
  def notSatisfy(predicate: => Boolean)(exc: => Throwable): Boolean = if(predicate) true else throw exc
  def satisfy(predicate: => Boolean)(exc: => Throwable): Boolean = if(predicate) throw exc else true
  implicit class asSuccessful[T](value: T) {
    def successful: Future[T] = Future.successful(value)
  }
  implicit class asFailed[T](exception: Throwable) {
    def failed: Future[T] = Future.failed(exception)
  }
}
