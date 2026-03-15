package stream

import cats.effect.IO
import cats.effect.std.Queue
import domain.Event
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
      _ <- IO.println(s"[consumer] finished processing event ${event.id.value}")
    yield ()

  def stream(queue: Queue[IO, Event]): Stream[IO, Unit] = {
    val maxParallelism = 5
    // This creates an fs2 Stream that continuously reads events from the queue.
    Stream
      .fromQueueUnterminated(queue)
      // NOTE: The `parEvalMap(maxParallelism)` means that up to X events can be processed concurrently.
      .parEvalMap(maxParallelism)(process)
  }
