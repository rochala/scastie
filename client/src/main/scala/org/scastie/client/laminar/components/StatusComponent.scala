package org.scastie.client.laminar.components

import com.raquo.laminar.api.L.*
import org.scastie.api.*
import org.scastie.client.{StatusState, Page}
import org.scastie.client.i18n.I18n
import com.raquo.waypoint.Router

/**
 * Status component showing SBT runners state - Laminar version
 *
 * Migrated from: org.scastie.client.components.Status
 */
object StatusComponent:

  /**
   * Render an SBT task list.
   */
  private def renderSbtTask(tasks: Vector[TaskId], isAdmin: Boolean, router: Option[Router[Page]]): HtmlElement =
    if !isAdmin then
      div()
    else if tasks.isEmpty then
      div(I18n.t("status.no_task"))
    else
      ul(
        tasks.zipWithIndex.map { case (TaskId(snippetId), j) =>
          li(
            key := snippetId.toString,
            // TODO: Add router link when router is implemented
            s"${I18n.t("status.task")} $j - ${snippetId.toString}"
          )
        }
      )

  /**
   * Render configuration status.
   */
  private def renderConfiguration(serverInputs: SbtInputs, inputs: BaseInputs): HtmlElement =
    val (cssConfig, label) = inputs match
      case sbtInputs: SbtInputs if serverInputs.needsReload(sbtInputs) =>
        ("needs-reload", I18n.t("status.different_config"))
      case _: ScalaCliInputs =>
        ("different-target", I18n.t("status.sbt_runner_config"))
      case _ =>
        ("ready", I18n.t("status.same_config"))

    span(cls := s"runner $cssConfig")(label)

  /**
   * Create a status display component.
   *
   * @param state Signal containing the status state
   * @param isAdmin Signal indicating if user is admin
   * @param inputs Signal containing current inputs
   * @param router Optional router for task links
   * @param language Language code for i18n
   * @return Status display element
   */
  def apply(
    state: Signal[StatusState],
    isAdmin: Signal[Boolean],
    inputs: Signal[BaseInputs],
    router: Option[Router[Page]] = None,
    language: String = "en"
  ): HtmlElement =
    div(
      child <-- Signal.combine(state, isAdmin, inputs).map { case (statusState, admin, currentInputs) =>
        statusState.sbtRunners match
          case Some(sbtRunners) =>
            div(
              h1(I18n.t("status.sbt_runners")),
              ul(
                sbtRunners.zipWithIndex.map { case (sbtRunner, i) =>
                  li(
                    key := i.toString,
                    renderConfiguration(sbtRunner.config, currentInputs),
                    renderSbtTask(sbtRunner.tasks, admin, router)
                  )
                }
              )
            )
          case None =>
            div()
      }
    )

  /**
   * Simplified version with static isAdmin.
   */
  def apply(
    state: Signal[StatusState],
    isAdmin: Boolean,
    inputs: Signal[BaseInputs]
  ): HtmlElement =
    apply(state, Val(isAdmin), inputs, None, "en")
