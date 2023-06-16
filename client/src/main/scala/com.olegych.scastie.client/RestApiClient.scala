package com.olegych.scastie.client

import java.net.URI
import scala.concurrent.Future
import scala.util.Try

import com.olegych.scastie.api._
import org.scalajs.dom
import org.scalajs.dom.XMLHttpRequest
import play.api.libs.json._
import scalajs.concurrent.JSExecutionContext.Implicits.queue
import scalajs.js
import scalajs.js.Thenable.Implicits._
import scastie.endpoints.ApiEndpoints
import scastie.endpoints.OAuthEndpoints
import sttp.capabilities.WebSockets
import sttp.client3.FetchBackend
import sttp.client3.FetchOptions
import sttp.model.Uri
import sttp.model.Uri._
import sttp.tapir.client.sttp.SttpClientInterpreter
import sttp.tapir.Endpoint

class RestApiClient(maybeServerUrl: Option[String]) extends RestApi {
  import scastie.endpoints.SecureClientExtensions.SecureScalaJSClient

  private val backend = FetchBackend()
  val client          = SttpClientInterpreter()
  val serverUri       = maybeServerUrl.map(serverUrl => uri"$serverUrl")

  private def getXSRFToken: Option[String] = dom.document.cookie
    .split(";")
    .find(_.startsWith("__Host-XSRF-Token"))
    .flatMap(_.split("=").lastOption)

  def logout = client
    .toClientThrowErrors(OAuthEndpoints.logout, serverUri, backend)
    .apply(())

  def run(inputs: Inputs) = client
    .toClientThrowErrors(ApiEndpoints.runEndpoint, serverUri, backend)
    .apply(inputs)

  def save(inputs: Inputs): Future[SnippetId] = client
    .toSecureClientThrowErrors(ApiEndpoints.saveEndpoint.clientEndpoint, serverUri, backend)
    .apply(getXSRFToken)(inputs)

  def format(request: FormatRequest): Future[FormatResponse] = client
    .toClientThrowErrors(ApiEndpoints.formatEndpoint, serverUri, backend)
    .apply(request)

  def update(editInputs: EditInputs): Future[Option[SnippetId]] = client
    .toSecureClientThrowDecodeFailures(ApiEndpoints.updateEndpoint.clientEndpoint, serverUri, backend)
    .apply(getXSRFToken)(editInputs)
    .map(_.toOption)

  def fork(editInputs: EditInputs): Future[Option[SnippetId]] = client
    .toSecureClientThrowDecodeFailures(ApiEndpoints.forkEndpoint.clientEndpoint, serverUri, backend)
    .apply(getXSRFToken)(editInputs)
    .map(_.toOption)

  def delete(snippetId: SnippetId): Future[Boolean] = client
    .toSecureClientThrowDecodeFailures(ApiEndpoints.deleteEndpoint.clientEndpoint, serverUri, backend)
    .apply(getXSRFToken)(snippetId)
    .map(_.isRight)

  def fetch(snippetId: SnippetId): Future[Option[FetchResult]] = client
    .toClientThrowDecodeFailures(ApiEndpoints.snippetApiEndpoint.underlying, serverUri, backend)
    .apply(snippetId)
    .map(_.toOption)

  def fetchOld(id: Int): Future[Option[FetchResult]] = client
    .toClientThrowDecodeFailures(ApiEndpoints.oldSnippetApiEndpoint, serverUri, backend)
    .apply(id)
    .map(_.toOption)

  def fetchUser(): Future[Option[User]] = client
    .toSecureClientThrowDecodeFailures(ApiEndpoints.userSettingsEndpoint.clientEndpoint, serverUri, backend)
    .apply(getXSRFToken)(())
    .map(_.toOption)

  def fetchUserSnippets(): Future[List[SnippetSummary]] = client
    .toSecureClientThrowDecodeFailures(ApiEndpoints.userSnippetsEndpoint.clientEndpoint, serverUri, backend)
    .apply(getXSRFToken)(())
    .map(_.getOrElse(Nil))

}
