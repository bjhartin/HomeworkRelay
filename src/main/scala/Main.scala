package pdftojpg

import com.codahale.metrics.MetricRegistry
import com.typesafe.config.ConfigFactory
import org.http4s.HttpService
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.metrics.Metrics
import org.http4s.server.{Server, ServerApp}

import scalaz.concurrent.Task

object Main extends ServerApp {
  override def server(args: List[String]): Task[Server] =
    for {
      c   <- loadConfig
      mr   =  new MetricRegistry()
      svc =  service(mr)
      b    <- startBlazeServer(c, svc)
      _    <- printStartMessage
    } yield b

  def startBlazeServer(config: HttpConfig, service: HttpService): Task[Server] = BlazeBuilder
    .bindHttp(config.port, config.host)
    .mountService(service, "/")
    .start

  def service(metricRegistry: MetricRegistry) = Metrics(metricRegistry)(PingRoute.pingRouteService)

  def printStartMessage = Task.delay(println("Started!"))

  val loadConfig: Task[HttpConfig] = Task.delay {
    val config = ConfigFactory.load("application.conf")
    pureconfig.loadConfigOrThrow[HttpConfig](config, "app.http")
  }
}
