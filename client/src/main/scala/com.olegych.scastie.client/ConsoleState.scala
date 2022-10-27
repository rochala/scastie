package com.olegych.scastie.client

import io.circe.generic.semiauto._
import io.circe._

object ConsoleState {
  implicit val consoleStateCodec: Codec[ConsoleState] = deriveCodec[ConsoleState]

  def default: ConsoleState = ConsoleState(
    consoleIsOpen = false,
    consoleHasUserOutput = false,
    userOpenedConsole = false
  )
}

case class ConsoleState(
    consoleIsOpen: Boolean,
    consoleHasUserOutput: Boolean,
    userOpenedConsole: Boolean
)
