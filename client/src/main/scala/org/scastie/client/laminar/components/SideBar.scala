package org.scastie.client.laminar.components

import com.raquo.laminar.api.L.*
import org.scastie.api.{EditorMode, Default, Vim, Emacs, BaseInputs}
import org.scastie.client.{View, StatusState}
import org.scastie.client.i18n.I18n

/**
 * Side navigation bar component - Laminar version
 *
 * Migrated from: org.scastie.client.components.SideBar
 */
object SideBar:

  // Asset imports (using JS module imports)
  // Note: These should be handled by ScalablyTyped or custom facades
  private val logoSrc = "/assets/images/icon-scastie.png"  // TODO: Use proper asset import

  /**
   * Render theme toggle button.
   */
  private def themeButton(
    isDarkTheme: Signal[Boolean],
    onToggleTheme: Observer[Unit]
  ): HtmlElement =
    li(
      onClick.mapTo(()) --> onToggleTheme,
      role := "button",
      cls := "btn",
      title <-- isDarkTheme.map { dark =>
        val theme = if dark then "light" else "dark"
        I18n.t(s"sidebar.theme_${theme}_tooltip")
      },
      child <-- isDarkTheme.map { dark =>
        val label = if dark then I18n.t("sidebar.theme_light") else I18n.t("sidebar.theme_dark")
        val icon = if dark then "fa fa-sun-o" else "fa fa-moon-o"
        div(
          i(cls := icon),
          span(label)
        )
      }
    )

  /**
   * Render runners status button.
   */
  private def runnersStatusButton(
    status: Signal[StatusState],
    setView: Observer[View]
  ): HtmlElement =
    li(
      onClick.mapTo(View.Status) --> setView,
      role := "button",
      title := I18n.t("sidebar.status_tooltip"),
      cls := "btn",
      cls <-- status.map { s =>
        s.sbtRunnerCount match
          case None => "status-unknown"
          case Some(0) => "status-down"
          case Some(_) => "status-up"
      },
      child <-- status.map { s =>
        val (iconClass, label) = s.sbtRunnerCount match
          case None =>
            ("fa fa-times-circle", I18n.t("sidebar.status_unknown"))
          case Some(0) =>
            ("fa fa-times-circle", I18n.t("sidebar.status_down"))
          case Some(_) =>
            ("fa fa-check-circle", I18n.t("sidebar.status_up"))

        div(
          i(cls := iconClass),
          span(label)
        )
      }
    )

  /**
   * View toggle button.
   */
  private def viewToggleButton(
    currentView: Signal[View],
    targetView: View,
    buttonTitle: String,
    faIcon: String,
    setView: Observer[View]
  ): HtmlElement =
    li(
      onClick.mapTo(targetView) --> setView,
      role := "button",
      title := buttonTitle,
      cls := "btn",
      cls <-- currentView.map(v => if v == targetView then "selected" else ""),
      i(cls := s"fa $faIcon"),
      span(buttonTitle)
    )

  /**
   * Editor mode selector dropdown.
   */
  private def editorModeSelector(
    editorMode: Signal[EditorMode],
    setEditorMode: Observer[EditorMode],
    isDarkTheme: Signal[Boolean]
  ): HtmlElement =
    li(
      cls := "btn",
      i(cls := "fa fa-keyboard-o"),
      select(
        value <-- editorMode.map(_.toString),
        cls <-- isDarkTheme.map(dark => s"editor-mode-select ${if dark then "dark" else "light"}"),
        onInput.mapToValue.map {
          case "Default" => Default
          case "Vim" => Vim
          case "Emacs" => Emacs
          case _ => Default
        } --> setEditorMode,
        option(value := "Default", "Default"),
        option(value := "Vim", "Vim"),
        option(value := "Emacs", "Emacs")
      )
    )

  /**
   * Create a sidebar component.
   *
   * @param view Signal containing current view
   * @param setView Observer to change view
   * @param isDarkTheme Signal indicating dark theme state
   * @param status Signal containing status state
   * @param inputs Signal containing current inputs
   * @param editorMode Signal containing editor mode
   * @param setEditorMode Observer to change editor mode
   * @param onToggleTheme Observer to toggle theme
   * @param onOpenHelp Observer to open help modal
   * @param onOpenPrivacyPolicy Observer to open privacy policy modal
   * @param language Language code for i18n
   * @return Sidebar navigation element
   */
  def apply(
    view: Signal[View],
    setView: Observer[View],
    isDarkTheme: Signal[Boolean],
    status: Signal[StatusState],
    inputs: Signal[BaseInputs],
    editorMode: Signal[EditorMode],
    setEditorMode: Observer[EditorMode],
    onToggleTheme: Observer[Unit],
    onOpenHelp: Observer[Unit],
    onOpenPrivacyPolicy: Observer[Unit],
    language: String = "en"
  ): HtmlElement =
    nav(
      cls := "sidebar",

      div(cls := "sidebar-top")(
        img(src := logoSrc, alt := "Scastie Logo", cls := "logo"),

        ul(cls := "sidebar-buttons")(
          viewToggleButton(view, View.Editor, I18n.t("sidebar.editor"), "fa-edit", setView),
          viewToggleButton(view, View.BuildSettings, I18n.t("sidebar.build_settings"), "fa-gear", setView),
          runnersStatusButton(status, setView)
        )
      ),

      div(cls := "sidebar-bottom")(
        ul(cls := "sidebar-buttons")(
          editorModeSelector(editorMode, setEditorMode, isDarkTheme),
          themeButton(isDarkTheme, onToggleTheme),

          li(
            onClick.mapTo(()) --> onOpenPrivacyPolicy,
            role := "button",
            title := I18n.t("sidebar.privacy_policy_tooltip"),
            cls := "btn",
            i(cls := "fa fa-user-secret"),
            span(I18n.t("sidebar.privacy_policy"))
          ),

          li(
            onClick.mapTo(()) --> onOpenHelp,
            role := "button",
            title := I18n.t("sidebar.help_tooltip"),
            cls := "btn",
            i(cls := "fa fa-question-circle"),
            span(I18n.t("sidebar.help"))
          )
        )
      )
    )

  /**
   * Simplified version.
   */
  def apply(
    view: Signal[View],
    setView: Observer[View],
    isDarkTheme: Signal[Boolean],
    onToggleTheme: Observer[Unit]
  ): HtmlElement =
    apply(
      view = view,
      setView = setView,
      isDarkTheme = isDarkTheme,
      status = Val(StatusState.empty),
      inputs = Val(org.scastie.api.SbtInputs.default),
      editorMode = Val(Default),
      setEditorMode = Observer.empty,
      onToggleTheme = onToggleTheme,
      onOpenHelp = Observer.empty,
      onOpenPrivacyPolicy = Observer.empty,
      language = "en"
    )
