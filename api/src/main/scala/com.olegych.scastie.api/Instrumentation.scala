package com.olegych.scastie.api

import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._


object Render {

  implicit val valueCodec: Codec.AsObject[Value] = deriveCodec[Value]
  implicit val htmlCodec: Codec.AsObject[Html] = deriveCodec[Html]
  implicit val attachedDomCodec: Codec.AsObject[AttachedDom] = deriveCodec[AttachedDom]

  implicit def codec: Codec[Render] = new Codec[Render] {
    override def apply(subtype: Render): Json =
      (subtype match {
        case value: Value => value.asJsonObject
        case html: Html => html.asJsonObject
        case attachedDom: AttachedDom => attachedDom.asJsonObject
      }).+:("tpe" -> subtype.getClass.getName.asJson).asJson

    override def apply(c: HCursor): Decoder.Result[Render] = c.downField("tpe").as[String].flatMap {
      case "Value" => c.as[Value]
      case "Html" => c.as[Html]
      case "AttachedDom" => c.as[AttachedDom]
      case other => Left(DecodingFailure(other, c.history))
    }.map(_.asInstanceOf[Render])
  }
}

sealed trait Render

case class Value(v: String, className: String) extends Render
case class Html(a: String, folded: Boolean = false) extends Render {
  def stripMargin: Html = copy(a = a.stripMargin)
  def fold: Html = copy(folded = true)
}
case class AttachedDom(uuid: String, folded: Boolean = false) extends Render {
  def fold: AttachedDom = copy(folded = true)
}

object Instrumentation {
  val instrumentedObject = "Playground"

  implicit val instrumentationEncoder: Encoder[Instrumentation] = deriveEncoder[Instrumentation]
  implicit val instrumentationDecoder: Decoder[Instrumentation] = deriveDecoder[Instrumentation]
}

case class Instrumentation(
    position: Position,
    render: Render
)
