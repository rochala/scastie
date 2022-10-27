package com.olegych.scastie.api

import io.circe._
import io.circe.generic.semiauto._

object Severity {
  implicit val severityDecoder: Decoder[Severity] = Decoder[String].emap {
    case "Info" => Right(Info)
    case "Warning" => Right(Warning)
    case "Error" => Right(Error)
    case other => Left(s"Invalid mode: $other")
  }

  implicit val severityEncoder: Encoder[Severity] = Encoder[String].contramap {
    case Info => "Info"
    case Warning => "Warning"
    case Error => "Error"
  }
}

sealed trait Severity
case object Info extends Severity
case object Warning extends Severity
case object Error extends Severity

object Problem {
  implicit val problemEncoder: Encoder[Problem] = deriveEncoder[Problem]
  implicit val problemDecoder: Decoder[Problem] = deriveDecoder[Problem]
}

case class Problem(
    severity: Severity,
    line: Option[Int],
    message: String
)
