package domain

import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema

import java.time.Instant
import java.util.UUID

final case class EventId(value: UUID) extends AnyVal

object EventId:
  given Encoder[EventId] =
    Encoder.encodeUUID.contramap(_.value)

  given Decoder[EventId] =
    Decoder.decodeUUID.map(EventId(_))

  given Schema[EventId] =
    Schema.schemaForUUID.map(uuid => Some(EventId(uuid)))(_.value)

// TODO: I could implement a real world event, having a proper object for the payload with ENUM and more validation
final case class Event(
    id: EventId,
    eventType: String,
    payload: String,
    receivedAt: Instant
)
