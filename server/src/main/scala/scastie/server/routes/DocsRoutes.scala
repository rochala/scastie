package scastie.server.routes

import scala.concurrent.Future

import scastie.endpoints._
import scastie.endpoints.ApiEndpoints
import scastie.server.endpoints._
import scastie.server.ServerConfig
import sttp.tapir.redoc.bundle.RedocInterpreter

class DocsRoutes() {

  private val publicEndpoints = ApiEndpoints.publicEndpoints ++
    ProgressEndpoints.endpoints ++
    FrontPageEndpoints.endpoints

  private val allEndpoints = publicEndpoints ++
    ApiEndpoints.internalEndpoints ++
    DownloadEndpoints.endpoints ++
    StatusEndpoints.endpoints ++
    OAuthEndpoints.endpoints

  val endpointsForDocumentation =
    if (ServerConfig.production) publicEndpoints
    else allEndpoints

  val serverEndpoints = RedocInterpreter()
    .fromEndpoints[Future](endpointsForDocumentation, "Scastie", "1.0.0-RC1")
}
