package api

import sttp.tapir.*

object Endpoints:
  val health: PublicEndpoint[Unit, Unit, String, Any] =
    endpoint.get
      .in("health")
      .out(stringBody)
