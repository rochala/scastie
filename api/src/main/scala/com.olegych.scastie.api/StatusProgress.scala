package com.olegych.scastie.api

import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._

object SbtRunnerState {
  implicit val sbtRunnerStateCodec: Codec[SbtRunnerState] = deriveCodec[SbtRunnerState]
}

case class SbtRunnerState(
    config: Inputs,
    tasks: Vector[TaskId],
    sbtState: SbtState
)
sealed trait StatusProgress

object StatusProgress {
  implicit val sbtCodec: Codec.AsObject[StatusProgress.Sbt] = deriveCodec[StatusProgress.Sbt]


  implicit def codec: Codec[StatusProgress] = new Codec[StatusProgress] {
    override def apply(subtype: StatusProgress): Json =
      (subtype match {
        case KeepAlive => ("tpe" -> "StatusProgress.KeepAlive").asJson
        case sbt: StatusProgress.Sbt => sbt.asJsonObject.+:("tpe" -> "StatusProgress.Sbt".asJson).asJson
      })

    override def apply(c: HCursor): Decoder.Result[StatusProgress] =
      c.downField("tpe").as[String].flatMap {
        case "StatusProgress.KeepAlive" => Right(KeepAlive)
        case "StatusProgress.Sbt" => c.as[StatusProgress.Sbt]
        case other => Left(DecodingFailure(other, c.history))
    }.map(_.asInstanceOf[StatusProgress])
  }

  case object KeepAlive extends StatusProgress
  case class Sbt(runners: Vector[SbtRunnerState]) extends StatusProgress
}
