package stream

import app.AppConfig
import cats.effect.{IO, Ref}
import cats.effect.std.Queue
import domain.{DeadLetterEvent, Event, ProcessingStats}
import fs2.Stream

object EventConsumer:

  def stream(
      queue: Queue[IO, Event],
      statsRef: Ref[IO, ProcessingStats],
      deadLetterRef: Ref[IO, Vector[DeadLetterEvent]],
      config: AppConfig
  ): Stream[IO, Unit] =
    Stream
      .fromQueueUnterminated(queue)
      // NOTE: The `parEvalMap(maxParallelism)` means that up to X events can be processed concurrently.
      // NOTE: We pass the `statsRef` to `processSafely` so that it can update the processing statistics for each event, whether it succeeds or fails.
      .parEvalMap(config.maxParallelism)(event => EventProcessor.processSafely(event, statsRef, deadLetterRef, config))
