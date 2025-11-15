package org.scastie.client.laminar.components

import com.raquo.laminar.api.L.*
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
 * Privacy policy modal component - Laminar version
 *
 * Migrated from: org.scastie.client.components.PrivacyPolicyModal
 */
object PrivacyPolicyModal:

  // Import privacy policy HTML content
  // Note: This uses JS module import similar to React version
  @js.native
  @JSImport("@scastieRoot/privacy-policy.md", "html")
  private val privacyPolicyHTMLContent: String = js.native

  /**
   * Create a privacy policy modal element.
   *
   * @param isDarkTheme Signal indicating dark theme state
   * @param isClosed Signal indicating if modal is closed
   * @param onClose Observer to close modal
   * @return Privacy policy modal element
   */
  def apply(
    isDarkTheme: Signal[Boolean],
    isClosed: Signal[Boolean],
    onClose: Observer[Unit]
  ): HtmlElement =
    val theme = isDarkTheme.map(dark => if dark then "dark" else "light")

    Modal(
      title = "Scastie privacy policy",
      isClosed = isClosed,
      onClose = onClose,
      modalCss = "", // Will be set dynamically via theme
      modalId = "privacy-policy",
      content = div(
        cls := "markdown-body",
        cls <-- theme,
        inContext { thisNode =>
          // Set innerHTML with the privacy policy content
          thisNode.ref.innerHTML = privacyPolicyHTMLContent
          emptyMod
        }
      )
    )

  /**
   * Static version with boolean states.
   */
  def apply(
    isDarkTheme: Boolean,
    isClosed: Boolean,
    onClose: Observer[Unit]
  ): HtmlElement =
    apply(Val(isDarkTheme), Val(isClosed), onClose)
