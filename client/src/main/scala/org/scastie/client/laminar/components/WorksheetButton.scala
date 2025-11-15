package org.scastie.client.laminar.components

import com.raquo.laminar.api.L.*
import org.scastie.client.{View, i18n}
import org.scastie.client.i18n.I18n

/**
 * Worksheet mode toggle button - Laminar version
 *
 * Migrated from: org.scastie.client.components.WorksheetButton
 */
object WorksheetButton:

  /**
   * Create a worksheet mode toggle button.
   *
   * @param hasWorksheetMode Signal indicating if worksheet mode is supported
   * @param isWorksheetMode Signal indicating if worksheet mode is active
   * @param onToggle Observer to handle toggle action
   * @param view Signal for current view (to show alpha state when not in editor)
   * @param language Language code for i18n
   * @return Button element as list item
   */
  def apply(
    hasWorksheetMode: Signal[Boolean],
    isWorksheetMode: Signal[Boolean],
    onToggle: Observer[Unit],
    view: Signal[View],
    language: String = "en"
  ): HtmlElement =
    val tooltipSignal = Signal.combine(hasWorksheetMode, isWorksheetMode).map {
      case (hasMode, isMode) =>
        if !hasMode then
          I18n.t("editor.worksheet_unsupported")
        else if isMode then
          I18n.t("editor.worksheet_off_tooltip")
        else
          I18n.t("editor.worksheet_on_tooltip")
    }

    val enabledClassSignal = Signal.combine(isWorksheetMode, view).map {
      case (isMode, currentView) =>
        if isMode then
          if currentView != View.Editor then "enabled alpha"
          else "enabled"
        else ""
    }

    li(
      title <-- tooltipSignal,
      cls <-- enabledClassSignal,
      role := "button",
      cls := "btn editor",
      onClick.mapTo(()) --> onToggle,
      i(cls := "fa fa-calendar"),
      span(I18n.t("editor.worksheet")),
      i(
        cls := "workSheetIndicator fa fa-circle",
        cls <-- enabledClassSignal
      )
    )
