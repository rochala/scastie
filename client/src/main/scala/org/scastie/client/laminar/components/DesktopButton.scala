package org.scastie.client.laminar.components

import com.raquo.laminar.api.L.*

/**
 * Desktop button component - Laminar version
 *
 * Migrated from: org.scastie.client.components.DesktopButton
 */
object DesktopButton:

  /**
   * Create a desktop mode toggle button.
   *
   * @param onForceDesktop Observer to handle desktop mode toggle
   * @return Button element as list item
   */
  def apply(
    onForceDesktop: Observer[Unit]
  ): HtmlElement =
    li(
      title := "Go to desktop",
      cls := "btn",
      onClick.mapTo(()) --> onForceDesktop,
      i(cls := "fa fa-desktop"),
      span("Desktop")
    )
