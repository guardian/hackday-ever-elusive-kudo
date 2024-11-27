package com.adamnfish.tdc.testutils

import munit.Suite

import scala.concurrent.Future
import munit.internal.FutureCompat.*

import scala.util.Try
import munit.internal.console.StackTraces

trait DescribeValueTransforms { this: Suite =>

  final class ValueTransform(
    val name: String,
    fn: PartialFunction[Any, Future[Any]]
  ) extends Function1[Any, Option[Future[Any]]] {
    def apply(v1: Any): Option[Future[Any]] = fn.lift(v1)
  }

  def munitValueTransforms: List[ValueTransform] =
    List(
      munitFutureTransform
    )

  final def munitValueTransform(testValue: => Any): Future[Any] = {
    // Takes an arbitrarily nested future `Future[Future[Future[...]]]` and
    // returns a `Future[T]` where `T` is not a `Future`.
    def flattenFuture(future: Future[?]): Future[?] = {
      val nested: Future[Future[Any]] = future.map { value =>
        val transformed = munitValueTransforms.iterator
          .map(fn => fn(value))
          .collectFirst { case Some(future) => future }
        transformed match {
          case Some(f) => flattenFuture(f)
          case None    => Future.successful(value)
        }
      }(munitExecutionContext)
      nested.flattenCompat(munitExecutionContext)
    }
    val wrappedFuture = Future.fromTry(Try(StackTraces.dropOutside(testValue)))
    flattenFuture(wrappedFuture)
  }

  final def munitFutureTransform: ValueTransform =
    new ValueTransform("Future", { case e: Future[_] => e })
}
