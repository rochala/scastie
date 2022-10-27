package com.olegych.scastie.api

import io.circe._
import io.circe.generic.semiauto._


object TaskId {
  implicit val taskIdCodec: Codec[TaskId] = deriveCodec[TaskId]
}

case class TaskId(snippetId: SnippetId)
