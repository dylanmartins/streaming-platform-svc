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

### Observability (planned)
- structured logging
- metrics
- stream monitoring

## Architecture Overview
The platform is structured around three main layers:

1. API Layer
Defines typed HTTP endpoints using Tapir:
- Endpoint contracts separated from business logic
- OpenAPI documentation generated automatically
- http4s interpreter exposes endpoints as routes

2. Streaming Core (WIP)
Will include:
- ingestion pipeline via FS2 queues
- bounded backpressure handling
- parallel processing using IO fibers
- error recovery strategies

## API Documentation
Swagger UI:
```declarative
http://localhost:8080/docs
```

## Running the Project
# Requirements
- Java 17+ recommended
- sbt installed

# Start the server
```declarative
make run
```

# Server will start on:
```declarative
http://localhost:8080
```
