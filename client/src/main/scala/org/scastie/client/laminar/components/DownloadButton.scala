package org.scastie.client.laminar.components

import com.raquo.laminar.api.L.*
import org.scastie.api.SnippetId
import org.scastie.client.i18n.I18n

/**
 * Download button component - Laminar version
 *
 * Migrated from: org.scastie.client.components.DownloadButton
 */
object DownloadButton:

  /**
   * Create a download button element.
   *
   * @param snippetId Signal containing the snippet ID to download
   * @param language Language code for i18n
   * @return Button element as list item with download link
   */
  def apply(
    snippetId: Signal[SnippetId],
    language: String = "en"
  ): HtmlElement =
    li(
      child <-- snippetId.map { id =>
        val url = id.url
        val fullUrl = s"/api/download/$url"
        val downloadName = url.replaceAll("/", "-") + ".zip"

        a(
          href := fullUrl,
          download := downloadName,
          title := I18n.t("editor.download"),
          role := "button",
          cls := "btn",
          i(cls := "fa fa-download"),
          span(I18n.t("editor.download"))
        )
      }
    )

  /**
   * Static version with non-reactive snippet ID
   */
  def apply(
    snippetId: SnippetId,
    language: String
  ): HtmlElement =
    val url = snippetId.url
    val fullUrl = s"/api/download/$url"
    val downloadName = url.replaceAll("/", "-") + ".zip"

    li(
      a(
        href := fullUrl,
        download := downloadName,
        title := I18n.t("editor.download"),
        role := "button",
        cls := "btn",
        i(cls := "fa fa-download"),
        span(I18n.t("editor.download"))
      )
    )
