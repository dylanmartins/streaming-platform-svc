package api

import cats.effect.IO
import cats.effect.std.Queue
import domain.Event
import org.http4s.HttpRoutes
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object HttpApi:

  def routes(queue: Queue[IO, Event]): HttpRoutes[IO] =
    val endpointDescriptions = List(
      Endpoints.health,
      Endpoints.ingestEvent
    )

    val businessServerEndpoints: List[ServerEndpoint[Any, IO]] =
      List(
        Endpoints.health.serverLogicSuccess[IO](_ => IO.pure("ok"))
      )

    val docsServerEndpoints: List[ServerEndpoint[Any, IO]] =
      SwaggerInterpreter()
        .fromEndpoints[IO](endpointDescriptions, "Streaming Platform Project", "0.1")

    Http4sServerInterpreter[IO]()
      .toRoutes(businessServerEndpoints ++ docsServerEndpoints)
