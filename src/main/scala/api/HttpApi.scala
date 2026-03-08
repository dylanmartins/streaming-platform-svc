package api

import cats.effect.IO
import cats.effect.std.Queue
import domain.{Event, EventId}
import org.http4s.HttpRoutes
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import java.time.Instant
import java.util.UUID

object HttpApi:

  def routes(queue: Queue[IO, Event]): HttpRoutes[IO] =
    val endpointDescriptions = List(
      Endpoints.health,
      Endpoints.ingestEvent
    )

    val healthServerEndpoint: ServerEndpoint[Any, IO] =
      Endpoints.health.serverLogicSuccess[IO] { _ =>
        IO.pure("ok")
      }

    val ingestEventServerEndpoint: ServerEndpoint[Any, IO] =
      Endpoints.ingestEvent.serverLogicSuccess[IO] { request =>
        // Create a new Event with a generated UUID and the current timestamp
        val event = Event(
          id = EventId(UUID.randomUUID()),
          eventType = request.eventType,
          payload = request.payload,
          receivedAt = Instant.now()
        )

        // 1. Enqueue the event and return its ID
        // here `offer` means we are using "blocking backpressure"
        // - if the queue is full, this will wait until there is space
        // 2. The `.as(event.id)` means that after the event is successfully enqueued,
        // we return the event's ID as the response
        queue.offer(event).as(event.id)
      }

    val businessServerEndpoints: List[ServerEndpoint[Any, IO]] =
      List(
        healthServerEndpoint,
        ingestEventServerEndpoint
      )

    val docsServerEndpoints: List[ServerEndpoint[Any, IO]] =
      SwaggerInterpreter()
        .fromEndpoints[IO](endpointDescriptions, "Streaming Platform Project", "0.1")

    Http4sServerInterpreter[IO]()
      .toRoutes(businessServerEndpoints ++ docsServerEndpoints)
