package org.scastie.client.laminar

import com.raquo.laminar.api.L.*
import org.scastie.client.{ScastieState, View}
import org.scastie.client.laminar.api.ApiClient
import org.scastie.api.*
import java.util.UUID
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
 * Extended Scastie store with API integration.
 *
 * This extends the basic ScastieStore with all backend functionality
 * from ScastieBackend, including API calls, snippet loading, etc.
 */
class ScastieStoreExtended(
  initialState: ScastieState,
  scastieId: UUID,
  serverUrl: Option[String] = None
) extends ScastieStore(initialState):

  private val apiClient = ApiClient(serverUrl)

  // ===== API Actions =====

  /** Run code action */
  val runObserver: Observer[Unit] = Observer[Unit] { _ =>
    val inputs = currentState.inputs

    // Set running state
    updateState(_.copy(isRunning = true).clearOutputs)

    // Call API
    apiClient.run(inputs).foreach { snippetId =>
      updateState(state =>
        state
          .setSnippetId(snippetId)
          .setRunning(false)
          .setCleanInputs
      )
      // TODO: Start listening for SSE events
    }(unsafeWindowOwner)
  }

  /** Save code action */
  val saveObserver: Observer[Unit] = Observer[Unit] { _ =>
    val state = currentState

    if state.isRunning then
      // Already running, just show editor
      setView(View.Editor)
    else
      // Run the code
      runObserver.onNext(())
  }

  /** Format code action */
  val formatObserver: Observer[Unit] = Observer[Unit] { _ =>
    val state = currentState
    val request = FormatRequest(state.inputs.code, state.inputs.isWorksheetMode)

    apiClient.format(request).foreach { response =>
      response match
        case FormatResponse(Right(formattedCode)) =>
          setCode(formattedCode)
        case FormatResponse(Left(error)) =>
          // TODO: Show error message
          org.scalajs.dom.console.error(s"Format error: $error")
    }(unsafeWindowOwner)
  }

  /** Clear outputs action */
  val clearObserver: Observer[Unit] = Observer[Unit] { _ =>
    updateState(_.clearOutputsPreserveConsole.closeModals)
  }

  /** New snippet action */
  val newSnippetObserver: Observer[Unit] = Observer[Unit] { _ =>
    updateState(state =>
      state
        .copy(isDesktopForced = false)
        .setInputs(SbtInputs.default.copyBaseInput(code = ""))
        .clearOutputs
        .clearSnippetId
        .setChangedInputs
    )
    setView(View.Editor)
  }

  /** Reset build action */
  val resetBuildObserver: Observer[Unit] = Observer[Unit] { _ =>
    updateState(state =>
      state
        .setInputs(SbtInputs.default.copyBaseInput(code = state.inputs.code))
        .clearOutputs
        .clearSnippetId
        .setChangedInputs
    )
    setView(View.Editor)
  }

  /** Toggle worksheet mode */
  val toggleWorksheetObserver: Observer[Unit] = Observer[Unit] { _ =>
    updateState(state =>
      state.setInputs(state.inputs.setWorksheetMode(!state.isWorksheetMode))
    )
  }

  /** Set Scala target */
  def setTarget(target: ScalaTarget): Unit =
    updateState(_.setTarget(target))

  val setTargetObserver: Observer[ScalaTarget] =
    Observer[ScalaTarget](target => setTarget(target))

  /** Add Scala dependency */
  def addScalaDependency(dependency: ScalaDependency, project: Project): Unit =
    updateState(_.addScalaDependency(dependency, project))

  /** Load snippet by ID */
  def loadSnippet(snippetId: SnippetId): Unit =
    apiClient.fetchSnippet(snippetId).foreach { resultOpt =>
      resultOpt.foreach { result =>
        updateState(state =>
          state
            .setInputs(result.inputs)
            .setSnippetId(snippetId)
            .setCleanInputs
            .clearOutputs
        )
      }
    }(unsafeWindowOwner)

  /** Load old snippet by number */
  def loadOldSnippet(id: Int): Unit =
    apiClient.fetchOldSnippet(id).foreach { resultOpt =>
      resultOpt.foreach { result =>
        updateState(state =>
          state
            .setInputs(result.inputs)
            .setCleanInputs
            .clearOutputs
        )
      }
    }(unsafeWindowOwner)

  /** Update current snippet */
  def updateSnippet(): Unit =
    currentState.snippetId.foreach { snippetId =>
      val inputs = currentState.inputs
      apiClient.update(snippetId, inputs).foreach { _ =>
        updateState(_.setCleanInputs)
      }(unsafeWindowOwner)
    }

  /** Delete snippet */
  def deleteSnippet(snippetId: SnippetId): Unit =
    apiClient.delete(snippetId).foreach { _ =>
      // Snippet deleted
      // TODO: Show notification
    }(unsafeWindowOwner)

  /** Set SBT config extra */
  def setSbtConfigExtra(config: String): Unit =
    updateState(_.setSbtConfigExtra(config))

  val setSbtConfigExtraObserver: Observer[String] =
    Observer[String](config => setSbtConfigExtra(config))

  /** Set language */
  def setLanguage(language: String): Unit =
    updateState(_.setLanguage(language))

  val setLanguageObserver: Observer[String] =
    Observer[String](lang => setLanguage(lang))

object ScastieStoreExtended:
  /** Create store with UUID */
  def apply(
    isEmbedded: Boolean = false,
    scastieId: UUID = UUID.randomUUID(),
    serverUrl: Option[String] = None
  ): ScastieStoreExtended =
    new ScastieStoreExtended(
      ScastieState.default(isEmbedded),
      scastieId,
      serverUrl
    )

  /** Create store with custom initial state */
  def apply(
    initialState: ScastieState,
    scastieId: UUID,
    serverUrl: Option[String]
  ): ScastieStoreExtended =
    new ScastieStoreExtended(initialState, scastieId, serverUrl)
