package org.scastie.client.components

import org.scastie.api._
import org.scastie.buildinfo.BuildInfo
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.builder.Lifecycle.RenderScope
import org.scalajs.dom

import scala.concurrent.Future

import vdom.all._
import dom.ext.KeyCode
import dom.{HTMLInputElement, HTMLElement}
import scalajs.js.Thenable.Implicits._
import scalajs.concurrent.JSExecutionContext.Implicits.queue

import io.circe._
import io.circe.syntax._
import io.circe.parser._

final case class ScaladexSearch(
    removeScalaDependency: ScalaDependency ~=> Callback,
    updateDependencyVersion: (ScalaDependency, String) ~=> Callback,
    addScalaDependency: (ScalaDependency, Project) ~=> Callback,
    libraries: Set[ScalaDependency],
    scalaTarget: SbtScalaTarget
) {
  @inline def render: VdomElement = ScaladexSearch.component(this)
}

object ScaladexSearch {

  implicit val propsReusability: Reusability[ScaladexSearch] =
    Reusability.derive[ScaladexSearch]

  implicit val selectedReusability: Reusability[Selected] =
    Reusability.derive[Selected]

  implicit val stateReusability: Reusability[SearchState] =
    Reusability.derive[SearchState]

  private def toQuery(in: Map[String, String]): String =
    in.map { case (k, v) => s"$k=$v" }.mkString("?", "&", "")

  def queryAndParse(t: SbtScalaTarget, query: String): Future[List[(Project, ScalaTarget)]] = {
    val q = toQuery(t.scaladexRequest + ("q" -> query))
    for {
      response <- dom.fetch(scaladexApiUrl + "/search" + q)
      text <- response.text()
    } yield {
      decode[List[Project]](text).getOrElse(Nil).map(_ -> t)
    }
  }

  def fetchSelected(project: Project, artifact: String, target: SbtScalaTarget, version: Option[String]) = {
    val query = toQuery(
      Map(
        "organization" -> project.organization,
        "repository" -> project.repository
      ) ++ target.scaladexRequest
    )

    for {
      response <- dom.fetch(scaladexApiUrl + "/project" + query)
      text <- response.text()
    } yield {
      decode[ReleaseOptions](text).toOption.map { options =>
        {
          Selected(
            project = project,
            release = ScalaDependency(
              groupId = options.groupId,
              artifact = artifact,
              target = target,
              version = version.getOrElse(options.version),
            ),
            options = options,
          )
        }
      }
    }
  }

  def addArtifact(projectAndArtifact: (Project, String, Option[String]), target: ScalaTarget, state: hooks.Hooks.UseStateF[CallbackTo, SearchState], props: ScaladexSearch): Callback = {
    val (project, artifact, version) = projectAndArtifact
    if (state.value.selecteds.exists(_.matches(project, artifact))) Callback(())
    else
      Callback.future {
        target match {
          case sbtScalaTarget: SbtScalaTarget =>
            fetchSelected(project, artifact, sbtScalaTarget, version).map {
              case Some(selected) if !state.value.selecteds.exists(_.release.matches(selected.release)) =>
                state.modState(_.addSelected(selected)) >> props.addScalaDependency(selected.release -> selected.project)
              case _ => Callback(())
            }
          case _ => Future.successful(Callback(()))
        }
      }
  }

  private[ScaladexSearch] object SearchState {
    def default: SearchState = {
      SearchState(
        query = "",
        selectedIndex = 0,
        projects = List.empty,
        selecteds = List.empty,
      )
    }
  }

  private[ScaladexSearch] case class Selected(
      project: Project,
      release: ScalaDependency,
      options: ReleaseOptions
  ) {
    def matches(p: Project, artifact: String) = p == project && release.artifact == artifact
  }

  private[ScaladexSearch] case class SearchState(
      query: String,
      selectedIndex: Int,
      projects: List[(Project, ScalaTarget)],
      selecteds: List[Selected],
  ) {

    private val selectedProjectsArtifacts = selecteds
      .map(selected => (selected.project, selected.release.artifact, None, selected.release.target))
      .toSet

    val search: List[(Project, String, Option[String], ScalaTarget)] =
      projects
        .flatMap {
          case (project, target) => project.artifacts.map(artifact => (project, artifact, None, target))
        }
        .filter { projectAndArtifact =>
          !selectedProjectsArtifacts.contains(projectAndArtifact)
        }

    def removeSelected(selected: Selected): SearchState = {
      copy(selecteds = selecteds.filterNot(_.release.matches(selected.release)))
    }

    def addSelected(selected: Selected): SearchState = {
      copy(
        selecteds = selected :: selecteds.filterNot(_.release.matches(selected.release))
      )
    }

    def updateVersion(selected: Selected, version: String): SearchState = {
      val updated = selected.copy(release = selected.release.copy(version = version), options = selected.options.copy(version = version))
      copy(
        selecteds = selecteds.filterNot(_.release.matches(updated.release)) :+ updated
      )
    }

    def setProjects(projects: List[(Project, ScalaTarget)]): SearchState = {
      copy(projects = projects)
    }

    def clearProjects: SearchState = {
      copy(projects = List())
    }
  }

  // private val scaladexBaseUrl = "http://localhost:8080"
  private val scaladexBaseUrl = "https://index.scala-lang.org"
  private val scaladexApiUrl = scaladexBaseUrl + "/api"

  private implicit val projectOrdering: Ordering[Project] =
    Ordering.by { project: Project =>
      (project.organization, project.repository)
    }

  private implicit val scalaDependenciesOrdering: Ordering[ScalaDependency] =
    Ordering.by { scalaDependency: ScalaDependency =>
      scalaDependency.artifact
    }

  private implicit val selectedOrdering: Ordering[Selected] =
    Ordering.by { selected: Selected =>
      (selected.project, selected.release)
    }

  private val projectListRef = Ref[HTMLElement]
  private val searchInputRef = Ref[HTMLInputElement]


  private def render(props: ScaladexSearch, state: hooks.Hooks.UseStateF[CallbackTo, SearchState]): VdomElement = {
    def keyDown(e: ReactKeyboardEventFromInput): Callback = {

      if (e.keyCode == KeyCode.Down || e.keyCode == KeyCode.Up) {
        val diff =
          if (e.keyCode == KeyCode.Down) +1
          else -1

        def clamp(max: Int, v: Int) =
          if (v >= max) max - 1
          else if (v < 0) 0
          else v

        def interpolate(b: Int, d: Int, x: Int): Double = {
          b.toDouble / d.toDouble * x.toDouble
        }

        def scrollToSelected(selected: Int, total: Int) = {
          projectListRef.unsafeGet().scrollTop = Math.abs(
            interpolate(projectListRef.unsafeGet().scrollHeight, total, selected + diff)
          )
        }

        def selectProject =
          state.modState(
            s =>
              s.copy(
                selectedIndex = clamp(s.search.size, s.selectedIndex + diff)
            )
          )

        def scrollToSelectedProject = Callback {
          scrollToSelected(state.value.selectedIndex, state.value.search.size)
        }

        selectProject >>
          e.preventDefaultCB >>
          scrollToSelectedProject

      } else if (e.keyCode == KeyCode.Enter) {

        def addArtifactIfInRange =
          for {
            _ <- if (0 <= state.value.selectedIndex && state.value.selectedIndex < state.value.search.size) {
              val (p, a, v, t) = state.value.search(state.value.selectedIndex)
              addArtifact((p, a, v), t, state, props)
            } else Callback.empty
          } yield ()

        addArtifactIfInRange >> Callback(searchInputRef.unsafeGet().focus())
      } else {
        Callback.empty
      }
    }

    def removeSelected(selected: Selected): Callback = {

      def removeDependencyLocal = state.modState(_.removeSelected(selected))
      def removeDependecyBackend = props.removeScalaDependency(selected.release)

      removeDependecyBackend >> removeDependencyLocal
    }

    def updateVersion(selected: Selected)(e: ReactEventFromInput): Callback = {
      val version = e.target.value
      def updateDependencyVersionLocal = state.modState(_.updateVersion(selected, version))
      def updateDependencyVersionBackend = props.updateDependencyVersion(selected.release, version)
      updateDependencyVersionBackend >> updateDependencyVersionLocal
    }

    def selectIndex(index: Int): Callback =
      state.modState(s => s.copy(selectedIndex = index))

    def resetQuery: Callback =
      state.modState(s => s.copy(query = "", projects = Nil))

    def setQuery(e: ReactEventFromInput): Callback = {
      state.modState(_.copy(query = e.target.value)) >> fetchProjects()
    }

    def fetchProjects(): Callback = {
      def fetch(target: SbtScalaTarget, searchState: SearchState): Callback = {
        if (!searchState.query.isEmpty) {

          val projsForThisTarget = queryAndParse(target, searchState.query)
          val projects: Future[List[(Project, ScalaTarget)]] = target match {
            // If scala3 but no scala 3 versions available, offer 2.13 artifacts
            case Scala3(_) =>
              projsForThisTarget.flatMap { ls =>
                queryAndParse(Jvm(BuildInfo.latest213), searchState.query)
                  .map(arts213 => ls ::: arts213)
              }
            case _ => projsForThisTarget
          }

          Callback.future(
            projects.map(projects => state.modState(_.setProjects(projects)))
          )
        } else state.modState(_.clearProjects)
      }

      fetch(props.scalaTarget, state.value)
    }

    def selectedIndex(index: Int, selected: Int) =
      (cls := "selected").when(index == selected)

    def renderProject(project: Project,
                      artifact: String,
                      scalaTarget: ScalaTarget,
                      selected: TagMod,
                      handlers: TagMod = EmptyVdom,
                      remove: TagMod = EmptyVdom,
                      options: TagMod = EmptyVdom) = {
      import project._

      val common = TagMod(title := organization, cls := "logo")
      val artifact2 =
        artifact
          .replace(project.repository + "-", "")
          .replace(project.repository, "")

      val label =
        if (project.repository != artifact)
          s"${project.repository} / $artifact2"
        else artifact

      val scaladexLink =
        s"https://scaladex.scala-lang.org/$organization/$repository/$artifact"

      div(cls := "result", selected, handlers)(
        a(cls := "scaladexresult", href := scaladexLink, target := "_blank")(
          i(cls := "fa fa-external-link")
        ),
        remove,
        span(
          logo
            .map(url => img(src := (url + "&s=40"), common, alt := s"$organization logo or avatar"))
            .getOrElse(
              img(src := Assets.placeholder, common, alt := s"placeholder logo for $organization")
            ),
          span(cls := "artifact")(label),
          options,
          if (scalaTarget.binaryScalaVersion != props.scalaTarget.binaryScalaVersion)
            span(cls := "artifact")(s"(Scala ${scalaTarget.binaryScalaVersion} artifacts)")
          else ""
        ),
      )
    }

    def renderOptions(selected: Selected) = {
      div(cls := "select-wrapper")(
        select(
          selected.options.versions.reverse.map(v => option(value := v)(v)).toTagMod,
          value := selected.release.version,
          onChange ==> updateVersion(selected),
        )
      )
    }

    val added = {
      val hideAdded =
        if (state.value.selecteds.isEmpty) display.none
        else EmptyVdom

      div(cls := "added", hideAdded)(
        state.value.selecteds.sorted.map { selected =>
          renderProject(
            selected.project,
            selected.release.artifact,
            selected.release.target,
            i(cls := "fa fa-close")(
              onClick --> removeSelected(selected),
              cls := "remove"
            ),
            options = renderOptions(selected)
          )
        }.toTagMod
      )
    }

    val displayResults =
      if (state.value.search.isEmpty) display.none
      else display.block

    val displayClose =
      if (state.value.search.isEmpty) display.none
      else display.inlineBlock

    div(cls := "search", cls := "library")(
      added,
      div(cls := "search-input")(
        input.search.withRef(searchInputRef)(
          cls := "search-query",
          placeholder := "Search for 'cats'",
          value := state.value.query,
          onChange ==> setQuery,
          onKeyDown ==> keyDown
        ),
        div(cls := "close", displayClose)(
          i(cls := "fa fa-close")(
            onClick --> resetQuery
          )
        )
      ),
      div.withRef(projectListRef)(cls := "results", displayResults)(
        state.value.search.zipWithIndex.map {
          case ((project, artifact, version, target), index) =>
            renderProject(
              project,
              artifact,
              target,
              selected = selectedIndex(index, state.value.selectedIndex),
              handlers = TagMod(
                onClick --> addArtifact((project, artifact, version), target, state, props),
                onMouseOver --> selectIndex(index)
              )
            )
        }.toTagMod
      )
    )
  }

  def updateState(props: ScaladexSearch, state: hooks.Hooks.UseStateF[CallbackTo, SearchState]): Callback = {
    val target = props.scalaTarget
    val getProject: ScalaDependency => Future[Option[Selected]] = dependency => {
      ScaladexSearch.queryAndParse(target, dependency.artifact).flatMap { results =>
        val possibleMatches = results.filter {
          case (project, scalaTarget) => project.artifacts.contains(dependency.artifact)
        }

        val possibleProjects = Future.sequence {
          possibleMatches.map { case (project, scalaTarget) =>
            ScaladexSearch.fetchSelected(
              project,
              dependency.artifact,
              target,
              Some(dependency.version)
            ).map(_.toList)
          }
        }

        possibleProjects.map(_.flatten.find(_.release.groupId == dependency.groupId))
          .recover(_ => None)
      }
    }

    val (scastieRuntime, rest) = props.libraries.partition(_.groupId == "org.scastie")
    val librariesFromList = Future.sequence(rest.toList.map(getProject)).map(_.flatten)

    Callback.future { librariesFromList.map { libraries =>
      Callback.sequence {
        val failedLibraries = rest.diff(libraries.map(_.release).toSet)
        val removalTask = failedLibraries.map(failed => props.removeScalaDependency(failed)) // + display some kind of popup
        val addTask = libraries
          .filterNot(library => failedLibraries.contains(library.release))
          .map(selected => state.modState(_.addSelected(selected)))
        removalTask.toList ++ addTask
      }
    }}
  }

  private val component =
    ScalaFnComponent
      .withHooks[ScaladexSearch]
      .useState(SearchState.default)
      .useEffectOnMountBy((props, state) => updateState(props, state))
      .renderWithReuse((props, state) => render(props, state))

      // .useLayoutEffectOnMountBy((props, ref, prevProps, editorView) => init(props, ref.value, editorView))
      // .useEffectBy(
      //   (props, ref, prevProps, editorView) => updateComponent(props, ref.value, prevProps.value, editorView) >> prevProps.set(Some(props))
      // )useEffectBy
      // .render((_, ref, _, _) => Editor.render(ref.value))(())
      // .
      // .backend(new ScaladexSearchBackend(_))
      // .renderPS(render)
      // .componentDidMount(
      //     props.librariesFrom.toList.sortBy(_._1.artifact).map { lib =>
      //       scope.backend.addArtifact((lib._2, lib._1.artifact, Some(lib._1.version)), lib._1.target, scope.state, localOnly = true)
      //     }
      //   )
      // .componentWillReceiveProps { x =>
      //   println("THIS IS SUPER IMPORTANTES")
      //   println(x)
      //   Callback.traverse(x.nextProps.librariesFrom.toList.sortBy(_._1.artifact)) { lib =>
      //     x.backend.addArtifact((lib._2, lib._1.artifact, Some(lib._1.version)), lib._1.target, x.state, localOnly = true)
      //   }
      // }
      // // .configure(Reusability.shouldComponentUpdate)
      // .build
}