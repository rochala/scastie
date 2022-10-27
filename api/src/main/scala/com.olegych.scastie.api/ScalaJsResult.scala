package com.olegych.scastie.api

import io.circe._
import io.circe.generic.semiauto._

case class ScalaJsResult(
    in: Either[Option[RuntimeError], List[Instrumentation]]
)

object ScalaJsResult {
  import io.circe.generic.auto._

  private case class Error(er: Option[RuntimeError])
  private case class Instrumentations(instrs: List[Instrumentation])

  implicit val ScalaJsResultCodec: Codec[ScalaJsResult] = deriveCodec[ScalaJsResult]
}
