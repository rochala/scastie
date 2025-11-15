package org.scastie.client.laminar.components

import com.raquo.laminar.api.L.*
import org.scastie.api.{SnippetId, User, ScalaTarget, ScalaTargetType}
import org.scastie.client.{View, Page, MetalsStatus}
import org.scastie.client.i18n.I18n
import com.raquo.waypoint.Router

/**
 * Editor top bar with action buttons - Laminar version
 *
 * Migrated from: org.scastie.client.components.EditorTopBar
 */
object EditorTopBar:

  /**
   * Create an editor top bar component.
   *
   * @param view Signal containing current view
   * @param setView Observer to change view
   * @param isRunning Signal indicating if code is running
   * @param isStatusOk Signal indicating if status is ok
   * @param inputsHasChanged Signal indicating if code has changed
   * @param isDarkTheme Signal indicating dark theme state
   * @param snippetId Signal containing optional snippet ID
   * @param scalaTarget Signal containing Scala target
   * @param isWorksheetMode Signal indicating worksheet mode
   * @param metalsStatus Signal containing Metals status
   * @param onRun Observer to run code
   * @param onClear Observer to clear messages
   * @param onFormat Observer to format code
   * @param onToggleWorksheet Observer to toggle worksheet mode
   * @param onToggleMetals Observer to toggle Metals status
   * @param router Optional router for URL generation
   * @param language Language code for i18n
   * @return Editor top bar element
   */
  def apply(
    view: Signal[View],
    setView: Observer[View],
    isRunning: Signal[Boolean],
    isStatusOk: Signal[Boolean],
    inputsHasChanged: Signal[Boolean],
    isDarkTheme: Signal[Boolean],
    snippetId: Signal[Option[SnippetId]],
    scalaTarget: Signal[ScalaTarget],
    isWorksheetMode: Signal[Boolean],
    metalsStatus: Signal[MetalsStatus],
    onRun: Observer[Unit],
    onClear: Observer[Unit],
    onFormat: Observer[Unit],
    onToggleWorksheet: Observer[Unit],
    onToggleMetals: Observer[Unit],
    router: Option[Router[Page]] = None,
    language: String = "en"
  ): HtmlElement =
    val isDisabledSignal = view.map(_ != View.Editor)

    nav(
      cls := "editor-topbar",
      cls <-- isDisabledSignal.map(disabled => if disabled then "disabled" else ""),

      ul(
        cls := "editor-buttons",

        // Run button
        RunButton(
          isRunning = isRunning,
          isStatusOk = isStatusOk,
          onSave = onRun,
          setView = setView,
          embedded = false
        ),

        // Format button
        FormatButton(
          onFormat = onFormat,
          inputsHasChanged = inputsHasChanged,
          isStatusOk = isStatusOk,
          language = language
        ),

        // Clear button
        ClearButton(
          onClear = onClear,
          language = language
        ),

        // Worksheet button
        child <-- Signal.combine(scalaTarget, isWorksheetMode, view).map {
          case (target, worksheetMode, currentView) =>
            WorksheetButton(
              hasWorksheetMode = Val(target.targetType != ScalaTargetType.ScalaCli),
              isWorksheetMode = Val(worksheetMode),
              onToggle = onToggleWorksheet,
              view = Val(currentView),
              language = language
            )
        },

        // Download button (only if snippet ID exists)
        child <-- snippetId.map {
          case Some(sid) =>
            DownloadButton(sid, language)
          case None =>
            emptyNode
        }

        // TODO: Add embedded modal button when modals are migrated
        // TODO: Add Metals status indicator when component is created
      )
    )

  /**
   * Simplified version with fewer parameters.
   */
  def apply(
    view: Signal[View],
    isRunning: Signal[Boolean],
    onRun: Observer[Unit],
    onClear: Observer[Unit],
    onFormat: Observer[Unit]
  ): HtmlElement =
    apply(
      view = view,
      setView = Observer.empty,
      isRunning = isRunning,
      isStatusOk = Val(true),
      inputsHasChanged = Val(false),
      isDarkTheme = Val(false),
      snippetId = Val(None),
      scalaTarget = Val(ScalaTarget.Scala3.default),
      isWorksheetMode = Val(false),
      metalsStatus = Val(org.scastie.client.MetalsDisabled),
      onRun = onRun,
      onClear = onClear,
      onFormat = onFormat,
      onToggleWorksheet = Observer.empty,
      onToggleMetals = Observer.empty,
      router = None,
      language = "en"
    )
