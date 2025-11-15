package org.scastie.client.laminar.components

import com.raquo.laminar.api.L.*
import org.scastie.client.i18n.I18n

/**
 * Prompt modal with confirm/cancel actions - Laminar version
 *
 * Migrated from: org.scastie.client.components.PromptModal
 */
object PromptModal:

  /**
   * Create a prompt modal with action buttons.
   *
   * @param isDarkTheme Signal indicating dark theme state
   * @param modalText Modal title text
   * @param modalId Modal element ID
   * @param isClosed Signal indicating if modal is closed
   * @param onClose Observer to close modal
   * @param actionText Prompt text to display
   * @param actionLabel Label for action button
   * @param onAction Observer for the main action
   * @return Prompt modal element
   */
  def apply(
    isDarkTheme: Signal[Boolean],
    modalText: String,
    modalId: String,
    isClosed: Signal[Boolean],
    onClose: Observer[Unit],
    actionText: String,
    actionLabel: String,
    onAction: Observer[Unit]
  ): HtmlElement =
    Modal(
      title = modalText,
      isClosed = isClosed,
      onClose = onClose,
      modalCss = "modal-reset",
      modalId = modalId,
      content = div(
        p(
          cls := "modal-intro",
          actionText
        ),
        ul(
          li(
            onClick.stopPropagation --> Observer[Any] { _ =>
              onAction.onNext(())
              onClose.onNext(())
            },
            cls := "btn",
            actionLabel
          ),
          li(
            onClick.stopPropagation.mapTo(()) --> onClose,
            cls := "btn",
            I18n.t("Cancel")
          )
        )
      )
    )

  /**
   * Static version with boolean closed state.
   */
  def apply(
    isDarkTheme: Boolean,
    modalText: String,
    modalId: String,
    isClosed: Boolean,
    onClose: Observer[Unit],
    actionText: String,
    actionLabel: String,
    onAction: Observer[Unit]
  ): HtmlElement =
    apply(Val(isDarkTheme), modalText, modalId, Val(isClosed), onClose, actionText, actionLabel, onAction)
