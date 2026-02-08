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
