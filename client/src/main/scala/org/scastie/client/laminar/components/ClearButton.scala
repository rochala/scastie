package org.scastie.client.laminar.components

import com.raquo.laminar.api.L.*
import org.scastie.client.components.editor.EditorKeymaps
import org.scastie.client.i18n.I18n

/**
 * Clear button component - Laminar version
 *
 * Migrated from: org.scastie.client.components.ClearButton
 */
object ClearButton:

  /**
   * Create a clear button element.
   *
   * @param onClear Observer to handle clear action
   * @param language Language code for i18n
   * @return Button element as list item
   */
  def apply(
    onClear: Observer[Unit],
    language: String = "en"
  ): HtmlElement =
    li(
      title := s"${I18n.t("editor.clear_messages")} (${EditorKeymaps.clear.getName})",
      role := "button",
      cls := "btn",
      onClick.mapTo(()) --> onClear,
      div(
        i(cls := "fa fa-eraser"),
        span(I18n.t("editor.clear_messages"))
      )
    )
