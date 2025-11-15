package org.scastie.client.laminar.components

import com.raquo.laminar.api.L.*
import org.scalajs.dom

/**
 * Simple Laminar button component - proof of concept migration from React.
 *
 * This demonstrates the migration pattern from scalajs-react to Laminar.
 *
 * React version (old):
 * {{{
 * object RunButton {
 *   case class Props(onClick: Callback, disabled: Boolean)
 *
 *   private val component = ScalaComponent
 *     .builder[Props]("RunButton")
 *     .render_P { props =>
 *       button(
 *         cls := "run-button",
 *         onClick --> props.onClick,
 *         disabled := props.disabled,
 *         "Run"
 *       )
 *     }
 *     .build
 *
 *   def apply(props: Props) = component(props)
 * }
 * }}}
 */
object RunButton:

  /**
   * Create a run button element.
   *
   * @param onClick Observer to handle click events
   * @param disabled Signal indicating whether button is disabled
   * @param text Optional button text (defaults to "Run")
   * @return Button element
   */
  def apply(
    onClick: Observer[Unit],
    disabled: Signal[Boolean],
    text: String = "Run"
  ): HtmlElement =
    button(
      cls := "run-button btn-run",
      onClick.mapTo(()) --> onClick,
      disabled <-- disabled,
      text
    )

  /**
   * Alternative: Create with static disabled state
   */
  def apply(
    onClick: Observer[Unit],
    disabled: Boolean,
    text: String
  ): HtmlElement =
    button(
      cls := "run-button btn-run",
      onClick.mapTo(()) --> onClick,
      disabled := disabled,
      text
    )
