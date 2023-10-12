package com.olegych.scastie.client.components.editor

import scastie.api
import typings.codemirrorLint.mod.Severity

object JsUtils {

  def parseSeverity(severity: api.Severity): Severity = severity match {
    case api.Error   => Severity.error
    case api.Info    => Severity.info
    case api.Warning => Severity.warning
  }

}
