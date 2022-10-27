package com.olegych.scastie.api

import io.circe._
import io.circe.generic.semiauto._

case object SbtPing
case object SbtPong

case class SbtRunnerConnect(hostname: String, port: Int)
case object ActorConnected

case class SnippetSummary(
    snippetId: SnippetId,
    summary: String,
    time: Long
)

object SnippetSummary {
  implicit val snippetSummaryCodec: Codec[SnippetSummary] = deriveCodec[SnippetSummary]
}

case class FormatRequest(
    code: String,
    isWorksheetMode: Boolean,
    scalaTarget: ScalaTarget
)

object FormatRequest {
  implicit val formatRequestCodec: Codec[FormatRequest] = deriveCodec[FormatRequest]
}

object FormatResponse {
  import io.circe.generic.auto._

  implicit val formatResponseCodec: Codec[FormatResponse] = deriveCodec[FormatResponse]
}


case class FormatResponse(
    result: Either[String, String]
)

object FetchResult {
  implicit val fetchResultCodec: Codec[FetchResult] = deriveCodec[FetchResult]

  def create(inputs: Inputs, progresses: List[SnippetProgress]) = FetchResult(inputs, progresses.sortBy(p => (p.id, p.ts)))
}

case class FetchResult private (inputs: Inputs, progresses: List[SnippetProgress])

case class FetchScalaJs(snippetId: SnippetId)
case class FetchResultScalaJs(content: String)

case class FetchScalaJsSourceMap(snippetId: SnippetId)
case class FetchResultScalaJsSourceMap(content: String)

case class FetchScalaSource(snippetId: SnippetId)
case class FetchResultScalaSource(content: String)

object ScalaDependency {
  implicit val scalaDependencyCodec: Codec[ScalaDependency] = deriveCodec[ScalaDependency]
}

case class ScalaDependency(
    groupId: String,
    artifact: String,
    target: ScalaTarget,
    version: String
) {
  def matches(sd: ScalaDependency): Boolean =
    sd.groupId == this.groupId &&
      sd.artifact == this.artifact

  override def toString: String = target.renderSbt(this)
}

object Project {
  implicit val projectCodec: Codec[Project] = deriveCodec[Project]
}

case class Project(
    organization: String,
    repository: String,
    logo: Option[String],
    artifacts: List[String]
)

// Keep websocket connection
case class KeepAlive(msg: String = "") extends AnyVal
