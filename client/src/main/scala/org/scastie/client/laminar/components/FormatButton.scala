package org.scastie.client.laminar.components

import com.raquo.laminar.api.L.*
import org.scastie.client.components.editor.EditorKeymaps
import org.scastie.client.i18n.I18n

/**
 * Format code button component - Laminar version
 *
 * Migrated from: org.scastie.client.components.FormatButton
 */
object FormatButton:

  /**
   * Create a format code button.
   *
   * @param onFormat Observer to handle format action
   * @param inputsHasChanged Signal indicating if code has changed (for UI feedback)
   * @param isStatusOk Signal indicating if status is ok
   * @param language Language code for i18n
   * @return Button element as list item
   */
  def apply(
    onFormat: Observer[Unit],
    inputsHasChanged: Signal[Boolean] = Val(false),
    isStatusOk: Signal[Boolean] = Val(true),
    language: String = "en"
  ): HtmlElement =
    li(
      title := s"${I18n.t("editor.format_tooltip")} (${EditorKeymaps.format.getName})",
      role := "button",
      cls := "btn",
      onClick.mapTo(()) --> onFormat,
      i(cls := "fa fa-align-left"),
      span(I18n.t("editor.format"))
    )
