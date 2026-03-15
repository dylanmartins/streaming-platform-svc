package stream

import cats.effect.{IO, Ref}
import cats.effect.std.Queue
import domain.{Event, ProcessingStats}
import fs2.Stream

import scala.concurrent.duration.*

object EventConsumer:

  private def process(event: Event): IO[Unit] =
    for
      _ <- IO.println(
        s"[consumer] received event ${event.id.value} of type ${event.eventType}, processing..."
      )
      // Added a sleep to simulate processing time.
      // NOTE: I'm using IO.sleep here to avoid blocking the thread, which allows other events to be processed concurrently.
      _ <- IO.sleep(2.seconds)
      _ <-
        if event.payload.contains("fail") then
          IO.raiseError(new RuntimeException(s"processing failed for event ${event.id.value}"))
        else IO.unit
      _ <- IO.println(s"[consumer] finished processing event ${event.id.value}")
    yield ()

  private def processSafely(
      event: Event,
      statsRef: Ref[IO, ProcessingStats]
  ): IO[Unit] =
    // NOTE: We wrap the `process` call in `handleErrorWith` to catch any exceptions that occur during processing.
    // This way, if one event fails to process, it won't crash the entire stream, and we can log the error instead.
    process(event).attempt.flatMap {
      // Updating the statsRef based on whether the processing succeeded or failed.
      case Right(_) =>
        statsRef.update(stats => stats.copy(processed = stats.processed + 1))

      case Left(error) =>
        statsRef.update(stats => stats.copy(failed = stats.failed + 1)) *>
          IO.println(
            s"[consumer] ERROR processing event ${event.id.value}: ${error.getMessage}"
          )
    }

  def stream(
      queue: Queue[IO, Event],
      statsRef: Ref[IO, ProcessingStats]
  ): Stream[IO, Unit] = {
    val maxParallelism = 5
    // This creates an fs2 Stream that continuously reads events from the queue.
    Stream
      .fromQueueUnterminated(queue)
      // NOTE: The `parEvalMap(maxParallelism)` means that up to X events can be processed concurrently.
      // NOTE: We pass the `statsRef` to `processSafely` so that it can update the processing statistics for each event, whether it succeeds or fails.
      .parEvalMap(maxParallelism)(event => processSafely(event, statsRef))
  }
