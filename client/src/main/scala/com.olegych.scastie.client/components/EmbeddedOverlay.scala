package com.olegych.scastie.client.components

import scala.concurrent.Future

import com.olegych.scastie.api._
import com.olegych.scastie.client.View
import japgolly.scalajs.react._
import org.scalajs.dom
import scalajs.concurrent.JSExecutionContext.Implicits.queue
import vdom.all._

final case class EmbeddedOverlay(
  inputsHasChanged: Boolean,
  embeddedSnippetId: Option[SnippetId],
  serverUrl: Option[String],
  save: Reusable[AsyncCallback[Option[SnippetId]]]
) {
  @inline def render: VdomElement = EmbeddedOverlay.component(this)
}

object EmbeddedOverlay {

  implicit val reusability: Reusability[EmbeddedOverlay] = Reusability.derive[EmbeddedOverlay]

  private def render(props: EmbeddedOverlay): VdomElement = {

    val urlBase = props.serverUrl.getOrElse("")

    def openScastie: AsyncCallback[Unit] = {
      def open(snippetId: SnippetId): Callback =
        Callback(dom.window.open(urlBase + "/" + snippetId.url, "_blank").focus())

      props.embeddedSnippetId match {
        case Some(snippetId) if !props.inputsHasChanged => open(snippetId).asAsyncCallback
        case _ => props.save.value.flatMapSync {
            case Some(value) => open(value)
            case None        => Callback.empty
          }
      }
    }

    ul(cls := "embedded-overlay")(
      li(cls  := "logo")(
        i(cls := "fa fa-border fa-external-link"),
        onClick --> openScastie
      )
    )
  }

  private val component = ScalaFnComponent
    .withHooks[EmbeddedOverlay]
    .renderWithReuse(render)

}
