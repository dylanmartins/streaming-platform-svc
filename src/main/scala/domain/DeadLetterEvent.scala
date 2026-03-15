package domain

import java.time.Instant

final case class DeadLetterEvent(
    event: Event,
    reason: String,
    failedAt: Instant
)
