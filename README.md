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