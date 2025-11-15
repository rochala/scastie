package org.scastie.client.laminar.components

import com.raquo.laminar.api.L.*

/**
 * Base modal component - Laminar version
 *
 * Migrated from: org.scastie.client.components.Modal
 */
object Modal:

  /**
   * Create a modal dialog element.
   *
   * @param title Modal title
   * @param isClosed Signal indicating if modal is closed
   * @param onClose Observer to close the modal
   * @param modalCss Optional additional CSS classes
   * @param modalId Modal element ID
   * @param content Modal content element
   * @return Modal dialog element
   */
  def apply(
    title: String,
    isClosed: Signal[Boolean],
    onClose: Observer[Unit],
    modalCss: String = "",
    modalId: String = "modal",
    content: HtmlElement
  ): HtmlElement =
    div(
      cls := "modal",
      idAttr := modalId,
      display <-- isClosed.map(closed => if closed then "none" else "block"),

      div(
        cls := "modal-fade-screen",
        onClick.compose(_.stopPropagation).mapTo(()) --> onClose,

        div(
          cls := "modal-window",
          cls := modalCss,
          onClick.compose(_.stopPropagation) --> Observer.empty, // Prevent closing when clicking inside

          div(
            cls := "modal-header",

            div(
              cls := "modal-close",
              onClick.compose(_.stopPropagation).mapTo(()) --> onClose,
              role := "button",
              title := "close modal"
            ),

            h1(title)
          ),

          div(
            cls := "modal-inner",
            content
          )
        )
      )
    )

  /**
   * Static version with boolean closed state.
   */
  def apply(
    title: String,
    isClosed: Boolean,
    onClose: Observer[Unit],
    modalCss: String,
    modalId: String,
    content: HtmlElement
  ): HtmlElement =
    apply(title, Val(isClosed), onClose, modalCss, modalId, content)
