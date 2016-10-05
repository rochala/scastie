package client

import japgolly.scalajs.react._, vdom.all._, extra.router.RouterCtl

import api._
import autowire._
import scalajs.concurrent.JSExecutionContext.Implicits.queue

import org.scalajs.dom.{WebSocket, MessageEvent, Event, CloseEvent, ErrorEvent, window}
import scala.util.{Success, Failure}

import upickle.default.{read => uread}

object App {
  case class State(
    // ui
    sideBarClosed: Boolean = true,
    websocket: Option[WebSocket] = None,
    dark: Boolean = true,

    inputs: Inputs = Inputs(),
    outputs: Outputs = Outputs()
  ) {
    def toogleTheme             = copy(dark = !dark)
    def toogleSidebar           = copy(sideBarClosed = !sideBarClosed)
    def log(line: String): State       = log(Seq(line))
    def log(lines: Seq[String]): State = copy(outputs = outputs.copy(console = outputs.console ++ lines))

    def setCode(code: String) = copy(inputs = inputs.copy(code = code))
    def resetOutputs = copy(outputs = Outputs())
    def addOutputs(compilationInfos: List[api.Problem], instrumentations: List[api.Instrumentation]) =
      copy(outputs = outputs.copy(
        compilationInfos = outputs.compilationInfos ++ compilationInfos.toSet,
        instrumentations = outputs.instrumentations ++ instrumentations.toSet
      ))
  }

  class Backend(scope: BackendScope[(RouterCtl[Page], Option[Snippet]), State]) {
    def codeChange(newCode: String) = scope.modState(_.setCode(newCode))

    private def connect(id: Long) = CallbackTo[WebSocket]{
      val direct = scope.accessDirect

      def onopen(e: Event): Unit = direct.modState(_.log("Connected."))
      def onmessage(e: MessageEvent): Unit = {
        val progress = uread[PasteProgress](e.data.toString)
        direct.modState(_.addOutputs(progress.compilationInfos, progress.instrumentations).log(progress.output))
      }
      def onerror(e: ErrorEvent): Unit = direct.modState(_.log(s"Error: ${e.message}"))
      def onclose(e: CloseEvent): Unit = direct.modState(_.copy(websocket = None).log(s"Closed: ${e.reason}"))

      val protocol = if(window.location.protocol == "https:") "wss" else "ws"
      val uri = s"$protocol://${window.location.host}/progress/$id"
      val socket = new WebSocket(uri)

      socket.onopen = onopen _
      socket.onclose = onclose _
      socket.onmessage = onmessage _
      socket.onerror = onerror _
      socket
    }

    def clear(): Callback = scope.modState(_.resetOutputs)

    def run(): Callback = {
      scope.state.flatMap(s =>
        Callback.future(api.Client[Api].run(s.inputs.code).call().map(id =>
          connect(id).attemptTry.flatMap{
            case Success(ws) => {
              def clearLogs = 
                scope.modState(
                  _.resetOutputs
                   .copy(websocket = Some(ws))
                   .log("Connecting...")
                )

              def urlRewrite =
                scope.props.flatMap{ case (router, snippet) =>
                  router.set(Snippet(id))
                }

              clearLogs >> urlRewrite
            }
            case Failure(error) => scope.modState(_.resetOutputs.log(error.toString))
          }
        ))
      )
    }
    def runE(e: ReactEventI): Callback = run()
    def start(props: (RouterCtl[Page], Option[Snippet])): Callback = {
      val (router, snippet) = props

      snippet match {
        case Some(Snippet(id)) => Callback.future(api.Client[Api].fetch(id).call().map(codeChange))
        case None              => Callback(())
      }
    }

    def toogleTheme() = scope.modState(_.toogleTheme)
  }

  val SideBar = ReactComponentB[(State, Backend)]("SideBar")
    .render_P { case (state, backend) =>
      div(
        button(onClick ==> backend.runE)("run")
      )
    }
    .build

  val defaultCode =
    """|object Main {
       |  def main(args: Array[String]): Unit = {
       |    println(util.Properties.versionString)
       |  }
       |}""".stripMargin

  val component = ReactComponentB[(RouterCtl[Page], Option[Snippet])]("App")
    .initialState(State(inputs = Inputs(code = defaultCode)))
    .backend(new Backend(_))
    .renderPS{ case (scope, (router, snippet), state) => {
      val sideStyle =
        if(state.sideBarClosed) "sidebar-closed"
        else "sidebar-open"

      val hideOutput = 
        if(state.outputs.console.isEmpty) display.none
        else display.block

      div(`class` := "app")(
        div(`class` := s"editor $sideStyle")(
          Editor(state, scope.backend),
          ul(`class` := "output", hideOutput)(
            state.outputs.console.map(o => li(o))
          )
        ),
        div(`class` := s"sidebar $sideStyle")(SideBar((state, scope.backend)))
      )
    }}
    .componentDidMount(s => s.backend.start(s.props))
    .build

  def apply(router: RouterCtl[Page], snippet: Option[Snippet]) = component((router, snippet))
}
