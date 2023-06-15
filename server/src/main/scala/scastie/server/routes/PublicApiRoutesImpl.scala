package scastie.server.routes

import akka.actor.{ActorRef, ActorSystem}
import scastie.endpoints.ApiEndpoints
import scastie.server.RestApiServer
import sttp.tapir._

class PublicApiRoutesImpl(dispatchActor: ActorRef)(implicit system: ActorSystem) {
  import system.dispatcher

  val runImpl = ApiEndpoints.runEndpoint
    .in(clientIp)
    .serverLogicSuccess(inputs => {
      val (input, clientIp) = inputs
      new RestApiServer(dispatchActor, None, clientIp).run(input)
    })

  val formatImpl = ApiEndpoints.formatEndpoint
    .serverLogicSuccess(
      new RestApiServer(dispatchActor, None).format(_)
    )

  val snippetEndpoint = ApiEndpoints.snippetApiEndpoint.underlying
    .serverLogicOption(new RestApiServer(dispatchActor, None).fetch(_))

  val serverEndpoints = List(runImpl, formatImpl, snippetEndpoint)
}
