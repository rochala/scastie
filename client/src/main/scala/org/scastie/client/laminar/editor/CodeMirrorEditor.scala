package org.scastie.client.laminar.editor

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation.*

/**
 * CodeMirror 6 integration for Laminar
 *
 * Provides a reactive wrapper around CodeMirror 6 editor.
 */
object CodeMirrorEditor:

  /**
   * CodeMirror editor configuration.
   */
  case class EditorConfig(
    language: String = "scala",
    theme: String = "light",
    readOnly: Boolean = false,
    lineNumbers: Boolean = true,
    vim: Boolean = false,
    emacs: Boolean = false
  )

  /**
   * Create a CodeMirror editor element.
   *
   * @param code Signal containing the code to display
   * @param onCodeChange Observer to receive code changes
   * @param config Signal containing editor configuration
   * @param onReady Optional callback when editor is ready
   * @return Editor container element
   */
  def apply(
    code: Signal[String],
    onCodeChange: Observer[String],
    config: Signal[EditorConfig] = Val(EditorConfig()),
    onReady: Option[EditorView => Unit] = None
  ): HtmlElement =
    div(
      cls := "codemirror-container",

      // Initialize CodeMirror on mount
      onMountUnmountCallback(
        mount = ctx => {
          val editorView = createEditor(ctx.thisNode.ref, code.now(), onCodeChange, config.now())

          onReady.foreach(_(editorView))

          // Subscribe to code changes
          code.foreach { newCode =>
            if editorView.getCode != newCode then
              editorView.setCode(newCode)
          }(ctx.owner)

          // Subscribe to config changes
          config.foreach { newConfig =>
            editorView.updateConfig(newConfig)
          }(ctx.owner)

          // Cleanup
          ctx.onUnmount {
            editorView.destroy()
          }
        },
        unmount = _ => ()
      )
    )

  /**
   * Create CodeMirror editor instance.
   */
  private def createEditor(
    container: dom.Element,
    initialCode: String,
    onCodeChange: Observer[String],
    config: EditorConfig
  ): EditorView =
    // This is a facade - actual implementation will use ScalablyTyped bindings
    // or custom JS facades to CodeMirror 6
    new EditorView(container, initialCode, onCodeChange, config)

  /**
   * EditorView facade for CodeMirror 6.
   *
   * This is a placeholder - the actual implementation will use
   * ScalablyTyped-generated bindings or custom facades.
   */
  @js.native
  @JSGlobal
  class EditorView extends js.Object:
    def getCode: String = js.native
    def setCode(code: String): Unit = js.native
    def updateConfig(config: EditorConfig): Unit = js.native
    def destroy(): Unit = js.native

  // Companion object for EditorView with constructor
  object EditorView:
    def apply(
      container: dom.Element,
      initialCode: String,
      onCodeChange: Observer[String],
      config: EditorConfig
    ): EditorView =
      // This would use the actual CodeMirror 6 API
      // For now, this is a stub that shows the intended interface
      js.Dynamic.literal(
        getCode = (() => initialCode),
        setCode = ((code: String) => ()),
        updateConfig = ((cfg: EditorConfig) => ()),
        destroy = (() => ())
      ).asInstanceOf[EditorView]
