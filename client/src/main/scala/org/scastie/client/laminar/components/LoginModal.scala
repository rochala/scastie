package org.scastie.client.laminar.components

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import org.scastie.client.i18n.I18n

/**
 * Login modal component - Laminar version
 *
 * Migrated from: org.scastie.client.components.LoginModal
 */
object LoginModal:

  /**
   * Navigate to login page.
   */
  private def login(): Unit =
    dom.window.location.pathname = "/login"

  /**
   * Render text with embedded link.
   */
  private def renderWithLink(template: String, onLinkClick: Observer[Unit]): HtmlElement =
    val linkRegex = """\{([^}]+)\}""".r

    linkRegex.findFirstMatchIn(template) match
      case Some(m) =>
        val before = template.substring(0, m.start)
        val linkText = m.group(1)
        val after = template.substring(m.end)

        p(
          before,
          a(
            href := "#",
            onClick.preventDefault.stopPropagation.mapTo(()) --> onLinkClick,
            linkText
          ),
          after
        )
      case None =>
        p(template)

  /**
   * Create a login modal element.
   *
   * @param isDarkTheme Signal indicating dark theme state
   * @param isClosed Signal indicating if modal is closed
   * @param onClose Observer to close modal
   * @param onOpenPrivacyPolicy Observer to open privacy policy modal
   * @return Login modal element
   */
  def apply(
    isDarkTheme: Signal[Boolean],
    isClosed: Signal[Boolean],
    onClose: Observer[Unit],
    onOpenPrivacyPolicy: Observer[Unit]
  ): HtmlElement =
    val theme = isDarkTheme.map(dark => if dark then "dark" else "light")

    Modal(
      title = I18n.t("sidebar.login_title"),
      isClosed = isClosed,
      onClose = onClose,
      modalCss = "",  // Will be set dynamically
      modalId = "modal-login",
      content = div(
        cls <-- theme.map(t => s"$t modal-login"),

        button(
          onClick --> Observer[Any] { _ =>
            login()
            onClose.onNext(())
          },
          cls := "github-login",
          i(cls := "fa fa-github"),
          I18n.t("sidebar.login_github")
        ),

        renderWithLink(
          I18n.t("sidebar.login_agreement"),
          onOpenPrivacyPolicy
        )
      )
    )

  /**
   * Static version with boolean states.
   */
  def apply(
    isDarkTheme: Boolean,
    isClosed: Boolean,
    onClose: Observer[Unit],
    onOpenPrivacyPolicy: Observer[Unit]
  ): HtmlElement =
    apply(Val(isDarkTheme), Val(isClosed), onClose, onOpenPrivacyPolicy)
