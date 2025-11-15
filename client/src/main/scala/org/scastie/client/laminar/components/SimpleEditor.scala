package org.scastie.client.laminar.components

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import typings.codemirrorLanguage.mod
import typings.codemirrorState.mod.*
import typings.codemirrorView.mod.*
import org.scastie.client.components.editor.{Editor, OnChangeHandler, SyntaxHighlightingTheme}
import scala.scalajs.js

/**
 * Simple editor component for configuration files - Laminar version
 *
 * Migrated from: org.scastie.client.components.editor.SimpleEditor
 */
object SimpleEditor:

  /**
   * Create a simple editor element.
   *
   * @param value Signal with the editor content
   * @param onChange Observer for content changes
   * @param isDarkTheme Signal indicating dark theme state
   * @param readOnly Whether the editor is read-only
   * @return Simple editor element
   */
  def apply(
    value: Signal[String],
    onChange: Observer[String],
    isDarkTheme: Signal[Boolean],
    readOnly: Boolean = false
  ): HtmlElement =
    val editorViewVar = Var[Option[EditorView]](None)

    div(
      cls := "simple-editor",

      onMountUnmountCallback(
        mount = ctx => {
          val divElement = ctx.thisNode.ref

          // Create CodeMirror extensions
          val basicExtensions = js.Array[Any](
            Editor.editorTheme.of(if isDarkTheme.now() then Editor.darkTheme else Editor.lightTheme),
            Editor.indentationMarkersExtension,
            typings.codemirror.mod.minimalSetup,
            mod.StreamLanguage.define(typings.codemirrorLegacyModes.modeClikeMod.scala_),
            SyntaxHighlightingTheme.highlightingTheme
          )

          val conditionalExtensions = if readOnly then
            js.Array[Any](EditorState.readOnly.of(true))
          else
            js.Array[Any](
              lineNumbers(),
              OnChangeHandler(code => onChange.onNext(code))
            )

          val editorStateConfig = EditorStateConfig()
            .setDoc(value.now())
            .setExtensions(conditionalExtensions ++ basicExtensions)

          val editor = new EditorView(
            EditorViewConfig()
              .setState(EditorState.create(editorStateConfig))
              .setParent(divElement)
          )

          editorViewVar.set(Some(editor))

          // Subscribe to value changes
          value.foreach { newValue =>
            editorViewVar.now().foreach { view =>
              val currentValue = view.state.doc.toString()
              if currentValue != newValue then
                val transaction = view.state.update(
                  TransactionSpec()
                    .setChanges(ChangeSpec().setFrom(0).setTo(view.state.doc.length).setInsert(newValue))
                )
                view.dispatch(transaction)
            }
          }(ctx.owner)

          // Subscribe to theme changes
          isDarkTheme.foreach { dark =>
            editorViewVar.now().foreach { view =>
              val theme = if dark then Editor.darkTheme else Editor.lightTheme
              val compartment = Editor.editorTheme
              view.dispatch(
                TransactionSpec().setEffects(compartment.reconfigure(theme))
              )
            }
          }(ctx.owner)
        },
        unmount = ctx => {
          editorViewVar.now().foreach(_.destroy())
          editorViewVar.set(None)
        }
      )
    )

  /**
   * Static version with boolean and string values.
   */
  def apply(
    value: String,
    onChange: Observer[String],
    isDarkTheme: Boolean,
    readOnly: Boolean
  ): HtmlElement =
    apply(Val(value), onChange, Val(isDarkTheme), readOnly)
