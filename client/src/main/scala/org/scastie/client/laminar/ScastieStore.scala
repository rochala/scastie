package org.scastie.client.laminar

import com.raquo.laminar.api.L.*
import org.scastie.client.{ScastieState, View, StatusState, ModalState}
import org.scastie.api.*

/**
 * Reactive state store for Scastie using Airstream.
 * This is the Laminar equivalent of ScastieBackend + ScastieState from the React version.
 */
class ScastieStore(initialState: ScastieState):

  // ===== Core State =====

  /** Main application state Var - the single source of truth */
  private val stateVar = Var(initialState)

  /** Public read-only signal of the state */
  val stateSignal: Signal[ScastieState] = stateVar.signal

  // ===== Derived Signals =====

  /** Dark theme state */
  val isDarkThemeSignal: Signal[Boolean] =
    stateSignal.map(_.isDarkTheme)

  /** Current code inputs */
  val inputsSignal: Signal[Inputs] =
    stateSignal.map(_.inputs)

  /** Current view (Editor, BuildSettings, etc.) */
  val viewSignal: Signal[View] =
    stateSignal.map(_.view)

  /** Status (running, compilation state, etc.) */
  val statusSignal: Signal[StatusState] =
    stateSignal.map(_.status)

  /** Whether presentation mode is active */
  val isPresentationModeSignal: Signal[Boolean] =
    stateSignal.map(_.isPresentationMode)

  /** Whether desktop mode is forced */
  val isDesktopForcedSignal: Signal[Boolean] =
    stateSignal.map(_.isDesktopForced)

  /** Modal state */
  val modalStateSignal: Signal[ModalState] =
    stateSignal.map(_.modalState)

  /** Editor mode (Normal, Vim, Emacs) */
  val editorModeSignal: Signal[String] =
    stateSignal.map(_.editorMode)

  /** Language for UI */
  val languageSignal: Signal[String] =
    stateSignal.map(_.language)

  /** Whether snippet is currently running */
  val isRunningSignal: Signal[Boolean] =
    stateSignal.map(_.isRunning)

  /** Current code as signal */
  val codeSignal: Signal[String] =
    inputsSignal.map(_.code)

  /** Whether inputs have changed (for dirty state indication) */
  val inputsHasChangedSignal: Signal[Boolean] =
    stateSignal.map(_.inputsHasChanged)

  /** User signal */
  val userSignal: Signal[Option[org.scastie.api.User]] =
    stateSignal.map(_.user)

  // ===== State Update Functions =====

  /** Update state with a function */
  def updateState(f: ScastieState => ScastieState): Unit =
    stateVar.update(f)

  /** Set state to a specific value */
  def setState(newState: ScastieState): Unit =
    stateVar.set(newState)

  /** Get current state (use sparingly - prefer signals) */
  def currentState: ScastieState =
    stateVar.now()

  // ===== Theme Actions =====

  /** Toggle between light and dark theme */
  def toggleTheme(): Unit =
    updateState(s => s.copy(isDarkTheme = !s.isDarkTheme))

  val toggleThemeObserver: Observer[Unit] =
    Observer[Unit](_ => toggleTheme())

  /** Set theme explicitly */
  def setTheme(dark: Boolean): Unit =
    updateState(_.copy(isDarkTheme = dark))

  // ===== View Actions =====

  /** Set the current view */
  def setView(view: View): Unit =
    updateState(_.copy(view = view))

  val setViewObserver: Observer[View] =
    Observer[View](view => setView(view))

  // ===== Code/Input Actions =====

  /** Update the code */
  def setCode(code: String): Unit =
    updateState(s => s.setCode(code))

  val setCodeObserver: Observer[String] =
    Observer[String](code => setCode(code))

  /** Set inputs */
  def setInputs(inputs: Inputs): Unit =
    updateState(_.setInputs(inputs))

  // ===== Editor Mode Actions =====

  /** Set editor mode (Normal, Vim, Emacs) */
  def setEditorMode(mode: String): Unit =
    updateState(_.copy(editorMode = mode))

  val setEditorModeObserver: Observer[String] =
    Observer[String](mode => setEditorMode(mode))

  // ===== Modal Actions =====

  /** Open help modal */
  def openHelpModal(): Unit =
    updateState(s => s.copy(modalState = s.modalState.copy(isHelpModalClosed = false)))

  /** Close help modal */
  def closeHelpModal(): Unit =
    updateState(s => s.copy(modalState = s.modalState.copy(isHelpModalClosed = true)))

  val closeHelpModalObserver: Observer[Unit] =
    Observer[Unit](_ => closeHelpModal())

  /** Open privacy policy modal */
  def openPrivacyPolicyModal(): Unit =
    updateState(s => s.copy(modalState = s.modalState.copy(isPrivacyPolicyModalClosed = false)))

  /** Close privacy policy modal */
  def closePrivacyPolicyModal(): Unit =
    updateState(s => s.copy(modalState = s.modalState.copy(isPrivacyPolicyModalClosed = true)))

  val closePrivacyPolicyModalObserver: Observer[Unit] =
    Observer[Unit](_ => closePrivacyPolicyModal())

  /** Open login modal */
  def openLoginModal(): Unit =
    updateState(s => s.copy(modalState = s.modalState.copy(isLoginModalClosed = false)))

  /** Close login modal */
  def closeLoginModal(): Unit =
    updateState(s => s.copy(modalState = s.modalState.copy(isLoginModalClosed = true)))

  val closeLoginModalObserver: Observer[Unit] =
    Observer[Unit](_ => closeLoginModal())

  // ===== Running State Actions =====

  /** Set running state */
  def setRunning(running: Boolean): Unit =
    updateState(_.copy(isRunning = running))

  // ===== Desktop Mode Actions =====

  /** Toggle desktop mode forcing */
  def toggleForceDesktop(): Unit =
    updateState(s => s.copy(isDesktopForced = !s.isDesktopForced))

object ScastieStore:
  /** Create a store with default state */
  def apply(isEmbedded: Boolean = false): ScastieStore =
    new ScastieStore(ScastieState.default(isEmbedded))

  /** Create a store with custom initial state */
  def apply(initialState: ScastieState): ScastieStore =
    new ScastieStore(initialState)
