# DECISIONS

## Streaming Library: FS2 vs Pekko Streams
Decision: Use FS2.

### Context & Reasoning:
Ecosystem Integration: FS2 "speaks the same language" as the Cats ecosystem, which is being used throughout the project.
Testing: FS2 provides a more straightforward testing experience compared to Pekko Streams.
Boilerplate: FS2 generally requires less boilerplate code to achieve similar streaming tasks.

## HTTP Server/Client: http4s vs Pekko HTTP
Decision: Use http4s.

### Context & Reasoning:
Functional purity: As a Cats-native library, http4s aligns with the goal of keeping the architecture functionally "pure".
Consistency: Since Pekko Streams was rejected in favor of FS2, integrating Pekko HTTP would introduce an unnecessary dependency on the Pekko ecosystem without the benefit of its streaming integration.


### Notes:
POST /events -> queue -> background consumer -> log

Queue.bounded controls how many events can wait in memory and .parEvalMap controls how many events can be actively worked on at once (parallelims).
- queue controls buffering/backpressure
- parEvalMap controls worker parallelism

And why limit is to a specific number `.parEvalMap(maxParallelism)(process)` is better than just start a new parallel thread when the event arrives?
because if you don't do it in huge load environment you can end up with too many threads and OOM. So you want to control how many events are being processed at the same time.
Risks:
- DB connections
- HTTP client pool size
- remote service rate limits
- CPU
- queue growth
- memory retention
Also, if you implement backpressure correctly, you can have a smaller queue size and rely on the backpressure to slow down the producers instead of buffering too many events in memory.

Ref
A Ref[F, A] is a safe mutable reference in an effectful/concurrent program.
- shared state
- updated safely
- usable from multiple fibers


queue -> validate -> process
