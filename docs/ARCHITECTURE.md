# System Architecture

This document describes the architecture of the Streaming Platform service.

---

# High-Level Overview

The service ingests events via an HTTP API and processes them asynchronously through a streaming pipeline.

Main components:

- HTTP API (Tapir + http4s)
- Bounded event queue
- FS2 streaming consumer
- Validation stage
- Processing stage
- Retry mechanism
- Dead-letter handling
- Observability endpoints

---

# System Flow
```
HTTP API
   │
   ▼
Queue (bounded)
   │
   ▼
FS2 Stream
   │
   ├─ Validation
   ├─ Processing (parallel fibers)
   ├─ Retry
   └─ Dead Letter
```

---

# Event Lifecycle

1. A client sends an event to the HTTP API.
2. The event is placed into a bounded queue.
3. The FS2 consumer reads events from the queue.
4. The event is validated.
5. If valid, the event is processed.
6. If processing fails, retries are attempted.
7. If retries are exhausted, the event is stored in the dead-letter store.

---

# Concurrency Model

The processing pipeline uses:

```

.parEvalMap(maxParallelism)

```

This allows multiple events to be processed concurrently using Cats Effect fibers.

Concurrency is bounded to avoid resource exhaustion.

---

# Backpressure

Backpressure is achieved through a bounded queue.

```

Queue.bounded[IO, Event](capacity)

```

If the queue is full, producers are slowed down until consumers catch up.

This prevents unlimited memory growth under heavy load.

---

# Observability

The system exposes several endpoints for monitoring:

### `/stats`

Processing counters:

- received events
- processed events
- failed events
- validation failures

### `/observability`

Provides:

- queue size
- queue capacity
- processing statistics

### `/dead-letters`

Returns all events that failed permanently after retries.

---

# Resilience

Resilience mechanisms include:

### Validation

Invalid events are rejected before processing.

### Retries

Processing failures are retried using a configurable retry policy.

### Dead Letter Handling

Events that fail after retries are stored for inspection.

---

# Technology Stack

- Scala 3
- Cats Effect
- FS2
- http4s
- Tapir
- Circe
- Ember HTTP server

---

# Future Improvements

Possible extensions include:

- Persistent dead-letter storage
- Dead-letter replay functionality
- Prometheus metrics integration
- Property-based testing of the pipeline
- Configurable retry policies
