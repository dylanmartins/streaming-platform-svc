package app

import scala.concurrent.duration.*

final case class AppConfig(
    queueCapacity: Int,
    maxParallelism: Int,
    processingDelay: FiniteDuration,
    retryCount: Int,
    retryDelay: FiniteDuration
)

object AppConfig:
  // NOTE: This can come from a configuration file, environment variables, or command-line arguments in a real application...
  val default: AppConfig =
    AppConfig(
      queueCapacity = 100,
      maxParallelism = 5,
      processingDelay = 2.seconds,
      retryCount = 2,
      retryDelay = 1.second
    )
