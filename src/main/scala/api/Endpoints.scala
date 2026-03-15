package api

import domain.{DeadLetterEvent, EventId, ObservabilitySnapshot, ProcessingStats}
import io.circe.generic.auto.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

final case class IngestEventRequest(
    eventType: String,
    payload: String
)

object Endpoints:
  val health: PublicEndpoint[Unit, Unit, String, Any] =
    endpoint.get
      .in("health")
      .out(stringBody)

  val ingestEvent: PublicEndpoint[IngestEventRequest, String, EventId, Any] =
    endpoint.post
      .in("events")
      .description("Ingest a new event with a type and payload. Returns the generated event ID.")
      .in(jsonBody[IngestEventRequest])
      .errorOut(stringBody)
      .out(jsonBody[EventId])

  val stats: PublicEndpoint[Unit, Unit, ProcessingStats, Any] =
    endpoint.get
      .in("stats")
      .description("Get processing statistics for received, processed, failed, and validation failed events.")
      .out(jsonBody[ProcessingStats])

  val observability: PublicEndpoint[Unit, Unit, ObservabilitySnapshot, Any] =
    endpoint.get
      .in("observability")
      .description("Get an observability snapshot including queue size, capacity, and processing stats.")
      .out(jsonBody[ObservabilitySnapshot])

  val deadLetters: PublicEndpoint[Unit, Unit, List[DeadLetterEvent], Any] =
    endpoint.get
      .in("dead-letters")
      .description("Get a list of events that failed processing after all retries (dead letters).")
      .out(jsonBody[List[DeadLetterEvent]])
