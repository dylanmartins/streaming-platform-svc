package stream

import cats.effect.IO
import cats.effect.std.Queue
import domain.Event
import fs2.Stream

import scala.concurrent.duration.*

object EventConsumer:

  def stream(queue: Queue[IO, Event]): Stream[IO, Unit] = {
    // This creates an fs2 Stream that continuously reads events from the queue.
    Stream
      .fromQueueUnterminated(queue)
      .evalMap { event =>
        for
          _ <- IO.println(
            s"[consumer] received event ${event.id.value} of type ${event.eventType}, processing..."
          )
          // Added a sleep to simulate processing time.
          // NOTE: I'm using IO.sleep here to avoid blocking the thread, which allows other events to be processed concurrently.
          _ <- IO.sleep(2.seconds)
          _ <- IO.println(s"[consumer] finished processing event ${event.id.value}")
        yield ()
      }
  }
