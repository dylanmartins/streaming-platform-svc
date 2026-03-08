package api

import domain.EventId
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
