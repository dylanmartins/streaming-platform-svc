package stream

import cats.effect.IO
import cats.effect.std.Queue
import domain.Event
import fs2.Stream

object EventConsumer:

  def stream(queue: Queue[IO, Event]): Stream[IO, Unit] = {
    // This creates an fs2 Stream that continuously reads events from the queue.
    Stream
      .fromQueueUnterminated(queue)
      .evalMap { event =>
        IO.println(s"[consumer] received event: $event")
      }
  }
