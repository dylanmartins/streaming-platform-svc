package api

import app.AppConfig
import cats.effect.IO
import cats.effect.Ref
import cats.effect.std.Queue
import domain.{DeadLetterEvent, Event, EventId, ObservabilitySnapshot, ProcessingStats}
import org.http4s.HttpRoutes
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import java.time.Instant
import java.util.UUID

object HttpApi:

  def routes(
      queue: Queue[IO, Event],
      statsRef: Ref[IO, ProcessingStats],
      deadLetterRef: Ref[IO, Vector[DeadLetterEvent]],
      config: AppConfig
  ): HttpRoutes[IO] =
    val endpointDescriptions = List(
      Endpoints.health,
      Endpoints.ingestEvent,
      Endpoints.stats,
      Endpoints.observability,
      Endpoints.deadLetters
    )

    val healthServerEndpoint: ServerEndpoint[Any, IO] =
      Endpoints.health.serverLogicSuccess[IO](_ => IO.pure("ok"))

    val ingestEventServerEndpoint: ServerEndpoint[Any, IO] =
      Endpoints.ingestEvent.serverLogicSuccess[IO] { request =>
        // Create a new Event with a generated UUID and the current timestamp
        val event = Event(
          id = EventId(UUID.randomUUID()),
          eventType = request.eventType,
          payload = request.payload,
          receivedAt = Instant.now()
        )

        // Enqueue the event and return its ID
        // NOTE: Here `offer` means we are using "blocking backpressure"
        // - if the queue is full, this will wait until there is space
        queue.offer(event) *>
          statsRef.update(stats => stats.copy(received = stats.received + 1)) *>
          IO.pure(event.id)
      }

    val statsServerEndpoint: ServerEndpoint[Any, IO] =
      Endpoints.stats.serverLogicSuccess[IO](_ => statsRef.get)

    val observabilityServerEndpoint: ServerEndpoint[Any, IO] =
      Endpoints.observability.serverLogicSuccess[IO] { _ =>
        for
          currentQueueSize <- queue.size
          stats <- statsRef.get
        yield ObservabilitySnapshot(
          queueSize = currentQueueSize,
          queueCapacity = config.queueCapacity,
          stats = stats
        )
      }

    val deadLettersServerEndpoint: ServerEndpoint[Any, IO] =
      Endpoints.deadLetters.serverLogicSuccess[IO] { _ =>
        deadLetterRef.get.map(_.toList)
      }

    val businessServerEndpoints: List[ServerEndpoint[Any, IO]] =
      List(
        healthServerEndpoint,
        ingestEventServerEndpoint,
        statsServerEndpoint,
        observabilityServerEndpoint,
        deadLettersServerEndpoint
      )

    val docsServerEndpoints: List[ServerEndpoint[Any, IO]] =
      SwaggerInterpreter()
        .fromEndpoints[IO](endpointDescriptions, "Streaming Platform Project", "0.1")

    Http4sServerInterpreter[IO]()
      .toRoutes(businessServerEndpoints ++ docsServerEndpoints)
