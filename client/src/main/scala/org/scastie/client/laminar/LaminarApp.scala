package org.scastie.client.laminar

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import org.scastie.client.{ScastieState, LocalStorage, View}
import org.scastie.client.laminar.components.{MainPanel, SideBar, HelpModal, LoginModal, PrivacyPolicyModal}
import org.scastie.api.*

/**
 * Main Laminar application entry point.
 * This replaces the React-based Scastie component with a fully functional Laminar app.
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

    // Load snippet if provided
    snippetId.foreach { id =>
      // TODO: Load snippet via API
      dom.console.log(s"Loading snippet: $id")
    }

    // Create the root element
    val appElement = createAppElement(store, isEmbedded)

    // Mount to DOM
    render(containerNode, appElement)

    // Set up document title updates
    setupDocumentTitle(store, isEmbedded)

  /**
   * Load initial state from LocalStorage or create default
   */
  private def loadInitialState(isEmbedded: Boolean): ScastieState =
    if isEmbedded then
      ScastieState.default(isEmbedded = true)
    else
      LocalStorage.load.getOrElse(ScastieState.default(isEmbedded = false))

  /**
   * Set up document title updates based on state
   */
  private def setupDocumentTitle(store: ScastieStore, isEmbedded: Boolean): Unit =
    if !isEmbedded then
      Signal.combine(store.codeSignal, store.inputsHasChangedSignal).foreach {
        case (code, hasChanged) =>
          val title =
            if code.isEmpty then "Scastie"
            else s"$code - Scastie"

          dom.document.title =
            if hasChanged then s"* $title"
            else title
      }(unsafeWindowOwner)

  /**
   * Create the root application element
   */
  private def createAppElement(
    store: ScastieStore,
    isEmbedded: Boolean
  ): Div =
    div(
      cls := "app scastie-laminar",
      cls <-- store.isDarkThemeSignal.map(dark => if dark then "dark" else "light"),
      cls <-- store.isDesktopForcedSignal.map(forced => if forced then "force-desktop" else ""),
      cls <-- store.isPresentationModeSignal.map(pres => if pres then "presentation-mode" else ""),

      // Sidebar (not shown in embedded mode)
      child <-- Val(!isEmbedded).map { showSidebar =>
        if showSidebar then
          SideBar(
            view = store.viewSignal,
            setView = store.setViewObserver,
            isDarkTheme = store.isDarkThemeSignal,
            status = store.statusSignal,
            inputs = store.inputsSignal,
            editorMode = store.editorModeSignal.map { mode =>
              mode match
                case "Vim" => org.scastie.api.Vim
                case "Emacs" => org.scastie.api.Emacs
                case _ => org.scastie.api.Default
            },
            setEditorMode = store.setEditorModeObserver.contramap(_.toString),
            onToggleTheme = store.toggleThemeObserver,
            onOpenHelp = Observer[Unit](_ => store.openHelpModal()),
            onOpenPrivacyPolicy = Observer[Unit](_ => store.openPrivacyPolicyModal())
          )
        else
          emptyNode
      },

      // Main panel with editor and views
      MainPanel(store, isEmbedded),

      // Modals
      HelpModal(
        isDarkTheme = store.isDarkThemeSignal,
        isClosed = store.modalStateSignal.map(_.isHelpModalClosed),
        onClose = store.closeHelpModalObserver
      ),

      LoginModal(
        isDarkTheme = store.isDarkThemeSignal,
        isClosed = store.modalStateSignal.map(_.isLoginModalClosed),
        onClose = store.closeLoginModalObserver,
        onOpenPrivacyPolicy = Observer[Unit](_ => store.openPrivacyPolicyModal())
      ),

      PrivacyPolicyModal(
        isDarkTheme = store.isDarkThemeSignal,
        isClosed = store.modalStateSignal.map(_.isPrivacyPolicyModalClosed),
        onClose = store.closePrivacyPolicyModalObserver
      )

      // TODO: Add remaining modals (Embedded, NewSnippet, Reset, Share)
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
