package domain

import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema

import java.time.Instant
import java.util.UUID

// In scala adding AnyVal tells the compiler to treat this class as
// just the raw underlying value (the UUID) at runtime. (Optimization)
final case class EventId(value: UUID) extends AnyVal

object EventId:
  given Encoder[EventId] =
    Encoder.encodeUUID.contramap(_.value)

  given Decoder[EventId] =
    Decoder.decodeUUID.map(EventId(_))

  given Schema[EventId] =
    Schema.schemaForUUID.map(uuid => Some(EventId(uuid)))(_.value)

final case class Event(
    id: EventId,
    eventType: String,
    payload: String,
    receivedAt: Instant
)
