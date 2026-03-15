# streaming-platform-svc

## Tech Stack

### Core FP Stack
- **Scala 3**
- **Cats Effect 3** – effect system, concurrency, resource safety
- **FS2** – streaming and backpressure

### HTTP Layer
- **http4s (Ember)** – functional HTTP server
- **Tapir** – typed endpoints + OpenAPI generation
- **Swagger UI** – auto-generated API documentation

## Serialization
- **Circe** – JSON encoding/decoding

## Key characteristics:
- **Backpressure** via bounded queues
- **Parallel processing** using Cats Effect fibers
- **Resilience** via retries and dead-letter handling
- **Observability** through runtime metrics endpoints

More details are available in:
- `ARCHITECTURE.md`
- `DECISIONS.md`

# Observability
The service exposes runtime information via API endpoints.

### `/stats`
Processing counters:
- received
- processed
- failed
- validationFailed

### `/observability`
Runtime metrics:
- queue size
- queue capacity
- processing statistics

### `/dead-letters`
Events that permanently failed after retries.

## API Documentation
Swagger UI:
```declarative
http://localhost:8080/docs
```

## Running the Project
### Requirements
- Java 17+ recommended
- sbt installed

### Start the server
```declarative
make run
```

### Server will start on:
```declarative
http://localhost:8080
```

# Example Request
```
curl -X POST http://localhost:8080/events

-H "Content-Type: application/json"
-d '{"eventType":"user-action","payload":"clicked-button"}'
```

### Assignment: Streaming Platform Service

**Data ingestion (FS2 or Pekko Streams)**

- HTTP ingestion via POST /events
- events pushed into Queue.bounded
- FS2 consumer:
```
Stream.fromQueueUnterminated(queue)
```

**Parallel processing with IO fibers**
```
.parEvalMap(config.maxParallelism)
```
- runs events concurrently
- uses Cats Effect fibers
- enforces bounded concurrency

**Error recovery and backpressure**

Error recovery
- validation
- retries
- dead-letter store

Pipeline outcome cases:
```
invalid -> validationFailed
valid -> processed
valid but fails -> retry
retry exhausted -> dead-letter
```

Backpressure
```
Queue.bounded
```
- bounded memory
- producer slowdown when queue fills
- controlled buffering

**Observability**
Endpoints:
```
/stats
/observability
/dead-letters
```
Metrics include:
- received
- processed
- failed
- validationFailed
- queue size
- queue capacity
