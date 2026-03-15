package stream

import app.AppConfig
import cats.effect.{IO, Ref}
import domain.{DeadLetterEvent, Event, EventId, ProcessingStats}
import munit.CatsEffectSuite

import java.util.UUID
import scala.concurrent.duration.*

class EventProcessorSpec extends CatsEffectSuite:

  private val testConfig = AppConfig(
    queueCapacity = 10,
    maxParallelism = 2,
    processingDelay = 1.millis,
    retryCount = 2,
    retryDelay = 1.millis
  )

  private def event(
      eventType: String = "user-action",
      payload: String = "ok"
  ): Event =
    Event(
      id = EventId(UUID.randomUUID()),
      eventType = eventType,
      payload = payload,
      receivedAt = java.time.Instant.now()
    )

  test("validate returns Right for a valid event") {
    val e = event()
    IO {
      val result = EventProcessor.validate(e)
      assert(result.isRight)
    }
  }

  test("validate returns Left when eventType and payload are empty") {
    val e = event(eventType = "", payload = "")
    IO {
      val result = EventProcessor.validate(e)
      assert(result.isLeft)

      val error = result.swap.toOption.get
      assertEquals(error.reasons.length, 2)
      assert(error.reasons.contains("eventType must not be empty"))
      assert(error.reasons.contains("payload must not be empty"))
    }
  }

  test("retry eventually succeeds if the effect fails first and then succeeds") {
    for
      counter <- Ref.of[IO, Int](0)
      result <- EventProcessor.retry(
        ioa = counter.get.flatMap { current =>
          if current < 2 then counter.update(_ + 1) *> IO.raiseError(new RuntimeException("boom"))
          else IO.pure("success")
        },
        maxRetries = 3,
        delay = 1.millis
      )
    yield assertEquals(result, "success")
  }

  test("processSafely increments validationFailed for invalid events") {
    val invalidEvent = event(eventType = "", payload = "")

    for
      statsRef <- Ref.of[IO, ProcessingStats](ProcessingStats())
      deadLetterRef <- Ref.of[IO, Vector[DeadLetterEvent]](Vector.empty)
      _ <- EventProcessor.processSafely(invalidEvent, statsRef, deadLetterRef, testConfig)
      stats <- statsRef.get
      deadLetters <- deadLetterRef.get
    yield
      assertEquals(stats.validationFailed, 1L)
      assertEquals(stats.processed, 0L)
      assertEquals(stats.failed, 0L)
      assertEquals(deadLetters.size, 0)
  }

  test("processSafely increments processed for valid successful events") {
    val validEvent = event(payload = "all-good")

    for
      statsRef <- Ref.of[IO, ProcessingStats](ProcessingStats())
      deadLetterRef <- Ref.of[IO, Vector[DeadLetterEvent]](Vector.empty)
      _ <- EventProcessor.processSafely(validEvent, statsRef, deadLetterRef, testConfig)
      stats <- statsRef.get
      deadLetters <- deadLetterRef.get
    yield
      assertEquals(stats.processed, 1L)
      assertEquals(stats.failed, 0L)
      assertEquals(stats.validationFailed, 0L)
      assertEquals(deadLetters.size, 0)
  }

  test("processSafely moves permanently failing events to dead letters and increments failed") {
    val failingEvent = event(payload = "please-fail")

    for
      statsRef <- Ref.of[IO, ProcessingStats](ProcessingStats())
      deadLetterRef <- Ref.of[IO, Vector[DeadLetterEvent]](Vector.empty)
      _ <- EventProcessor.processSafely(failingEvent, statsRef, deadLetterRef, testConfig)
      stats <- statsRef.get
      deadLetters <- deadLetterRef.get
    yield
      assertEquals(stats.failed, 1L)
      assertEquals(stats.processed, 0L)
      assertEquals(stats.validationFailed, 0L)
      assertEquals(deadLetters.size, 1)
      assertEquals(deadLetters.head.event.id, failingEvent.id)
      assert(deadLetters.head.reason.contains("processing failed"))
  }
