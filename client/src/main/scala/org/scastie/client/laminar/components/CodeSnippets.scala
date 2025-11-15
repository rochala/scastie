package org.scastie.client.laminar.components

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import org.scastie.api.*
import org.scastie.client.{View, Page}
import org.scastie.client.i18n.I18n
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
 * Code snippets component - Laminar version
 *
 * Displays user's saved code snippets with share and delete functionality.
 *
 * Migrated from: org.scastie.client.components.CodeSnippets
 */
object CodeSnippets:

  /**
   * Sort snippets by time, keeping only the latest update of each snippet.
   */
  private def sortSnippets(xs: List[SnippetSummary]): List[SnippetSummary] =
    xs.groupBy(_.snippetId.base64UUID)
      .toList
      .flatMap { case (_, snippets) =>
        List(snippets.sortBy(_.snippetId.user.map(_.update).getOrElse(0)).last)
      }
      .sortBy(_.time)
      .reverse

  /**
   * Render a single snippet with share and delete actions.
   */
  private def renderSnippet(
    summary: SnippetSummary,
    user: User,
    isDarkTheme: Boolean,
    isShareModalClosed: Signal[Boolean],
    closeShareModal: Observer[Unit],
    openShareModal: Observer[Unit],
    deleteSnippet: Observer[SnippetSummary],
    navigateToSnippet: Observer[SnippetId]
  ): HtmlElement =
    val page = Page.fromSnippetId(summary.snippetId)
    val update = summary.snippetId.user.map(_.update.toString).getOrElse("")
    val snippetUrl = s"/${summary.snippetId.url}"

    div(
      cls := "snippet",

      // Share modal
      CopyModal(
        isDarkTheme = Val(isDarkTheme),
        title = Val(I18n.t("editor.embed_title")),
        subtitle = Val(I18n.t("editor.share_snippet")),
        modalId = "share-modal-" + summary.snippetId.url.replace(".", "-"),
        content = Val(snippetUrl),
        isClosed = isShareModalClosed,
        onClose = closeShareModal
      ),

      // Snippet header
      div(
        cls := "header",
        "/" + summary.snippetId.base64UUID,
        span(" - "),
        div(cls := "clear-mobile"),
        span(cls := "update", I18n.t("snippets.update") + update),
        div(cls := "actions",
          li(
            onClick.mapTo(()) --> openShareModal,
            cls := "btn",
            title := I18n.t("snippets.share"),
            role := "button",
            i(cls := "fa fa-share-alt")
          ),
          li(
            cls := "btn",
            role := "button",
            title := I18n.t("snippets.delete"),
            onClick.mapTo(summary) --> deleteSnippet,
            i(cls := "fa fa-trash")
          )
        )
      ),

      // Snippet code preview
      div(
        cls := "codesnippet",
        role := "button",
        onClick.mapTo(summary.snippetId) --> navigateToSnippet,
        pre(cls := "code", summary.summary)
      )
    )

  /**
   * Create a code snippets element.
   *
   * @param view Signal with the current view
   * @param user Signal with the current user
   * @param isDarkTheme Signal indicating dark theme state
   * @param snippets Signal with the list of snippet summaries
   * @param shareModalSnippetId Signal with the snippet ID for which share modal is open (None if closed)
   * @param closeShareModal Observer to close share modal
   * @param openShareModal Observer to open share modal for a snippet
   * @param deleteSnippet Observer to delete a snippet
   * @param navigateToSnippet Observer to navigate to a snippet
   * @param loadProfile Observer to load user profile
   * @return Code snippets element
   */
  def apply(
    view: Signal[View],
    user: Signal[Option[User]],
    isDarkTheme: Signal[Boolean],
    snippets: Signal[List[SnippetSummary]],
    shareModalSnippetId: Signal[Option[SnippetId]],
    closeShareModal: Observer[Unit],
    openShareModal: Observer[SnippetId],
    deleteSnippet: Observer[SnippetSummary],
    navigateToSnippet: Observer[SnippetId],
    loadProfile: Observer[Unit]
  ): HtmlElement =

    // Load profile when view changes to CodeSnippets
    val _ = view.changes
      .filter(_ == View.CodeSnippets)
      .mapTo(()) --> loadProfile

    div(
      cls := "code-snippets-container",

      child <-- user.map {
        case Some(u) =>
          div(
            // User avatar
            div(
              cls := "avatar",
              img(
                src := u.avatar_url + "&s=70",
                alt := "Your Github Avatar",
                cls := "image-button avatar"
              )
            ),

            // User name
            h2(u.name.getOrElse("")),

            // User login
            div(
              cls := "username",
              i(cls := "fa fa-github"),
              u.login
            ),

            // Snippets section
            h2(I18n.t("snippets.saved")),
            div(
              cls := "snippets",

              // Empty state
              child <-- snippets.map { sums =>
                if sums.isEmpty then
                  p(I18n.t("snippets.empty"))
                else
                  emptyNode
              },

              // Snippet list
              children <-- Signal.combine(snippets, isDarkTheme, shareModalSnippetId).map {
                case (sums, dark, shareSnippetId) =>
                  sortSnippets(sums).map { summary =>
                    div(
                      cls := "group",
                      key := summary.snippetId.base64UUID,
                      renderSnippet(
                        summary,
                        u,
                        dark,
                        shareSnippetId.map(_ == Some(summary.snippetId)),
                        closeShareModal,
                        openShareModal.contramap[Unit](_ => summary.snippetId),
                        deleteSnippet,
                        navigateToSnippet
                      )
                    )
                  }
              }
            )
          )

        case None =>
          div(
            cls := "code-snippets-empty",
            p(I18n.t("snippets.login_required"))
          )
      }
    )

  /**
   * Static version with concrete values.
   */
  def apply(
    view: View,
    user: Option[User],
    isDarkTheme: Boolean,
    snippets: List[SnippetSummary],
    shareModalSnippetId: Option[SnippetId],
    closeShareModal: Observer[Unit],
    openShareModal: Observer[SnippetId],
    deleteSnippet: Observer[SnippetSummary],
    navigateToSnippet: Observer[SnippetId],
    loadProfile: Observer[Unit]
  ): HtmlElement =
    apply(
      Val(view),
      Val(user),
      Val(isDarkTheme),
      Val(snippets),
      Val(shareModalSnippetId),
      closeShareModal,
      openShareModal,
      deleteSnippet,
      navigateToSnippet,
      loadProfile
    )
