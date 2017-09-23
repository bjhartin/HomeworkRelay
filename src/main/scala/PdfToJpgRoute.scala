package pdftojpg

import org.http4s._
import org.http4s.dsl._

object PdfToJpgRoute extends PingRoute

trait PdfToJpgRoute {
  val pdfToJpgRouteService = HttpService {
    case POST -> Root / "pdftojpg" => Ok("pong") // TODO - handle multipart and convert
  }
}