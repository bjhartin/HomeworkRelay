package pdftojpg

import org.http4s._
import org.http4s.dsl._

object PingRoute extends PingRoute

trait PingRoute {
  val pingRouteService = HttpService {
    case GET -> Root / "ping" => Ok("pong")
  }
}
