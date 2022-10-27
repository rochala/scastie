package com.olegych.scastie.api

import io.circe._
import io.circe.generic.semiauto._

object ReleaseOptions {
  implicit val releaseOptionsCodec: Codec[ReleaseOptions] = deriveCodec[ReleaseOptions]
}

case class ReleaseOptions(groupId: String, versions: List[String], version: String)

// case class MavenReference(groupId: String, artifactId: String, version: String)

object Outputs {
  implicit val outputsCodec: Codec[Outputs] = deriveCodec[Outputs]

  def default: Outputs = Outputs(
    consoleOutputs = Vector(),
    compilationInfos = Set(),
    instrumentations = Set(),
    runtimeError = None,
    sbtError = false
  )
}
case class Outputs(
    consoleOutputs: Vector[ConsoleOutput],
    compilationInfos: Set[Problem],
    instrumentations: Set[Instrumentation],
    runtimeError: Option[RuntimeError],
    sbtError: Boolean
) {

  def console: String = consoleOutputs.map(_.show).mkString("\n")

  def isClearable: Boolean =
    consoleOutputs.nonEmpty ||
      compilationInfos.nonEmpty ||
      instrumentations.nonEmpty ||
      runtimeError.isDefined
}

object Position {
  implicit val positionCodec: Codec[Position] = deriveCodec[Position]
}

case class Position(start: Int, end: Int)
