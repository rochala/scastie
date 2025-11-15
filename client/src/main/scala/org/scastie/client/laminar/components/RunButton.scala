package org.scastie.client.laminar.components

import com.raquo.laminar.api.L.*
import org.scastie.client.{View, i18n}
import org.scastie.client.components.editor.EditorKeymaps
import org.scastie.client.i18n.I18n

/**
 * Run/Save button component - Laminar version
 *
 * Migrated from: org.scastie.client.components.RunButton
 *
 * Shows different states:
 * - "Run" when not running
 * - "Running..." with spinner when execution is in progress
 */
object RunButton:

  /**
   * Create a run/save button element.
   *
   * @param isRunning Signal indicating if code is currently running
   * @param isStatusOk Signal indicating if server status is ok
   * @param onSave Observer to handle save/run action
   * @param setView Observer to set view (used when clicking while running)
   * @param embedded Whether this is embedded mode
   * @return Button element as list item
   */
  def apply(
    isRunning: Signal[Boolean],
    isStatusOk: Signal[Boolean],
    onSave: Observer[Unit],
    setView: Observer[View],
    embedded: Boolean = false
  ): HtmlElement =
    li(
      role := "button",
      cls := "btn run-button",

      // Dynamic title based on status
      title <-- Signal.combine(isRunning, isStatusOk).map {
        case (running, _) if running =>
          I18n.t("editor.running_tooltip")
        case (_, statusOk) if statusOk =>
          s"${I18n.t("editor.run")} (${EditorKeymaps.saveOrUpdate.getName})"
        case _ =>
          s"${I18n.t("editor.run")} (${EditorKeymaps.saveOrUpdate.getName}) - ${I18n.t("editor.status_unknown_warning")}"
      },

      // Dynamic click handler
      onClick.compose(_.stopPropagation) --> Observer[Any] { _ =>
        if isRunning.now() then
          setView.onNext(View.Editor)
        else
          onSave.onNext(())
      },

      // Dynamic icon and text
      child <-- isRunning.map { running =>
        if running then
          div(
            i(cls := "fa fa-spinner fa-spin"),
            span(I18n.t("editor.running"))
          )
        else
          div(
            i(cls := "fa fa-play"),
            span(I18n.t("editor.run"))
          )
      }
    )

  /**
   * Simplified version with separate observers for each state
   */
  def apply(
    isRunning: Signal[Boolean],
    isStatusOk: Signal[Boolean],
    onRun: Observer[Unit],
    onShowEditor: Observer[Unit]
  ): HtmlElement =
    apply(
      isRunning,
      isStatusOk,
      onRun,
      onShowEditor.contramap(_ => View.Editor),
      embedded = false
    )
