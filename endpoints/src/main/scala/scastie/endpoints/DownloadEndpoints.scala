package scastie.endpoints

import sttp.tapir._

object DownloadEndpoints {

  val endpointBase = endpoint.in("api")

  val downloadSnippetEndpoint = SnippetMatcher
    .getApiSnippetEndpoint(endpointBase.in("download"))
    .map(_.out(fileBody))

  val endpoints = downloadSnippetEndpoint.documentationEndpoints
}
