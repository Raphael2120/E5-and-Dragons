import cats.effect.{IO, IOApp}
import http.Server

object Main extends IOApp.Simple:
  override def run: IO[Unit] = Server.run
