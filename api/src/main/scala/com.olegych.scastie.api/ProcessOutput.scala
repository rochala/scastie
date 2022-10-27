package com.olegych.scastie.api

import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._

trait ProcessOutputType

object ProcessOutputType {
  case object StdOut extends ProcessOutputType
  case object StdErr extends ProcessOutputType

  implicit val decodeProcessOutputType: Decoder[ProcessOutputType] = Decoder[String].emap {
    case "StdOut" => Right(StdOut)
    case "StdErr" => Right(StdErr)
    case other => Left(s"Invalid mode: $other")
  }

  implicit val encodeProcessOutputType: Encoder[ProcessOutputType] = Encoder[String].contramap {
    case StdOut => "StdOut"
    case StdErr => "StdErr"
  }
}

object ProcessOutput {
  implicit val processOutputEncoder: Encoder[ProcessOutput] = deriveEncoder[ProcessOutput]
  implicit val processOutputDecoder: Decoder[ProcessOutput] = deriveDecoder[ProcessOutput]
}

case class ProcessOutput(
    line: String,
    tpe: ProcessOutputType,
    id: Option[Long]
)
