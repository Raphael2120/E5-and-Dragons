package errors

sealed trait MapError extends Throwable

object MapError:
  final case class IllegalMapFormat() extends MapError
