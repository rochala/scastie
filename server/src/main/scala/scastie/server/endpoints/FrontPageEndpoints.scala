package scastie.server.endpoints

import sttp.tapir._
import sttp.tapir.files._

import play.api.libs.json._
import sttp.model.headers.CacheDirective
import sttp.model.Header
import sttp.model.MediaType
import scastie.endpoints.SnippetIdUtils._
import scastie.endpoints.SnippetMatcher
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter

object FrontPageEndpoints {
  val classLoader = ClassLoader.getSystemClassLoader()

  val embeddedJSEndpoint = staticResourcesGetEndpoint("embedded.js")
    .out(header(Header.cacheControl(CacheDirective.NoCache)))
    .out(header(Header.contentType(MediaType.TextJavascript)))
    .description("Access point to JavaScript source required to run embedded snippets")
    .name("Get embedded.js")

  val embeddedCSSEndpoint = staticResourcesGetEndpoint("public" / "embedded.css")
    .out(header(Header.cacheControl(CacheDirective.NoCache)))
    .out(header(Header.contentType(MediaType.TextCss)))
    .description("Access point to CSS source required to run embedded snippets")
    .name("Get embedded.css")

  val publicAssetsEndpoint = staticResourcesGetEndpoint("public")
    .name("Public assets")

  val indexEndpoint = staticResourcesGetEndpoint("")
    .out(header(Header.cacheControl(CacheDirective.NoCache)))
    .name("Index")

  // TODO: Migrate to Scala3 enum and move this to API
  object ColorScheme extends Enumeration {
    type ColorScheme = Value
    val Light = Value("light")
    val Dark = Value("dark")
  }

  implicit val colorSchemeFormat: Format[ColorScheme.Value] = Json.formatEnum(ColorScheme)

  val frontPageSnippetEndpoint: PublicEndpoint[(MaybeEmbeddedSnippet, Option[ColorScheme.Value]), Unit, FrontPageSnippet, Any] =
    SnippetMatcher.getFrontPageSnippetEndpoints(endpoint)
      .in(query[Option[ColorScheme.Value]]("theme"))
      .name("Snippet URL access")
      .description(
        """|This endpoint serves 3 purposes:
           | - it is used to access and share snippets between users,
           | - it is used to access open graph metadata for a snippet.
           | - it is used to access embedded snippet JavaScript code,
           |
           |Snippet access and open graph metadata is determined by the snippet Id.
           |The snippet Id consists of 3 parts: a user name, a snippet id and optionally a revision number.
           |The user name is optional and if not provided, the snippet is assumed to be owned by the anonymous user.
           |Revision number is also optional is always bounded to username. If not provided, the latest revision is used.
           |Example:
           |  - https://scastie.scala-lang.org/randomUUID
           |  - https://scastie.scala-lang.org/username/randomUUID
           |  - https://scastie.scala-lang.org/username/randomUUID/1
           |
           |Embedded snippet access is determined in the same way as snippet access,
           |but additionally requires a `.js` suffix. It also takes optional `theme` parameter.
           |Example:
           |  - https://scastie.scala-lang.org/randomUUID.js
           |  - https://scastie.scala-lang.org/username/randomUUID.js
           |  - https://scastie.scala-lang.org/username/randomUUID/1.js
           |
           |""".stripMargin
       )

  val endpoints: List[AnyEndpoint] = List(
    embeddedJSEndpoint,
    embeddedCSSEndpoint,
    publicAssetsEndpoint,
    indexEndpoint,
    frontPageSnippetEndpoint
  )
}
