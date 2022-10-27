package com.olegych.scastie.client

import io.circe.syntax._
import io.circe._

sealed trait View
object View {
  case object Editor extends View
  case object BuildSettings extends View
  case object CodeSnippets extends View
  case object Status extends View

  private val values: Map[String, View] =
    List[View](
      Editor,
      BuildSettings,
      CodeSnippets,
      Status
    ).map(v => (v.toString, v)).toMap

  implicit val viewDecoder: Decoder[View] = Decoder[String].emap { value =>
    values.get(value).toRight(s"Illegal key: $value")
  }

  implicit val severityEncoder: Encoder[View] = Encoder[String].contramap { _.toString }

}
