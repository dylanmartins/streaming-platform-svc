package domain

final case class ProcessingStats(
    received: Long = 0,
    processed: Long = 0,
    failed: Long = 0
)
