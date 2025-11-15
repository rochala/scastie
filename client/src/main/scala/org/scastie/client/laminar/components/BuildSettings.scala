package org.scastie.client.laminar.components

import com.raquo.laminar.api.L.*
import org.scastie.api.*
import org.scastie.client.i18n.I18n

/**
 * Build settings component - Laminar version
 *
 * Migrated from: org.scastie.client.components.BuildSettings
 */
object BuildSettings:

  /**
   * Render text with embedded link.
   */
  private def renderWithElement(template: String, elementBuilder: String => HtmlElement): HtmlElement =
    val elementRegex = """\{([^}]+)\}""".r
    elementRegex.findFirstMatchIn(template) match
      case Some(m) =>
        val before = template.substring(0, m.start)
        val elementContent = m.group(1)
        val element = elementBuilder(elementContent)
        val after = template.substring(m.end)
        span(before, element, after)
      case None =>
        span(template)

  /**
   * Render reset button with confirmation modal.
   */
  private def renderResetButton(
    inputs: Signal[BaseInputs],
    isBuildDefault: Signal[Boolean],
    isResetModalClosed: Signal[Boolean],
    closeResetModal: Observer[Unit],
    resetBuild: Observer[Unit],
    openResetModal: Observer[Unit],
    isDarkTheme: Signal[Boolean]
  ): HtmlElement =
    div(
      PromptModal(
        isDarkTheme = isDarkTheme,
        modalText = Val(I18n.t("build.reset_title")),
        modalId = "reset-build-modal",
        isClosed = isResetModalClosed,
        onClose = closeResetModal,
        actionText = Val(I18n.t("build.reset_confirmation")),
        actionLabel = Val(I18n.t("build.reset")),
        onAction = resetBuild
      ),
      div(
        hidden <-- Signal.combine(isBuildDefault, inputs).map { case (isDefault, inp) =>
          isDefault || inp.target.targetType == ScalaTargetType.ScalaCli
        },
        title := I18n.t("build.reset_tooltip"),
        onClick.mapTo(()) --> openResetModal,
        role := "button",
        cls := "btn",
        I18n.t("build.reset")
      )
    )

  /**
   * Render Scaladex search panel.
   */
  private def scaladexSearch(
    sbtInputs: Signal[SbtInputs],
    removeScalaDependency: Observer[ScalaDependency],
    updateDependencyVersion: Observer[(ScalaDependency, String)],
    addScalaDependency: Observer[(ScalaDependency, Project)],
    isDarkTheme: Signal[Boolean],
    language: Signal[String]
  ): HtmlElement =
    ScaladexSearch(
      libraries = sbtInputs.map(_.libraries),
      scalaTarget = sbtInputs.map(_.target),
      removeScalaDependency = removeScalaDependency,
      updateDependencyVersion = updateDependencyVersion,
      addScalaDependency = addScalaDependency,
      isDarkTheme = isDarkTheme,
      language = language
    )

  /**
   * Render SBT extra configuration panel.
   */
  private def sbtExtraConfigurationPanel(
    sbtInputs: Signal[SbtInputs],
    sbtConfigChange: Observer[String],
    isDarkTheme: Signal[Boolean]
  ): HtmlElement =
    div(
      h2(span(I18n.t("build.sbt_config"))),
      pre(
        cls := "configuration",
        SimpleEditor(
          value = sbtInputs.map(_.sbtConfigExtra),
          onChange = sbtConfigChange,
          isDarkTheme = isDarkTheme,
          readOnly = false
        )
      )
    )

  /**
   * Render base SBT configuration panel (read-only).
   */
  private def baseSbtConfiguration(
    sbtInputs: Signal[SbtInputs],
    isDarkTheme: Signal[Boolean]
  ): HtmlElement =
    div(
      h2(span(I18n.t("build.sbt_base_config"))),
      pre(
        cls := "configuration",
        SimpleEditor(
          value = sbtInputs.map(_.sbtConfig),
          onChange = Observer.empty,
          isDarkTheme = isDarkTheme,
          readOnly = true
        )
      )
    )

  /**
   * Render base SBT plugins configuration panel (read-only).
   */
  private def baseSbtPluginsConfiguration(
    sbtInputs: Signal[SbtInputs],
    isDarkTheme: Signal[Boolean]
  ): HtmlElement =
    div(
      h2(span(I18n.t("build.sbt_plugins_config"))),
      pre(
        cls := "configuration",
        SimpleEditor(
          value = sbtInputs.map(_.sbtPluginsConfig),
          onChange = Observer.empty,
          isDarkTheme = isDarkTheme,
          readOnly = true
        )
      )
    )

  /**
   * Render SBT build settings panel.
   */
  private def sbtBuildSettingsPanel(
    sbtInputs: Signal[SbtInputs],
    setTarget: Observer[ScalaTarget],
    removeScalaDependency: Observer[ScalaDependency],
    updateDependencyVersion: Observer[(ScalaDependency, String)],
    addScalaDependency: Observer[(ScalaDependency, Project)],
    sbtConfigChange: Observer[String],
    isDarkTheme: Signal[Boolean],
    language: Signal[String]
  ): HtmlElement =
    div(
      h2(I18n.t("build.scala_version")),
      VersionSelector(
        scalaTarget = sbtInputs.map(_.target),
        onChange = setTarget
      ),
      h2(span(I18n.t("build.libraries"))),
      scaladexSearch(sbtInputs, removeScalaDependency, updateDependencyVersion, addScalaDependency, isDarkTheme, language),
      sbtExtraConfigurationPanel(sbtInputs, sbtConfigChange, isDarkTheme),
      baseSbtConfiguration(sbtInputs, isDarkTheme),
      baseSbtPluginsConfiguration(sbtInputs, isDarkTheme)
    )

  /**
   * Render Scala CLI build settings panel.
   */
  private def scalaCliBuildSettingsPanel(): HtmlElement =
    div(
      p(
        renderWithElement(
          I18n.t("build.scala_cli_version_doc"),
          content => a(
            href := "https://scala-cli.virtuslab.org/docs/reference/directives/#scala-version",
            target := "_blank",
            content
          )
        )
      ),
      p(
        renderWithElement(
          I18n.t("build.scala_cli_dependency_doc"),
          content => a(
            href := "https://scala-cli.virtuslab.org/docs/reference/directives#dependency",
            target := "_blank",
            content
          )
        )
      )
    )

  /**
   * Create a build settings element.
   *
   * @param visible Signal indicating if the panel is visible
   * @param inputs Signal with the current inputs
   * @param isDarkTheme Signal indicating dark theme state
   * @param isBuildDefault Signal indicating if build is default
   * @param isResetModalClosed Signal indicating if reset modal is closed
   * @param setTarget Observer for target changes
   * @param closeResetModal Observer to close reset modal
   * @param resetBuild Observer to reset build
   * @param openResetModal Observer to open reset modal
   * @param sbtConfigChange Observer for SBT config changes
   * @param removeScalaDependency Observer for removing dependencies
   * @param updateDependencyVersion Observer for updating dependency versions
   * @param addScalaDependency Observer for adding dependencies
   * @param language Signal with the current language
   * @return Build settings element
   */
  def apply(
    visible: Signal[Boolean],
    inputs: Signal[BaseInputs],
    isDarkTheme: Signal[Boolean],
    isBuildDefault: Signal[Boolean],
    isResetModalClosed: Signal[Boolean],
    setTarget: Observer[ScalaTarget],
    closeResetModal: Observer[Unit],
    resetBuild: Observer[Unit],
    openResetModal: Observer[Unit],
    sbtConfigChange: Observer[String],
    removeScalaDependency: Observer[ScalaDependency],
    updateDependencyVersion: Observer[(ScalaDependency, String)],
    addScalaDependency: Observer[(ScalaDependency, Project)],
    language: Signal[String] = Val("en")
  ): HtmlElement =
    div(
      cls := "build-settings-container",
      display <-- visible.map(v => if v then "block" else "none"),

      renderResetButton(
        inputs,
        isBuildDefault,
        isResetModalClosed,
        closeResetModal,
        resetBuild,
        openResetModal,
        isDarkTheme
      ),

      h2(span(I18n.t("build.target"))),
      TargetSelector(
        scalaTarget = inputs.map(_.target),
        onChange = setTarget
      ),

      // Target-specific settings
      child <-- inputs.map {
        case sbtInputs: SbtInputs =>
          sbtBuildSettingsPanel(
            Val(sbtInputs),
            setTarget,
            removeScalaDependency,
            updateDependencyVersion,
            addScalaDependency,
            sbtConfigChange,
            isDarkTheme,
            language
          )
        case _: ScalaCliInputs =>
          scalaCliBuildSettingsPanel()
        case _ =>
          div()
      }
    )

  /**
   * Static version with boolean and concrete values.
   */
  def apply(
    visible: Boolean,
    inputs: BaseInputs,
    isDarkTheme: Boolean,
    isBuildDefault: Boolean,
    isResetModalClosed: Boolean,
    setTarget: Observer[ScalaTarget],
    closeResetModal: Observer[Unit],
    resetBuild: Observer[Unit],
    openResetModal: Observer[Unit],
    sbtConfigChange: Observer[String],
    removeScalaDependency: Observer[ScalaDependency],
    updateDependencyVersion: Observer[(ScalaDependency, String)],
    addScalaDependency: Observer[(ScalaDependency, Project)],
    language: String
  ): HtmlElement =
    apply(
      Val(visible),
      Val(inputs),
      Val(isDarkTheme),
      Val(isBuildDefault),
      Val(isResetModalClosed),
      setTarget,
      closeResetModal,
      resetBuild,
      openResetModal,
      sbtConfigChange,
      removeScalaDependency,
      updateDependencyVersion,
      addScalaDependency,
      Val(language)
    )
