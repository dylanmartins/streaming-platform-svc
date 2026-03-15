package api

import domain.{EventId, ObservabilitySnapshot, ProcessingStats}
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
      .in(jsonBody[IngestEventRequest])
      .errorOut(stringBody)
      .out(jsonBody[EventId])

  val stats: PublicEndpoint[Unit, Unit, ProcessingStats, Any] =
    endpoint.get
      .in("stats")
      .out(jsonBody[ProcessingStats])

  val observability: PublicEndpoint[Unit, Unit, ObservabilitySnapshot, Any] =
    endpoint.get
      .in("observability")
      .out(jsonBody[ObservabilitySnapshot])
