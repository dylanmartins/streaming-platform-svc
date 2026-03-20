package stream

import app.AppConfig
import cats.effect.{IO, Ref}
import domain.{DeadLetterEvent, Event, ProcessingStats, ValidationError}

import java.time.Instant
import scala.concurrent.duration.FiniteDuration

object EventProcessor:

  def validate(event: Event): Either[ValidationError, Event] =
    // NOTE: We use `Either` here to represent the possibility of validation failure.
    // NOTE: The `Option.when` is a convenient way to conditionally create an error message if a validation rule fails.
    // We then flatten the list to get rid of the `None` values.
    val errors = List(
      Option.when(event.eventType.trim.isEmpty)("eventType must not be empty"),
      Option.when(event.payload.trim.isEmpty)("payload must not be empty")
    ).flatten

    if errors.isEmpty then Right(event)
    else Left(ValidationError(event.id, errors))

  def process(
      event: Event,
      config: AppConfig
  ): IO[Unit] =
    for
      _ <- IO.println(
        s"[consumer] received valid event ${event.id.value} of type ${event.eventType}, processing..."
      )
      // Added a sleep to simulate processing time.
      // NOTE: I'm using IO.sleep here to avoid blocking the thread, which allows other events to be processed concurrently.
      _ <- IO.sleep(config.processingDelay)
      _ <-
        if event.payload.contains("fail") then
          IO.raiseError(new RuntimeException(s"processing failed for event ${event.id.value}"))
        else IO.unit
      _ <- IO.println(s"[consumer] finished processing event ${event.id.value}")
    yield ()

  def retry[A](
      ioa: IO[A],
      maxRetries: Int,
      delay: FiniteDuration
  ): IO[A] =
    ioa.handleErrorWith { error =>
      if maxRetries > 0 then
        IO.println(
          s"[consumer] retrying after error: ${error.getMessage}. retries left: $maxRetries"
        ) *>
          IO.sleep(delay) *>
          retry(ioa, maxRetries - 1, delay)
      else IO.raiseError(error)
    }

  def processSafely(
      event: Event,
      statsRef: Ref[IO, ProcessingStats],
      deadLetterRef: Ref[IO, Vector[DeadLetterEvent]],
      config: AppConfig
  ): IO[Unit] = {
    // NOTE: We wrap the `process` call in `handleErrorWith` to catch any exceptions that occur during processing.
    // This way, if one event fails to process, it won't crash the entire stream, and we can log the error instead.
    // NOTE: Invalid events never reach the `process` function!
    validate(event) match
      case Left(validationError) =>
        // NOTE: The `*>` operator is used to link together multiple IO actions,
        // where the result of the first action is ignored and only the result of the second action is returned.
        statsRef.update(stats => stats.copy(validationFailed = stats.validationFailed + 1)) *>
          IO.println(
            s"[consumer] VALIDATION ERROR for event ${validationError.eventId.value}: ${validationError.reasons.mkString(", ")}"
          )

      // We only retry valid events!
      case Right(validEvent) =>
        retry(
          ioa = process(validEvent, config),
          maxRetries = config.retryCount,
          delay = config.retryDelay
        ).attempt.flatMap {
          case Right(_) =>
            statsRef.update(stats => stats.copy(processed = stats.processed + 1))

          case Left(error) =>
            val deadLetter = DeadLetterEvent(
              event = validEvent,
              reason = error.getMessage,
              failedAt = Instant.now()
            )

            statsRef.update(stats => stats.copy(failed = stats.failed + 1)) *>
              deadLetterRef.update(_ :+ deadLetter) *>
              IO.println(
                s"[consumer] ERROR processing event ${validEvent.id.value} after retries. Moved to dead-letter store: ${error.getMessage}"
              )
        }
  }
