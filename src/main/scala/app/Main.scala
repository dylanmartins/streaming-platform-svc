package app

import api.HttpApi
import cats.effect.{IO, IOApp, Ref}
import cats.effect.std.Queue
import com.comcast.ip4s.*
import domain.{DeadLetterEvent, Event, ProcessingStats}
import org.http4s.ember.server.EmberServerBuilder
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import stream.EventConsumer

object Main extends IOApp.Simple:

  private given Logger[IO] = Slf4jLogger.getLogger[IO]

  def run: IO[Unit] =
    val config = AppConfig.default

    for
      // This creates a bounded queue in memory that can hold up to 100 events.
      // NOTE: A bounded queue means that if the queue is full, producers will be suspended until there is space available.
      // This helps to prevent memory overflow and allows for backpressure in the system.
      // NOTE: For local environment it's fine but in production,
      // we would want to use something like Kafka or RabbitMQ instead of an in-memory queue.
      queue <- Queue.bounded[IO, Event](config.queueCapacity)
      // This creates a Ref that will hold the processing statistics.
      // NOTE: A Ref is a mutable reference that can be safely shared across multiple fibers (lightweight threads) in a concurrent environment.
      statsRef <- Ref.of[IO, ProcessingStats](ProcessingStats())
      // NOTE: The Ref is used again here to store the list of dead letter events.
      // This allows us to keep track of events that failed processing after all retries.
      deadLetterRef <- Ref.of[IO, Vector[DeadLetterEvent]](Vector.empty)
      // Start the event consumer in the background. This will continuously read events from the queue and print them.
      _ <- EventConsumer
        .stream(queue, statsRef, deadLetterRef, config)
        .compile
        .drain
        .start
      // Start the HTTP server. This will listen for incoming HTTP requests and handle them using the routes defined in HttpApi.
      _ <- EmberServerBuilder
        .default[IO]
        .withHost(host"0.0.0.0")
        .withPort(port"8080")
        .withHttpApp(HttpApi.routes(queue, statsRef, deadLetterRef, config).orNotFound)
        .build
        .useForever
    yield ()
