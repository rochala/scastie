package com.olegych.scastie.api

import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._

sealed trait ScalaTargetType {
  def defaultScalaTarget: ScalaTarget
}

object ScalaTargetType {

  def parse(targetType: String): Option[ScalaTargetType] = {
    targetType match {
      case "JVM"       => Some(Scala2)
      case "DOTTY"     => Some(Scala3)
      case "JS"        => Some(JS)
      case "NATIVE"    => Some(Native)
      case "TYPELEVEL" => Some(Typelevel)
      case _           => None
    }
  }

  private val values = List(Scala2, Scala3, JS, Native, Typelevel).map(v => (v.toString, v)).toMap

  implicit val decodeProcessOutputType: Decoder[ScalaTargetType] = Decoder[String].emap { field =>
    values.get(field).toRight(s"Invalid mode: $field")
  }

  implicit val encodeProcessOutputType: Encoder[ScalaTargetType] = Encoder[String].contramap {
    _.toString
  }

  case object Scala2 extends ScalaTargetType {
    def defaultScalaTarget: ScalaTarget = ScalaTarget.Jvm.default
  }

  case object Scala3 extends ScalaTargetType {
    def defaultScalaTarget: ScalaTarget = ScalaTarget.Scala3.default
  }

  case object JS extends ScalaTargetType {
    def defaultScalaTarget: ScalaTarget = ScalaTarget.Js.default
  }

  case object Native extends ScalaTargetType {
    def defaultScalaTarget: ScalaTarget = ScalaTarget.Native.default
  }

  case object Typelevel extends ScalaTargetType {
    def defaultScalaTarget: ScalaTarget = ScalaTarget.Typelevel.default
  }
}
