package org.scastie.client.laminar.components

import com.raquo.laminar.api.L.*
import org.scastie.api.*
import org.scastie.client.i18n.I18n
import org.scastie.buildinfo.BuildInfo

/**
 * Version selector component - Laminar version
 *
 * Migrated from: org.scastie.client.components.VersionSelector
 */
object VersionSelector:

  private def versionSelectors(scalaTarget: SbtScalaTarget, scalaVersion: String): ScalaTarget =
    scalaTarget match
      case _: Scala2    => Scala2(scalaVersion)
      case _: Typelevel => Typelevel(scalaVersion)
      case _: Scala3    => Scala3(scalaVersion)
      case js: Js       => Js(scalaVersion, js.scalaJsVersion)
      case n: Native    => Native(n.scalaNativeVersion, scalaVersion)

  private def renderRecommended3Versions(scalaVersion: String): String =
    if scalaVersion == BuildInfo.stableLTS then s"$scalaVersion LTS"
    else if scalaVersion == BuildInfo.stableNext then s"$scalaVersion Next"
    else scalaVersion

  /**
   * Create a version selector element.
   *
   * @param scalaTarget Signal with the current SBT scala target
   * @param onChange Observer for target changes
   * @return Version selector element
   */
  def apply(
    scalaTarget: Signal[SbtScalaTarget],
    onChange: Observer[ScalaTarget]
  ): HtmlElement =
    ul(
      cls := "suggestedVersions",

      // Render suggested versions as radio buttons
      children <-- scalaTarget.map { target =>
        ScalaVersions.suggestedScalaVersions(target.targetType).map { suggestedVersion =>
          li(
            input(
              typ := "radio",
              idAttr := s"scala-$suggestedVersion",
              value := suggestedVersion,
              nameAttr := "scalaV",
              onChange.mapTo(versionSelectors(target, suggestedVersion)) --> onChange,
              checked := target.scalaVersion == suggestedVersion
            ),
            label(
              forId := s"scala-$suggestedVersion",
              cls := "radio",
              role := "button",
              renderRecommended3Versions(suggestedVersion)
            )
          )
        }
      },

      // Render dropdown for "Other" versions
      li(
        label(
          div(cls := "select-wrapper",
            child <-- scalaTarget.map { target =>
              val isRecommended = ScalaVersions.suggestedScalaVersions(target.targetType).contains(target.scalaVersion)

              select(
                nameAttr := "scalaVersion",
                onInput.mapToValue.map(version => versionSelectors(target, version)) --> onChange,
                value := (if isRecommended then I18n.t("build.other") else target.scalaVersion),
                cls := (if !isRecommended then "selected-option" else ""),

                // Hidden disabled option for "Other"
                option(
                  I18n.t("build.other"),
                  hidden := true,
                  disabled := true
                ),

                // All available versions
                ScalaVersions.allVersions(target.targetType).map { version =>
                  option(value := version, version)
                }
              )
            }
          )
        )
      )
    )

  /**
   * Static version with SbtScalaTarget value.
   */
  def apply(
    scalaTarget: SbtScalaTarget,
    onChange: Observer[ScalaTarget]
  ): HtmlElement =
    apply(Val(scalaTarget), onChange)
