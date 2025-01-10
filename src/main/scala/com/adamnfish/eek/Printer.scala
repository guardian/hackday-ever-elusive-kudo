package com.adamnfish.eek

import cats.Show
import cats.effect.std.Console

trait Printer[F[_]] {
  def println[A](a: A)(implicit S: Show[A] = Show.fromToString[A]): F[Unit]
}

class ConsolePrinter[F[_] : Console] extends Printer[F] {
  override def println[A](a: A)(implicit S: Show[A] = Show.fromToString[A]): F[Unit] = {
    Console[F].println(a)
  }
}
object ConsolePrinter {
  def apply[F[_] : Console] = new ConsolePrinter[F]
}
