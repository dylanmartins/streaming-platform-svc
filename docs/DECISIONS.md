# Architecture Decisions

This document captures key architectural and technology decisions made during the development of the Streaming Platform project.

---

## Streaming Library: FS2 vs Pekko Streams

**Decision:** Use FS2.

### Context

The project required a streaming library to implement the data ingestion and processing pipeline.

The main options considered were:

- FS2
- Pekko Streams

### Reasoning

**Ecosystem Integration**

The project uses Cats Effect for concurrency and functional effects.  
FS2 is built on top of Cats Effect and integrates naturally with the Cats ecosystem.

**Functional Style**

FS2 encourages a functional streaming model that aligns well with the goal of building a purely functional pipeline.

**Testing**

FS2 pipelines are generally easier to test because they are simple values (`Stream[F, A]`) that can be interpreted during tests.

**Lower Boilerplate**

Compared to Pekko Streams, FS2 typically requires less configuration and boilerplate to construct simple pipelines.

---

## HTTP Server: http4s vs Pekko HTTP

**Decision:** Use http4s.

### Context

The system exposes an HTTP API to ingest events and expose observability endpoints.

Possible choices:

- http4s
- Pekko HTTP

### Reasoning

**Functional Purity**

http4s is built around Cats Effect and integrates directly with the functional programming model used in the rest of the system.

**Ecosystem Consistency**

Since Pekko Streams was not selected for the streaming layer, introducing Pekko HTTP would unnecessarily add Pekko dependencies.

Using http4s keeps the stack consistent and lightweight.

---

## API Documentation: Tapir

**Decision:** Use Tapir to define HTTP endpoints.

### Reasoning

Tapir allows defining endpoints as typed descriptions which can then be interpreted by different servers.

Benefits include:

- Automatic OpenAPI generation
- Type-safe endpoint definitions
- Clear separation between endpoint description and server logic

The generated API documentation is available at:
```http://localhost:8080/docs/```

---

## Concurrency Model: Bounded Parallelism

**Decision:** Use `parEvalMap(maxParallelism)`.

### Context

Events are processed concurrently using Cats Effect.

### Reasoning

Unbounded concurrency (starting a fiber per event) can cause resource exhaustion under heavy load.

Risks:

- Exhausting DB connection pools
- Saturating HTTP clients
- Triggering remote service rate limits
- Increased memory usage
- CPU pressure

Using:
```
.parEvalMap(maxParallelism)
```

ensures:
- bounded concurrency
- predictable resource usage
- controlled throughput

---

## Backpressure Strategy

**Decision:** Use a bounded in-memory queue.
```
Queue.bounded[IO, Event](queueSize)
```

### Reasoning

The bounded queue provides natural backpressure:

- If the queue is full, producers will block
- This prevents unlimited memory growth
- It allows consumers to catch up

The queue handles buffering while `parEvalMap` controls the processing parallelism.

---

## Dead Letter Handling

**Decision:** Implement an in-memory dead-letter store.

### Context

Events that fail processing after all retries are stored for later inspection.

### Implementation

Dead-letter events are stored in:
```
Ref[IO, Vector[DeadLetterEvent]]
```
### Note
This implementation exists for educational purposes!

In a production environment, dead-letter handling would typically be managed by the messaging infrastructure itself, for example:
- Kafka Dead Letter Topic
- RabbitMQ Dead Letter Exchange
- SQS Dead Letter Queue

These systems provide durability and cross-service visibility.

---

## Shared Mutable State: Ref

**Decision:** Use `Ref[F, A]` for concurrent state.

### Reasoning

`Ref[F, A]` provides:
- safe concurrent updates
- atomic modifications
- integration with Cats Effect

It is used for:
- processing statistics
- dead-letter storage
