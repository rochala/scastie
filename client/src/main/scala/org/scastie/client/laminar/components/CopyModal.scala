package org.scastie.client.laminar.components

import com.raquo.laminar.api.L.*
import org.scalajs.dom

/**
 * Copy modal with clipboard functionality - Laminar version
 *
 * Migrated from: org.scastie.client.components.CopyModal
 */
object CopyModal:

  /**
   * Copy text to clipboard using the DOM API.
   */
  private def copyToClipboard(element: dom.Element, content: String): Unit =
    val range = dom.document.createRange()
    val selection = dom.window.getSelection()
    range.selectNodeContents(element)
    selection.removeAllRanges()
    selection.addRange(range)

    if !dom.document.execCommand("copy") then
      dom.window.alert("Cannot copy to clipboard")

    selection.removeAllRanges()

  /**
   * Create a copy modal with clipboard functionality.
   *
   * @param isDarkTheme Signal indicating dark theme state
   * @param title Modal title
   * @param subtitle Modal subtitle/instructions
   * @param content Text content to copy
   * @param modalId Modal element ID
   * @param isClosed Signal indicating if modal is closed
   * @param onClose Observer to close modal
   * @return Copy modal element
   */
  def apply(
    isDarkTheme: Signal[Boolean],
    title: String,
    subtitle: String,
    content: String,
    modalId: String,
    isClosed: Signal[Boolean],
    onClose: Observer[Unit]
  ): HtmlElement =
    Modal(
      title = title,
      isClosed = isClosed,
      onClose = onClose,
      modalCss = "modal-share",
      modalId = modalId,
      content = div(
        p(
          cls := "modal-intro",
          subtitle
        ),
        div(
          cls := "snippet-link",

          // Copyable text div
          div(
            cls := "link-to-copy",
            inContext { thisNode =>
              onClick --> Observer[Any] { _ =>
                copyToClipboard(thisNode.ref, content)
              }
            },
            content
          ),

          // Copy button
          div(
            title := "Copy to Clipboard",
            cls := "snippet-clip clipboard-copy",
            inContext { thisNode =>
              onClick --> Observer[Any] { _ =>
                // Copy from the sibling div
                thisNode.ref.previousElementSibling match
                  case elem: dom.Element =>
                    copyToClipboard(elem, content)
                  case _ => ()
              }
            },
            i(cls := "fa fa-clipboard")
          )
        )
      )
    )

  /**
   * Static version with boolean closed state.
   */
  def apply(
    isDarkTheme: Boolean,
    title: String,
    subtitle: String,
    content: String,
    modalId: String,
    isClosed: Boolean,
    onClose: Observer[Unit]
  ): HtmlElement =
    apply(Val(isDarkTheme), title, subtitle, content, modalId, Val(isClosed), onClose)
