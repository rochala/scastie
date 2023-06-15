package scastie.server.routes

import scala.concurrent.Future

import akka.actor.{ActorRef, ActorSystem}
import cats.syntax.all._
import scastie.endpoints.ApiEndpoints
import scastie.server.RestApiServer
import sttp.tapir._

class ApiRoutesImpl(dispatchActor: ActorRef)(implicit system: ActorSystem) {
  import system.dispatcher
  import SessionManager._

  val saveImpl = ApiEndpoints.saveEndpoint.optionalSecure
    .serverLogicSuccess(maybeUser => new RestApiServer(dispatchActor, maybeUser).save(_))

  val forkImpl = ApiEndpoints.forkEndpoint.optionalSecure
    .in(clientIp)
    .serverLogic(maybeUser =>
      inputs => {
        val (input, clientIp) = inputs
        new RestApiServer(dispatchActor, maybeUser, clientIp).fork(input).map(_.toRight("Failure"))
      }
    )

  val deleteImpl = ApiEndpoints.deleteEndpoint.secure
    .serverLogicSuccess(user => new RestApiServer(dispatchActor, Some(user)).delete(_))

  val updateImpl = ApiEndpoints.updateEndpoint
    .in(clientIp)
    .secure
    .serverLogic(user =>
      inputs => {
        val (editInputs, clientIp) = inputs
        new RestApiServer(dispatchActor, Some(user), clientIp).update(editInputs).map(_.toRight("Failure"))
      }
    )

  val userSettingsImpl = ApiEndpoints.userSettingsEndpoint.secure
    .serverLogic(user => _ => Future(user.asRight))

  val userSnippetsEndpoint = ApiEndpoints.userSnippetsEndpoint.secure
    .serverLogicSuccess(user => _ => new RestApiServer(dispatchActor, Some(user)).fetchUserSnippets())

  val serverEndpoints = List(saveImpl, forkImpl, deleteImpl, updateImpl, userSettingsImpl, userSnippetsEndpoint)
}
