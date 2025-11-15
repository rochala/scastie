package org.scastie.client.laminar.components

import com.raquo.laminar.api.L.*
import org.scastie.client.{View, ScastieState}
import org.scastie.client.laminar.{ScastieStore, ScastieStoreExtended}
import org.scastie.client.laminar.editor.CodeMirrorEditor

/**
 * Main panel component containing the editor and other views - Laminar version
 *
 * Migrated from: org.scastie.client.components.MainPanel
 */
object MainPanel:

  // Helper functions to get observers from store
  private def getRunObserver(store: ScastieStore): Observer[Unit] =
    store match
      case extended: ScastieStoreExtended => extended.saveObserver
      case _ => Observer.empty

  private def getClearObserver(store: ScastieStore): Observer[Unit] =
    store match
      case extended: ScastieStoreExtended => extended.clearObserver
      case _ => Observer.empty

  private def getFormatObserver(store: ScastieStore): Observer[Unit] =
    store match
      case extended: ScastieStoreExtended => extended.formatObserver
      case _ => Observer.empty

  /**
   * Create a main panel component.
   *
   * @param store The Scastie store (can be basic or extended)
   * @param isEmbedded Whether this is embedded mode
   * @return Main panel element
   */
  def apply(
    store: ScastieStore,
    isEmbedded: Boolean = false
  ): HtmlElement =
    div(
      cls := "main-panel",

      // Top bar (not shown in embedded mode)
      child <-- Val(!isEmbedded).map { showTopBar =>
        if showTopBar then
          TopBar(
            view = store.viewSignal,
            setView = store.setViewObserver,
            user = store.stateSignal.map(_.user),
            openLoginModal = Observer.empty, // TODO: Wire to modal
            language = store.languageSignal,
            setLanguage = Observer.empty, // TODO: Wire to language setter
            isDarkTheme = store.isDarkThemeSignal
          )
        else
          emptyNode
      },

      // Main content area
      div(
        cls := "main-content",

        // Editor top bar
        EditorTopBar(
          view = store.viewSignal,
          isRunning = store.isRunningSignal,
          onRun = getRunObserver(store),
          onClear = getClearObserver(store),
          onFormat = getFormatObserver(store)
        ),

        // View-dependent content
        child <-- store.viewSignal.map {
          case View.Editor =>
            createEditorView(store)

          case View.BuildSettings =>
            createBuildSettingsView(store)

          case View.CodeSnippets =>
            createCodeSnippetsView(store)

          case View.Status =>
            createStatusView(store)
        }
      )
    )

  /**
   * Create editor view.
   */
  private def createEditorView(store: ScastieStore): HtmlElement =
    div(
      cls := "editor-view",

      CodeMirrorEditor(
        code = store.codeSignal,
        onCodeChange = store.setCodeObserver,
        config = store.stateSignal.map { state =>
          CodeMirrorEditor.EditorConfig(
            language = "scala",
            theme = if state.isDarkTheme then "dark" else "light",
            readOnly = state.isRunning,
            vim = state.editorMode == "Vim",
            emacs = state.editorMode == "Emacs"
          )
        }
      ),

      // Console
      ConsoleComponent(
        isOpen = store.stateSignal.map(_.consoleState.isOpen),
        isRunning = store.isRunningSignal,
        isEmbedded = false,
        consoleOutputs = store.stateSignal.map(_.outputs.console),
        onRun = Observer.empty, // TODO: Wire to run action
        setView = store.setViewObserver,
        onClose = Observer.empty, // TODO: Wire to console close
        onOpen = Observer.empty // TODO: Wire to console open
      )
    )

  /**
   * Create build settings view.
   */
  private def createBuildSettingsView(store: ScastieStore): HtmlElement =
    store match
      case extended: ScastieStoreExtended =>
        BuildSettings(
          visible = Val(true),
          inputs = extended.inputsSignal,
          isDarkTheme = extended.isDarkThemeSignal,
          isBuildDefault = extended.isBuildDefaultSignal,
          isResetModalClosed = extended.isResetModalClosedSignal,
          setTarget = extended.setTargetObserver,
          closeResetModal = extended.closeResetModalObserver,
          resetBuild = extended.resetBuildObserver,
          openResetModal = extended.openResetModalObserver,
          sbtConfigChange = extended.setSbtConfigExtraObserver,
          removeScalaDependency = extended.removeScalaDependencyObserver,
          updateDependencyVersion = extended.updateDependencyVersionObserver,
          addScalaDependency = extended.addScalaDependencyObserver,
          language = extended.languageSignal
        )
      case _ =>
        div(
          cls := "build-settings-view",
          h2("Build Settings"),
          p("Build settings require extended store with API integration")
        )

  /**
   * Create code snippets view.
   */
  private def createCodeSnippetsView(store: ScastieStore): HtmlElement =
    store match
      case extended: ScastieStoreExtended =>
        CodeSnippets(
          view = extended.viewSignal,
          user = extended.userSignal,
          isDarkTheme = extended.isDarkThemeSignal,
          snippets = extended.snippetSummariesSignal,
          shareModalSnippetId = extended.shareModalSnippetIdSignal,
          closeShareModal = extended.closeShareModalObserver,
          openShareModal = extended.openShareModalObserver,
          deleteSnippet = extended.deleteSnippetObserver,
          navigateToSnippet = extended.navigateToSnippetObserver,
          loadProfile = extended.loadUserSnippetsObserver
        )
      case _ =>
        div(
          cls := "code-snippets-view",
          h2("Code Snippets"),
          p("Code snippets require extended store with API integration")
        )

  /**
   * Create status view.
   */
  private def createStatusView(store: ScastieStore): HtmlElement =
    div(
      cls := "status-view",
      StatusComponent(
        state = store.statusSignal,
        isAdmin = store.stateSignal.map(_.user.exists(_.isAdmin)),
        inputs = store.inputsSignal
      )
    )
