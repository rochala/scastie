package org.scastie.client.laminar

import com.raquo.laminar.api.L.*
import org.scastie.client.{ScastieState, View}
import org.scastie.client.laminar.api.ApiClient
import org.scastie.client.laminar.sse.ScastieEventStream
import org.scastie.api.*
import java.util.UUID
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Success, Failure}

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

  // ===== Snippet Management State =====

  private val snippetSummariesVar = Var[List[SnippetSummary]](List.empty)
  val snippetSummariesSignal: Signal[List[SnippetSummary]] = snippetSummariesVar.signal

  // ===== SSE Stream Management =====

  private var progressStream: Option[ScastieEventStream[SnippetProgress]] = None
  private var statusStream: Option[ScastieEventStream[StatusProgress]] = None

  private def connectProgressStream(snippetId: SnippetId): Unit =
    // Close any existing stream
    progressStream.foreach(_.close())

    val apiBase = serverUrl.getOrElse("")
    val targetType = if currentState.inputs.target.targetType == ScalaTargetType.ScalaCli then "Scala-CLI" else "sbt"

    ScastieEventStream.connect[SnippetProgress](
      eventSourceUri = s"$apiBase/api/progress-sse/${snippetId.url}",
      websocketUri = s"$apiBase/api/progress-ws/${snippetId.url}",
      onMessage = { progress =>
        updateState(_.addProgress(progress))
        progress.isDone // Return true to close stream when done
      },
      onOpen = () => (),
      onError = error => org.scalajs.dom.console.error(s"Progress stream error: $error"),
      onClose = reason => progressStream = None,
      onConnectionError = error => org.scalajs.dom.console.error(s"Progress connection error: $error")
    ) match
      case Success(stream) =>
        progressStream = Some(stream)
      case Failure(error) =>
        org.scalajs.dom.console.error(s"Failed to connect progress stream: $error")
        updateState(_.setRunning(false))

  private def connectStatusStream(): Unit =
    // Close any existing stream
    statusStream.foreach(_.close())

    val apiBase = serverUrl.getOrElse("")

    ScastieEventStream.connect[StatusProgress](
      eventSourceUri = s"$apiBase/api/status-sse",
      websocketUri = s"$apiBase/api/status-ws",
      onMessage = { status =>
        status match
          case StatusProgress.KeepAlive => () // Ignore keep-alive
          case _ => updateState(_.addStatus(status))
        false // Don't close on status updates
      },
      onOpen = () => (),
      onError = error => org.scalajs.dom.console.error(s"Status stream error: $error"),
      onClose = reason => statusStream = None,
      onConnectionError = error => org.scalajs.dom.console.error(s"Status connection error: $error")
    ) match
      case Success(stream) =>
        statusStream = Some(stream)
      case Failure(error) =>
        org.scalajs.dom.console.error(s"Failed to connect status stream: $error")

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
          .setCleanInputs
      )
      // Connect to progress stream for real-time updates
      connectProgressStream(snippetId)
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

  val addScalaDependencyObserver: Observer[(ScalaDependency, Project)] =
    Observer[(ScalaDependency, Project)] { case (dependency, project) =>
      addScalaDependency(dependency, project)
    }

  /** Remove Scala dependency */
  def removeScalaDependency(dependency: ScalaDependency): Unit =
    updateState(_.removeScalaDependency(dependency))

  val removeScalaDependencyObserver: Observer[ScalaDependency] =
    Observer[ScalaDependency](dependency => removeScalaDependency(dependency))

  /** Update dependency version */
  def updateDependencyVersion(dependency: ScalaDependency, version: String): Unit =
    updateState(_.updateDependencyVersion(dependency, version))

  val updateDependencyVersionObserver: Observer[(ScalaDependency, String)] =
    Observer[(ScalaDependency, String)] { case (dependency, version) =>
      updateDependencyVersion(dependency, version)
    }

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

  /** Load user snippets */
  def loadUserSnippets(): Unit =
    apiClient.fetchUserSnippets().foreach { summaries =>
      snippetSummariesVar.set(summaries)
    }(unsafeWindowOwner)

  val loadUserSnippetsObserver: Observer[Unit] =
    Observer[Unit](_ => loadUserSnippets())

  /** Delete snippet */
  def deleteSnippet(summary: SnippetSummary): Unit =
    apiClient.delete(summary.snippetId).foreach { _ =>
      snippetSummariesVar.update(_.filterNot(_ == summary))
    }(unsafeWindowOwner)

  val deleteSnippetObserver: Observer[SnippetSummary] =
    Observer[SnippetSummary](summary => deleteSnippet(summary))

  /** Navigate to snippet (load it) */
  def navigateToSnippet(snippetId: SnippetId): Unit =
    loadSnippet(snippetId)
    setView(View.Editor)

  val navigateToSnippetObserver: Observer[SnippetId] =
    Observer[SnippetId](id => navigateToSnippet(id))

  // ===== Initialization and Cleanup =====

  /** Initialize status stream for server health monitoring */
  def initializeStatusStream(): Unit =
    connectStatusStream()

  /** Cleanup: close all active SSE/WebSocket connections */
  def cleanup(): Unit =
    progressStream.foreach(_.close())
    statusStream.foreach(_.close())
    progressStream = None
    statusStream = None

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
