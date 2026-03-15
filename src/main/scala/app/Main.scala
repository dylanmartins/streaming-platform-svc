package app

import api.HttpApi
import cats.effect.{IO, IOApp}
import cats.effect.std.Queue
import com.comcast.ip4s.*
import domain.Event
import org.http4s.ember.server.EmberServerBuilder
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import stream.EventConsumer

object Main extends IOApp.Simple:

  private given Logger[IO] = Slf4jLogger.getLogger[IO]

  def run: IO[Unit] =
    for
      // This creates a bounded queue in memory that can hold up to 100 events.
      // NOTE: For local environment it's fine but in production,
      // we would want to use something like Kafka or RabbitMQ instead of an in-memory queue.
      queue <- Queue.bounded[IO, Event](100)
      // Start the event consumer in the background. This will continuously read events from the queue and print them.
      _ <- EventConsumer
        .stream(queue)
        .compile
        .drain
        .start
      // Start the HTTP server. This will listen for incoming HTTP requests and handle them using the routes defined in HttpApi.
      _ <- EmberServerBuilder
        .default[IO]
        .withHost(host"0.0.0.0")
        .withPort(port"8080")
        .withHttpApp(HttpApi.routes(queue).orNotFound)
        .build
        .useForever
    yield ()
