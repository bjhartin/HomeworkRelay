package pdftojpg

import scalaz.\/
import scalaz.concurrent.Task

object TaskUtils {
  def taskFromUnsafe[T](t: => T): Task[T] =
    Task.fromDisjunction(\/.fromTryCatchNonFatal(t))
}
