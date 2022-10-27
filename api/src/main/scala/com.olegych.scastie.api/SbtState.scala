package com.olegych.scastie.api

import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._


sealed trait SbtState extends ServerState
object SbtState {
  case object Unknown extends SbtState {
    override def toString: String = "Unknown"
    def isReady: Boolean = true
  }

  case object Disconnected extends SbtState {
    override def toString: String = "Disconnected"
    def isReady: Boolean = false
  }


  implicit val sbtStateDecoder: Decoder[SbtState] = Decoder[String].emap {
    case "Unknown" => Right(Unknown)
    case "Disconnected" => Right(Disconnected)
    case other => Left(s"Invalid mode: $other")
  }

  implicit val sbtStateEncoder: Encoder[SbtState] = Encoder[String].contramap {
    case Unknown => Unknown.toString
    case Disconnected => Disconnected.toString
  }

}
