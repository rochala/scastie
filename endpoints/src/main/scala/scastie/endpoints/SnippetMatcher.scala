package scastie.endpoints

import com.olegych.scastie.api._
import sttp.model.Header
import sttp.model.MediaType
import sttp.tapir._

case class SnippetEndpoint[A, E, O, R](underlying: Endpoint[A, SnippetId, E, O, R]) {

  def map[NA, NE, NO, NR](
    f: Endpoint[A, SnippetId, E, O, R] => Endpoint[NA, SnippetId, NE, NO, NR]
  ): SnippetEndpoint[NA, NE, NO, NR] = {
    SnippetEndpoint(f(underlying))
  }

  def documentationEndpoints: List[AnyEndpoint] = List(
    underlying.in(path[String]("snippetId")),
    underlying.in(path[String]("username")).in(path[String]("snippetId")),
    underlying.in(path[String]("username")).in(path[String]("snippetId")).in(path[String]("revision"))
  )

}

object SnippetMatcher {
  import SnippetIdUtils._

  def getApiSnippetEndpoint[SecurityInput, Output](
    baseEndpoint: Endpoint[SecurityInput, Unit, Unit, Output, Any]
  ): SnippetEndpoint[SecurityInput, Unit, Output, Any] = {
    SnippetEndpoint(
      baseEndpoint
        .in(paths)
        .mapInDecode(toSnippetId(_))((_.path))
    )
  }

  def getFrontPageSnippetEndpoints(
    baseEndpoint: PublicEndpoint[Unit, Unit, Unit, Any]
  ): PublicEndpoint[MaybeEmbeddedSnippet, Unit, FrontPageSnippet, Any] = {
    val snippetOutputVariant = oneOf[FrontPageSnippet](
      oneOfVariant[EmbeddedSnippet](
        stringBody.map(EmbeddedSnippet(_))(_.content) and header(Header.contentType(MediaType.TextJavascript))
      ),
      oneOfVariant[UniversalSnippet](htmlBodyUtf8.map(UniversalSnippet(_))(_.content))
    )

    baseEndpoint
      .in(paths)
      .mapInDecode(toMaybeSnippetId(_))((_.path))
      .out(snippetOutputVariant)
  }

}
