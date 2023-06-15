package scastie.server.endpoints

import sttp.tapir._
import sttp.tapir.json.play._
import sttp.tapir.generic.auto._
import sttp.tapir.server.akkahttp.serverSentEventsBody
import com.olegych.scastie.api._
import sttp.capabilities.akka.AkkaStreams
import scastie.endpoints.SnippetMatcher


object ProgressEndpoints {

  val endpointBase = endpoint.in("api")
  val progressSSE = SnippetMatcher
      .getApiSnippetEndpoint(endpointBase.in("progress-sse"))
      .map(_.out(serverSentEventsBody)
        .description(
          """|Endpoint used to connect to EventStream for specific snippet Id.
             |The connection to it should be instantly estabilished after the snippet is run.
             |
             |Received events are of type `SnippetProgress`, and contain all output of both
             |the compilation and runtime of the snippet.
             |
             |Output is split into `ConsoleOutput` type hierarchy:
             | - `SbtOutput` - output from sbt shell
             | - `UserOutput` - runtime output
             | - `ScastieOutput` - output from scastie server
             |
             |""".stripMargin
          )
      )

  val progressWS = SnippetMatcher
      .getApiSnippetEndpoint(endpointBase.in("progress-ws"))
      .map(_.out(
          webSocketBody[String, CodecFormat.TextPlain, SnippetProgress, CodecFormat.Json](AkkaStreams))
      )

  val endpoints = progressSSE.documentationEndpoints ++ progressWS.documentationEndpoints
}
