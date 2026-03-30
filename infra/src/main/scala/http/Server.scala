package http

import cats.effect.{IO, Resource}
import com.comcast.ip4s.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.{CORS, Logger}
import org.http4s.{HttpApp, HttpRoutes}
import cats.effect.std.Console

object Server:
  def run: IO[Unit] =
    resources.useForever

  private def resources: Resource[IO, org.http4s.server.Server] =
    for
      sessionService <- Resource.eval(GameSessionService.create())
      historyService <- Resource.eval(GameHistoryService.create())
      routes          = GameRoutes(sessionService, historyService).routes
      corsRoutes      = CORS.policy
        .withAllowOriginAll
        .withAllowMethodsAll
        .withAllowHeadersAll
        .apply(routes)
      logged          = Logger.httpApp(logHeaders = false, logBody = false)(corsRoutes.orNotFound)
      server         <- EmberServerBuilder
        .default[IO]
        .withHost(ipv4"0.0.0.0")
        .withPort(port"8080")
        .withHttpApp(logged)
        .build
      _ <- Resource.eval(IO.println("🎮 E5 & Dragons API running on http://0.0.0.0:8080"))
    yield server
