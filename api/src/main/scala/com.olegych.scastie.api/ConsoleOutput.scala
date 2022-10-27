package com.olegych.scastie.api

import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._

sealed trait ConsoleOutput {
  def show: String
}

object ConsoleOutput {
  case class SbtOutput(output: ProcessOutput) extends ConsoleOutput {
    def show: String = s"sbt: ${output.line}"
  }

  case class UserOutput(output: ProcessOutput) extends ConsoleOutput {
    def show: String = output.line
  }

  case class ScastieOutput(line: String) extends ConsoleOutput {
    def show: String = s"scastie: $line"
  }

  implicit val sbtOutputCodec: Codec.AsObject[SbtOutput] = deriveCodec[SbtOutput]
  implicit val userOutputCodec: Codec.AsObject[UserOutput] = deriveCodec[UserOutput]
  implicit val scastieOutputCodec: Codec.AsObject[ScastieOutput] = deriveCodec[ScastieOutput]

  implicit def codec: Codec[ConsoleOutput] = new Codec[ConsoleOutput] {
    override def apply(subtype: ConsoleOutput): Json =
      (subtype match {
        case sbtOutput: SbtOutput => sbtOutput.asJsonObject
        case userOutput: UserOutput => userOutput.asJsonObject
        case scastieOutput: ScastieOutput => scastieOutput.asJsonObject
      }).+:("tpe" -> subtype.getClass.toString.asJson).asJson

    override def apply(c: HCursor): Decoder.Result[ConsoleOutput] =
      c.downField("tpe").as[String].flatMap {
        case "SbtOutput" => c.as[SbtOutput]
        case "UserOutput" => c.as[UserOutput]
        case "ScastieOutput" => c.as[ScastieOutput]
        case other => Left(DecodingFailure(other, c.history))
      }.map(_.asInstanceOf[ConsoleOutput])
  }
}
