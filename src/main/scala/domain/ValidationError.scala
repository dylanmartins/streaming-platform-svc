package domain

final case class ValidationError(
    eventId: EventId,
    reasons: List[String]
)
