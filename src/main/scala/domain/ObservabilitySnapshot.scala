package domain

final case class ObservabilitySnapshot(
    queueSize: Int,
    queueCapacity: Int,
    stats: ProcessingStats
)
