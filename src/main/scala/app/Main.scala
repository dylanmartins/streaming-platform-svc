package app

import api.HttpApi
import cats.effect.{IO, IOApp}
import cats.effect.std.Queue
import com.comcast.ip4s.*
import domain.Event
import org.http4s.ember.server.EmberServerBuilder
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp.Simple:

  private given Logger[IO] = Slf4jLogger.getLogger[IO]

  def run: IO[Unit] =
    for
      queue <- Queue.bounded[IO, Event](100)
      _ <- EmberServerBuilder
        .default[IO]
        .withHost(host"0.0.0.0")
        .withPort(port"8080")
        .withHttpApp(HttpApi.routes(queue).orNotFound)
        .build
        .useForever
    yield ()
