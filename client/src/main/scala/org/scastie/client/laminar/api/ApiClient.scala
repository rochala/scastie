package org.scastie.client.laminar.api

import com.raquo.airstream.core.EventStream
import org.scastie.api.*
import org.scastie.client.RestApiClient
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
 * Reactive API client for Laminar integration.
 *
 * Wraps RestApiClient and provides EventStream-based API.
 */
class ApiClient(serverUrl: Option[String] = None):

  private val restClient = new RestApiClient(serverUrl)

  /**
   * Run code and return snippet ID as EventStream.
   */
  def run(inputs: BaseInputs): EventStream[SnippetId] =
    EventStream.fromFuture(restClient.run(inputs))

  /**
   * Save code and return snippet ID as EventStream.
   */
  def save(inputs: BaseInputs): EventStream[SnippetId] =
    EventStream.fromFuture(restClient.save(inputs))

  /**
   * Format code and return formatted result as EventStream.
   */
  def format(request: FormatRequest): EventStream[FormatResponse] =
    EventStream.fromFuture(restClient.format(request))

  /**
   * Fetch snippet by ID.
   */
  def fetchSnippet(snippetId: SnippetId): EventStream[Option[FetchResult]] =
    EventStream.fromFuture(restClient.fetch(snippetId))

  /**
   * Fetch old snippet by number.
   */
  def fetchOldSnippet(id: Int): EventStream[Option[FetchResult]] =
    EventStream.fromFuture(restClient.fetchOld(id))

  /**
   * Fetch user snippets.
   */
  def fetchUserSnippets(): EventStream[List[SnippetSummary]] =
    EventStream.fromFuture(restClient.fetchUserSnippets())

  /**
   * Update snippet.
   */
  def update(snippetId: SnippetId, inputs: BaseInputs): EventStream[Option[SnippetId]] =
    EventStream.fromFuture(restClient.update(snippetId, inputs))

  /**
   * Delete snippet.
   */
  def delete(snippetId: SnippetId): EventStream[Unit] =
    EventStream.fromFuture(restClient.delete(snippetId).map(_ => ()))

  /**
   * Autocomplete.
   */
  def autocomplete(request: AutoCompletionRequest): EventStream[Option[AutoCompletionResponse]] =
    EventStream.fromFuture(restClient.autocomplete(request))

object ApiClient:
  def apply(serverUrl: Option[String] = None): ApiClient =
    new ApiClient(serverUrl)
