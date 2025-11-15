package org.scastie.client.laminar.components

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import org.scastie.api.User
import org.scastie.client.{View, i18n}
import org.scastie.client.i18n.I18n

/**
 * Top navigation bar component - Laminar version
 *
 * Migrated from: org.scastie.client.components.TopBar
 */
object TopBar:

  private def openInNewTab(link: String): Unit =
    dom.window.open(link, "_blank").focus()

  private def feedback(): Unit =
    openInNewTab("https://gitter.im/scalacenter/scastie")

  private def issue(): Unit =
    openInNewTab("https://github.com/scalacenter/scastie/issues/new/choose")

  private def logout(setView: Observer[View]): Unit =
    setView.onNext(View.Editor)
    dom.window.location.pathname = "/logout"

  /**
   * Render profile button/dropdown.
   */
  private def profileButton(
    user: Option[User],
    view: Signal[View],
    setView: Observer[View],
    openLoginModal: Observer[Unit]
  ): HtmlElement =
    user match
      case Some(u) =>
        li(
          cls := "btn dropdown",
          img(
            src := s"${u.avatar_url}&s=30",
            alt := "Your Github Avatar",
            cls := "avatar"
          ),
          span(u.login),
          i(cls := "fa fa-caret-down"),
          ul(
            cls := "subactions",
            li(
              onClick.mapTo(View.CodeSnippets) --> setView,
              role := "link",
              title := I18n.t("topbar.snippets_tooltip"),
              cls := "btn",
              cls <-- view.map(v => if v == View.CodeSnippets then "selected" else ""),
              i(cls := "fa fa-code"),
              I18n.t("topbar.snippets")
            ),
            li(
              role := "link",
              onClick --> Observer[Any](_ => logout(setView)),
              cls := "btn",
              i(cls := "fa fa-sign-out"),
              I18n.t("topbar.logout")
            )
          )
        )

      case None =>
        li(
          role := "link",
          onClick.mapTo(()) --> openLoginModal,
          cls := "btn",
          i(cls := "fa fa-sign-in"),
          I18n.t("topbar.login")
        )

  /**
   * Create a top bar component.
   *
   * @param view Signal containing current view
   * @param setView Observer to change view
   * @param user Signal containing optional user
   * @param openLoginModal Observer to open login modal
   * @param language Signal containing current language
   * @param setLanguage Observer to change language
   * @param isDarkTheme Signal indicating dark theme state
   * @return Top bar navigation element
   */
  def apply(
    view: Signal[View],
    setView: Observer[View],
    user: Signal[Option[User]],
    openLoginModal: Observer[Unit],
    language: Signal[String],
    setLanguage: Observer[String],
    isDarkTheme: Signal[Boolean]
  ): HtmlElement =
    nav(
      cls := "topbar",
      ul(
        cls := "actions",

        // Feedback dropdown
        li(
          cls := "btn dropdown",
          i(cls := "fa fa-comments"),
          span(I18n.t("topbar.feedback")),
          i(cls := "fa fa-caret-down"),
          ul(
            cls := "subactions",
            li(
              onClick --> Observer[Any](_ => feedback()),
              role := "link",
              title := I18n.t("topbar.feedback_tooltip"),
              cls := "btn",
              i(cls := "fa fa-gitter"),
              span(I18n.t("topbar.gitter"))
            ),
            li(
              onClick --> Observer[Any](_ => issue()),
              role := "link",
              title := I18n.t("topbar.github_tooltip"),
              cls := "btn",
              i(cls := "fa fa-github"),
              span(I18n.t("topbar.github_issues"))
            )
          )
        ),

        // Language selector
        li(
          cls := "btn",
          label(I18n.t("topbar.language_label")),
          select(
            value <-- language,
            cls <-- isDarkTheme.map(dark => s"language-select ${if dark then "dark" else "light"}"),
            onInput.mapToValue --> setLanguage,
            option(value := "en", "English")
          )
        ),

        // Profile button (dynamic based on user state)
        child <-- Signal.combine(user, view).map { case (u, v) =>
          profileButton(u, view, setView, openLoginModal)
        }
      )
    )

  /**
   * Simplified version with static user.
   */
  def apply(
    view: Signal[View],
    setView: Observer[View],
    user: Option[User],
    openLoginModal: Observer[Unit],
    language: Signal[String],
    setLanguage: Observer[String],
    isDarkTheme: Signal[Boolean]
  ): HtmlElement =
    apply(view, setView, Val(user), openLoginModal, language, setLanguage, isDarkTheme)
