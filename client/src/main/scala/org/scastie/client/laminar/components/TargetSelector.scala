package org.scastie.client.laminar.components

import com.raquo.laminar.api.L.*
import org.scastie.api.*

/**
 * Target selector component - Laminar version
 *
 * Migrated from: org.scastie.client.components.TargetSelector
 */
object TargetSelector:

  private val targetTypes = List[ScalaTargetType](
    ScalaTargetType.ScalaCli,
    ScalaTargetType.Scala3,
    ScalaTargetType.Scala2,
    ScalaTargetType.JS
    // ScalaTargetType.Native
  )

  private def labelFor(targetType: ScalaTargetType): String =
    targetType match
      case ScalaTargetType.ScalaCli  => "Scala-CLI"
      case ScalaTargetType.Scala2    => "Scala 2"
      case ScalaTargetType.JS        => "Scala.js"
      case ScalaTargetType.Scala3    => "Scala 3"
      case ScalaTargetType.Native    => "Native"
      case ScalaTargetType.Typelevel => "Typelevel"

  /**
   * Create a target selector element.
   *
   * @param scalaTarget Signal with the current target
   * @param onChange Observer for target changes
   * @return Target selector element
   */
  def apply(
    scalaTarget: Signal[ScalaTarget],
    onChange: Observer[ScalaTarget]
  ): HtmlElement =
    div(
      ul(cls := "target",
        targetTypes.map { targetType =>
          val targetLabel = labelFor(targetType)
          li(
            input(
              typ := "radio",
              idAttr := targetLabel,
              value := targetLabel,
              nameAttr := "target",
              onChange.mapTo(targetType.defaultScalaTarget) --> onChange,
              checked <-- scalaTarget.map(_.targetType == targetType)
            ),
            label(
              forId := targetLabel,
              role := "button",
              cls := "radio",
              targetLabel
            )
          )
        }
      )
    )

  /**
   * Static version with ScalaTarget value.
   */
  def apply(
    scalaTarget: ScalaTarget,
    onChange: Observer[ScalaTarget]
  ): HtmlElement =
    apply(Val(scalaTarget), onChange)
