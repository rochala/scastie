package org.scastie.client.laminar.components

import com.raquo.laminar.api.L.*
import org.scastie.client.components.editor.EditorKeymaps
import org.scastie.client.i18n.I18n

/**
 * Help modal component - Laminar version
 *
 * Migrated from: org.scastie.client.components.HelpModal
 */
object HelpModal:

  /**
   * Generate an external link.
   */
  private def generateATag(url: String, text: String): HtmlElement =
    a(
      href := url,
      target := "_blank",
      rel := "nofollow",
      text
    )

  /**
   * Render text with embedded element.
   */
  private def renderWithElement(template: String, elementBuilder: String => HtmlElement): HtmlElement =
    val elementRegex = """\{([^}]+)\}""".r

    elementRegex.findFirstMatchIn(template) match
      case Some(m) =>
        val before = template.substring(0, m.start)
        val elementContent = m.group(1)
        val element = elementBuilder(elementContent)
        val after = template.substring(m.end)

        p(before, element, after)
      case None =>
        p(template)

  /**
   * Create a help modal element.
   *
   * @param isDarkTheme Signal indicating dark theme state
   * @param isClosed Signal indicating if modal is closed
   * @param onClose Observer to close modal
   * @return Help modal element
   */
  def apply(
    isDarkTheme: Signal[Boolean],
    isClosed: Signal[Boolean],
    onClose: Observer[Unit]
  ): HtmlElement =
    Modal(
      title = I18n.t("help.title"),
      isClosed = isClosed,
      onClose = onClose,
      modalCss = "",
      modalId = "long-help",
      content = div(
        cls := "markdown-body",

        p(I18n.t("help.description")),

        p(
          renderWithElement(
            I18n.t("help.sublime_support"),
            content => generateATag(
              "https://sublime-text-unofficial-documentation.readthedocs.org/en/latest/reference/keyboard_shortcuts_osx.html",
              content
            )
          )
        ),

        h2(I18n.t("help.editor_modes")),
        p(
          I18n.t("help.editor_modes_1"),
          I18n.t("help.editor_modes_2"),
          I18n.t("help.editor_modes_3")
        ),

        h2(s"${I18n.t("help.save")} (${EditorKeymaps.saveOrUpdate.getName})"),
        p(I18n.t("help.save_description")),

        h2(s"${I18n.t("editor.new")} (${EditorKeymaps.openNewSnippetModal.getName})"),
        p(I18n.t("help.new_description")),

        h2(s"${I18n.t("editor.clear_messages")} (${EditorKeymaps.clear.getName})"),
        p(I18n.t("help.clear_messages_description")),

        h2(s"${I18n.t("editor.format")} (${EditorKeymaps.format.getName})"),
        p(
          renderWithElement(
            I18n.t("help.format_description"),
            content => generateATag(
              "https://scalameta.org/scalafmt/docs/configuration.html#disabling-or-customizing-formatting",
              content
            )
          )
        ),

        h2(I18n.t("editor.worksheet")),
        p(
          renderWithElement(
            I18n.t("help.worksheet_description"),
            content => code(content)
          )
        ),
        p(I18n.t("help.worksheet_packages_warning")),

        h2(I18n.t("editor.download")),
        p(I18n.t("help.download_description")),

        h2(I18n.t("editor.embed")),
        p(I18n.t("help.embed_description")),

        h2(s"${I18n.t("console.title")} (${EditorKeymaps.console.getName})"),
        p(I18n.t("help.console_description"))
      )
    )

  /**
   * Static version with boolean closed state.
   */
  def apply(
    isDarkTheme: Boolean,
    isClosed: Boolean,
    onClose: Observer[Unit]
  ): HtmlElement =
    apply(Val(isDarkTheme), Val(isClosed), onClose)
