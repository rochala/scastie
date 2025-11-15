package org.scastie.client.laminar.components

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import org.scastie.api.*
import org.scastie.client.{View, HTMLFormatter}
import org.scastie.client.i18n.I18n

/**
 * Console output component - Laminar version
 *
 * Migrated from: org.scastie.client.components.Console
 */
object ConsoleComponent:

  /**
   * Format console outputs to HTML string.
   */
  private def renderConsoleOutputs(outputs: Vector[ConsoleOutput]): String =
    val (users, systems) = outputs.partition {
      case _: UserOutput => true
      case _ => false
    }

    val toShow =
      if users.isEmpty then outputs
      else users

    s"<pre>${HTMLFormatter.format(toShow.map(_.show).mkString("\n"))}</pre>"

  /**
   * Create a console component.
   *
   * @param isOpen Signal indicating if console is open
   * @param isRunning Signal indicating if code is running
   * @param isEmbedded Whether this is embedded mode
   * @param consoleOutputs Signal containing console outputs
   * @param onRun Observer to run code
   * @param setView Observer to change view
   * @param onClose Observer to close console
   * @param onOpen Observer to open console
   * @param language Language code for i18n
   * @return Console component element
   */
  def apply(
    isOpen: Signal[Boolean],
    isRunning: Signal[Boolean],
    isEmbedded: Boolean,
    consoleOutputs: Signal[Vector[ConsoleOutput]],
    onRun: Observer[Unit],
    setView: Observer[View],
    onClose: Observer[Unit],
    onOpen: Observer[Unit],
    language: String = "en"
  ): HtmlElement =
    val displayConsoleSignal = isOpen.map(open => if open then "block" else "none")
    val displaySwitcherSignal = isOpen.map(open => if open then "none" else "flex")
    val consoleCssSignal = isOpen.map(open => if open then "console-open" else "")

    val consoleHtmlSignal = consoleOutputs.map(renderConsoleOutputs)

    div(
      cls := "console-container",
      cls <-- consoleCssSignal,

      // Main console (visible when open)
      div(
        cls := "console",
        display <-- displayConsoleSignal,

        div(cls := "handler"),

        div(
          cls := "switcher-hide",
          display := "flex",
          role := "button",
          onClick.mapTo(()) --> onClose,

          // Run button (only in embedded mode)
          child <-- Signal.combine(isRunning, Val(isEmbedded)).map {
            case (running, embedded) if embedded =>
              RunButton(
                isRunning = Val(running),
                isStatusOk = Val(true),
                onSave = onRun,
                setView = setView,
                embedded = true
              )
            case _ =>
              emptyNode
          },

          div(
            cls := "console-label",
            i(cls := "fa fa-terminal"),
            p(s"${I18n.t("console.title")} (F3)"),
            i(cls := "fa fa-caret-down")
          )
        ),

        div(
          cls := "output-console",
          inContext { thisNode =>
            consoleHtmlSignal --> Observer[String] { html =>
              thisNode.ref.innerHTML = html
            }
          }
        )
      ),

      // Switcher to show console (visible when closed)
      div(
        cls := "switcher-show",
        role := "button",
        onClick.mapTo(()) --> onOpen,
        display <-- displaySwitcherSignal,

        // Run button (only in embedded mode)
        child <-- Signal.combine(isRunning, Val(isEmbedded)).map {
          case (running, embedded) if embedded =>
            RunButton(
              isRunning = Val(running),
              isStatusOk = Val(true),
              onSave = onRun,
              setView = setView,
              embedded = true
            )
          case _ =>
            emptyNode
        },

        div(
          cls := "console-label",
          i(cls := "fa fa-terminal"),
          p(s"${I18n.t("console.title")} (F3)"),
          i(cls := "fa fa-caret-up")
        )
      )
    )

  /**
   * Simplified version with static open/running states.
   */
  def apply(
    isOpen: Boolean,
    isRunning: Boolean,
    consoleOutputs: Signal[Vector[ConsoleOutput]],
    onRun: Observer[Unit],
    onToggle: Observer[Unit]
  ): HtmlElement =
    apply(
      isOpen = Val(isOpen),
      isRunning = Val(isRunning),
      isEmbedded = false,
      consoleOutputs = consoleOutputs,
      onRun = onRun,
      setView = Observer.empty,
      onClose = onToggle,
      onOpen = onToggle,
      language = "en"
    )
