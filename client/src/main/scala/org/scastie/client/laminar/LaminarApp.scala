package org.scastie.client.laminar

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import org.scastie.client.{ScastieState, LocalStorage}
import org.scastie.api.*

/**
 * Main Laminar application entry point.
 * This will replace the React-based Scastie component.
 */
object LaminarApp:

  /**
   * Initialize and render the Laminar application.
   *
   * @param containerNode The DOM node to render into
   * @param isEmbedded Whether this is embedded mode
   * @param snippetId Optional snippet ID to load
   */
  def render(
    containerNode: dom.Element,
    isEmbedded: Boolean = false,
    snippetId: Option[SnippetId] = None
  ): Unit =

    // Initialize the store with appropriate state
    val initialState = loadInitialState(isEmbedded)
    val store = ScastieStore(initialState)

    // Create the root element
    val appElement = createAppElement(store, isEmbedded)

    // Mount to DOM
    render(containerNode, appElement)

  /**
   * Load initial state from LocalStorage or create default
   */
  private def loadInitialState(isEmbedded: Boolean): ScastieState =
    if isEmbedded then
      ScastieState.default(isEmbedded = true)
    else
      LocalStorage.load.getOrElse(ScastieState.default(isEmbedded = false))

  /**
   * Create the root application element
   */
  private def createAppElement(
    store: ScastieStore,
    isEmbedded: Boolean
  ): Div =
    div(
      cls := "laminar-app",
      cls <-- store.isDarkThemeSignal.map(dark => if dark then "dark" else "light"),
      cls <-- store.isDesktopForcedSignal.map(forced => if forced then "force-desktop" else ""),

      // Application content
      child <-- store.stateSignal.map { state =>
        createMainContent(store, state, isEmbedded)
      }
    )

  /**
   * Create main content based on state
   */
  private def createMainContent(
    store: ScastieStore,
    state: ScastieState,
    isEmbedded: Boolean
  ): HtmlElement =
    div(
      cls := "scastie-main",

      h1("Scastie - Laminar Migration"),

      p("This is the Laminar version of Scastie."),

      div(
        cls := "theme-toggle",
        button(
          "Toggle Theme",
          onClick --> store.toggleThemeObserver
        )
      ),

      div(
        cls := "state-display",
        p(s"Theme: ${if state.isDarkTheme then "Dark" else "Light"}"),
        p(s"Embedded: $isEmbedded"),
        p(s"Scala Version: ${state.inputs.target.scalaVersion}")
      ),

      // Placeholder for editor
      div(
        cls := "editor-placeholder",
        p("Editor will be integrated here"),
        textArea(
          cls := "code-editor-temp",
          placeholder := "Code editor will be integrated with CodeMirror...",
          rows := 20,
          cols := 80,
          controlled(
            value <-- store.codeSignal,
            onInput.mapToValue --> store.setCodeObserver
          )
        )
      )
    )

  /**
   * Bootstrap function for development/testing
   */
  def main(args: Array[String]): Unit =
    dom.document.addEventListener("DOMContentLoaded", { (_: dom.Event) =>
      val container = dom.document.getElementById("root")
      if container != null then
        render(container, isEmbedded = false)
      else
        dom.console.error("Root container not found!")
    })
