package org.scastie.client.laminar.components

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import org.scastie.api.*
import org.scastie.client.i18n.I18n
import org.scastie.buildinfo.BuildInfo
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import io.circe.*
import io.circe.syntax.*
import io.circe.parser.*
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@js.native
@JSImport("@resources/images/placeholder.png", JSImport.Default)
private object Placeholder extends js.Any

private object Assets:
  def placeholder: String = Placeholder.asInstanceOf[String]

/**
 * Scaladex search component - Laminar version
 *
 * Migrated from: org.scastie.client.components.ScaladexSearch
 */
object ScaladexSearch:

  // State management
  private case class Selected(
    project: Project,
    release: ScalaDependency,
    options: ReleaseOptions
  ):
    def matches(p: Project, artifact: String): Boolean =
      p == project && release.artifact == artifact

  private case class SearchState(
    query: String,
    selectedIndex: Int,
    projects: List[(Project, ScalaTarget)],
    selecteds: List[Selected]
  ):
    private val selectedProjectsArtifacts = selecteds
      .map(selected => (selected.project, selected.release.artifact, None, selected.release.target))
      .toSet

    private def matchScore(query: String, artifact: String, project: Project): Int =
      val queryLower = query.toLowerCase
      val artifactLower = artifact.toLowerCase
      val projectLower = project.repository.toLowerCase
      val orgLower = project.organization.toLowerCase

      (queryLower, artifactLower, projectLower, orgLower) match
        case (q, a, _, _) if a == q => 1000
        case (q, a, _, _) if a.startsWith(q) => 800
        case (q, a, _, _) if a.contains(q) => 600
        case (q, _, p, _) if p.contains(q) => 400
        case (q, _, _, o) if o.contains(q) => 200
        case _ => 0

    val search: List[(Project, String, Option[String], ScalaTarget)] =
      val results = projects
        .flatMap { case (project, target) =>
          project.artifacts.map(artifact => (project, artifact, None, target))
        }
        .filter { projectAndArtifact =>
          !selectedProjectsArtifacts.contains(projectAndArtifact)
        }

      if query.nonEmpty then
        results.sortBy { case (project, artifact, _, _) =>
          -matchScore(query, artifact, project)
        }(Ordering[Int])
      else
        results.sortBy { case (project, artifact, _, _) =>
          (project.organization, project.repository, artifact)
        }

    def removeSelected(selected: Selected): SearchState =
      copy(selecteds = selecteds.filterNot(_.release.matches(selected.release)))

    def addSelected(selected: Selected): SearchState =
      copy(selecteds = selected :: selecteds.filterNot(_.release.matches(selected.release)))

    def updateVersion(selected: Selected, version: String): SearchState =
      val updated = selected.copy(
        release = selected.release.copy(version = version),
        options = selected.options.copy(version = version)
      )
      copy(selecteds = selecteds.filterNot(_.release.matches(updated.release)) :+ updated)

    def setProjects(projects: List[(Project, ScalaTarget)]): SearchState =
      copy(projects = projects)

    def clearProjects: SearchState =
      copy(projects = List())

  private object SearchState:
    def default: SearchState =
      SearchState(
        query = "",
        selectedIndex = 0,
        projects = List.empty,
        selecteds = List.empty
      )

  // API constants
  private val scaladexBaseUrl = "https://index.scala-lang.org"
  private val scaladexApiUrl = scaladexBaseUrl + "/api"

  // Helper functions
  private def toQuery(in: Map[String, String]): String =
    in.map { case (k, v) => s"$k=$v" }.mkString("?", "&", "")

  private def queryAndParse(t: SbtScalaTarget, query: String): Future[List[(Project, ScalaTarget)]] =
    val q = toQuery(t.scaladexRequest + ("q" -> query))
    for
      response <- dom.fetch(scaladexApiUrl + "/search" + q).toFuture
      text <- response.text().toFuture
    yield
      decode[List[Project]](text).getOrElse(Nil).map(_ -> t)

  private def fetchSelected(
    project: Project,
    artifact: String,
    target: SbtScalaTarget,
    version: Option[String]
  ): Future[Option[Selected]] =
    val query = toQuery(
      Map(
        "organization" -> project.organization,
        "repository" -> project.repository
      ) ++ target.scaladexRequest
    )

    for
      response <- dom.fetch(scaladexApiUrl + "/project" + query).toFuture
      text <- response.text().toFuture

      artifactResponse <- dom.fetch(scaladexApiUrl + s"/v1/projects/${project.organization}/${project.repository}/versions/latest").toFuture
      artifactText <- artifactResponse.text().toFuture
      artifactJson = parse(artifactText).getOrElse(Json.Null)

      matchingArtifact: Option[Json] = artifactJson.asArray.getOrElse(Vector.empty).find { artifactObj =>
        val artifactId = artifactObj.hcursor.get[String]("artifactId").getOrElse("")
        val targetSuffix = target.targetType match
          case ScalaTargetType.Scala3 => "_3"
          case ScalaTargetType.Scala2 => s"_${target.binaryScalaVersion}"
          case ScalaTargetType.JS => s"_sjs1_${target.binaryScalaVersion}"
          case _ => ""
        artifactId == artifact || artifactId == s"$artifact$targetSuffix"
      }
      matchingGroupId = matchingArtifact.flatMap(obj => obj.hcursor.get[String]("groupId").toOption)
      matchingVersion = matchingArtifact.flatMap(obj => obj.hcursor.get[String]("version").toOption).orElse(version)
    yield
      decode[ReleaseOptions](text).toOption.map { options =>
        Selected(
          project = project,
          release = ScalaDependency(
            groupId = matchingGroupId.getOrElse(options.groupId),
            artifact = artifact,
            target = target,
            version = matchingVersion.getOrElse(options.version)
          ),
          options = options
        )
      }

  /**
   * Create a Scaladex search element.
   *
   * @param libraries Signal with the current set of libraries
   * @param scalaTarget Signal with the current Scala target
   * @param removeScalaDependency Observer for removing dependencies
   * @param updateDependencyVersion Observer for updating dependency versions
   * @param addScalaDependency Observer for adding dependencies
   * @param isDarkTheme Signal indicating dark theme state
   * @param language Signal with the current language
   * @return Scaladex search element
   */
  def apply(
    libraries: Signal[Set[ScalaDependency]],
    scalaTarget: Signal[SbtScalaTarget],
    removeScalaDependency: Observer[ScalaDependency],
    updateDependencyVersion: Observer[(ScalaDependency, String)],
    addScalaDependency: Observer[(ScalaDependency, Project)],
    isDarkTheme: Signal[Boolean],
    language: Signal[String] = Val("en")
  ): HtmlElement =

    // Local state
    val stateVar = Var(SearchState.default)
    val searchInputRef = Ref[dom.HTMLInputElement]
    val projectListRef = Ref[dom.HTMLElement]

    // Event handlers
    def addArtifact(project: Project, artifact: String, version: Option[String], target: ScalaTarget): Unit =
      if !stateVar.now().selecteds.exists(_.matches(project, artifact)) then
        target match
          case sbtScalaTarget: SbtScalaTarget =>
            fetchSelected(project, artifact, sbtScalaTarget, version).foreach {
              case Some(selected) if !stateVar.now().selecteds.exists(_.release.matches(selected.release)) =>
                stateVar.update(_.addSelected(selected))
                addScalaDependency.onNext(selected.release -> selected.project)
              case _ => ()
            }
          case _ => ()

    def removeSelected(selected: Selected): Unit =
      removeScalaDependency.onNext(selected.release)
      stateVar.update(_.removeSelected(selected))

    def updateVersion(selected: Selected, version: String): Unit =
      updateDependencyVersion.onNext(selected.release -> version)
      stateVar.update(_.updateVersion(selected, version))

    def fetchProjects(): Unit =
      val state = stateVar.now()
      scalaTarget.now() match
        case target: SbtScalaTarget if state.query.nonEmpty =>
          val projsForThisTarget = queryAndParse(target, state.query)
          val projects: Future[List[(Project, ScalaTarget)]] = target match
            case Scala3(_) =>
              projsForThisTarget.flatMap { ls =>
                queryAndParse(Scala2(BuildInfo.latest213), state.query)
                  .map(arts213 => ls ::: arts213)
              }
            case _ => projsForThisTarget

          projects.foreach { projs =>
            stateVar.update(_.setProjects(projs))
          }
        case _ =>
          stateVar.update(_.clearProjects)

    def handleToolkitToggle(enabled: Boolean): Unit =
      val toolkitProject = Project(
        organization = "scala",
        repository = "toolkit",
        logo = Some("https://avatars.githubusercontent.com/u/57059?v=4"),
        artifacts = List("toolkit", "toolkit-test")
      )
      val artifact = "toolkit"
      val versionOpt: Option[String] = None

      if enabled then
        addArtifact(toolkitProject, artifact, versionOpt, scalaTarget.now())
      else
        stateVar.now().selecteds.find { selected =>
          selected.release.groupId == "org.scala-lang" &&
          selected.release.artifact == "toolkit" &&
          selected.release.target == scalaTarget.now()
        }.foreach(removeSelected)

    // Render helpers
    def renderProject(
      project: Project,
      artifact: String,
      scalaTarget: ScalaTarget,
      currentTarget: SbtScalaTarget,
      selected: Modifier[HtmlElement] = emptyMod,
      handlers: Modifier[HtmlElement] = emptyMod,
      remove: Modifier[HtmlElement] = emptyMod,
      options: Modifier[HtmlElement] = emptyMod
    ): HtmlElement =
      import project.*

      val common = Seq(title := organization, cls := "logo")
      val artifact2 = artifact
        .replace(project.repository + "-", "")
        .replace(project.repository, "")

      val label =
        if project.repository != artifact then
          s"${project.repository} / $artifact2"
        else artifact

      val scaladexLink =
        s"https://scaladex.scala-lang.org/$organization/$repository/$artifact"

      div(
        cls := "result",
        selected,
        handlers,
        a(cls := "scaladexresult", href := scaladexLink, target := "_blank",
          i(cls := "fa fa-external-link")
        ),
        remove,
        span(
          logo
            .map(url => img(src := (url + "&s=40"), common, alt := s"$organization logo or avatar"))
            .getOrElse(img(src := Assets.placeholder, common, alt := s"placeholder logo for $organization")),
          span(cls := "artifact", label),
          options,
          if scalaTarget.binaryScalaVersion != currentTarget.binaryScalaVersion then
            span(cls := "artifact", s"(Scala ${scalaTarget.binaryScalaVersion} artifacts)")
          else emptyNode
        )
      )

    def renderOptions(selected: Selected): HtmlElement =
      div(
        cls := "select-wrapper",
        select(
          selected.options.versions.reverse.map(v => option(value := v, v)),
          value := selected.release.version,
          onInput.mapToValue --> Observer[String](version => updateVersion(selected, version))
        )
      )

    def toolkitSwitch(isEnabled: Boolean, isDark: Boolean): HtmlElement =
      val switchId = "switch-toolkit"
      val sliderClass = if isDark then "switch-slider dark" else "switch-slider"

      div(
        cls := "toolkit-switch",
        div(cls := "switch",
          input(
            typ := "checkbox",
            cls := "switch-input",
            idAttr := switchId,
            checked := isEnabled,
            onInput.mapToChecked --> Observer[Boolean](handleToolkitToggle)
          ),
          label(
            cls := sliderClass,
            forId := switchId
          )
        ),
        span(cls := "switch-description", I18n.t("build.enable_toolkit"))
      )

    // Main component
    div(
      cls := "search library",

      // Toolkit switch
      child <-- Signal.combine(libraries, scalaTarget, isDarkTheme).map { case (libs, target, dark) =>
        val toolkitEnabled = libs.exists { dep =>
          dep.groupId == "org.scala-lang" &&
          dep.artifact == "toolkit" &&
          dep.target == target
        }
        toolkitSwitch(toolkitEnabled, dark)
      },

      // Added libraries
      div(
        cls := "added",
        display <-- stateVar.signal.map(state => if state.selecteds.isEmpty then "none" else "block"),
        children <-- Signal.combine(stateVar.signal, scalaTarget).map { case (state, target) =>
          state.selecteds.sortBy(s => (s.project.organization, s.project.repository, s.release.artifact)).map { selected =>
            renderProject(
              selected.project,
              selected.release.artifact,
              selected.release.target,
              target,
              remove = i(
                cls := "fa fa-close remove",
                onClick --> Observer[dom.MouseEvent](_ => removeSelected(selected))
              ),
              options = renderOptions(selected)
            )
          }
        }
      ),

      // Search input
      div(
        cls := "search-input",
        input.withRef(searchInputRef)(
          typ := "search",
          cls := "search-query",
          placeholder := I18n.t("build.search_placeholder"),
          controlled(
            value <-- stateVar.signal.map(_.query),
            onInput.mapToValue --> Observer[String] { query =>
              stateVar.update(_.copy(query = query))
              fetchProjects()
            }
          ),
          onKeyDown --> Observer[dom.KeyboardEvent] { e =>
            val state = stateVar.now()
            e.keyCode match
              case 40 | 38 => // Down or Up arrow
                e.preventDefault()
                val diff = if e.keyCode == 40 then 1 else -1
                val newIndex = math.max(0, math.min(state.search.size - 1, state.selectedIndex + diff))
                stateVar.update(_.copy(selectedIndex = newIndex))

                // Scroll to selected
                projectListRef.foreach { listRef =>
                  val scrollHeight = listRef.scrollHeight
                  val total = state.search.size
                  if total > 0 then
                    listRef.scrollTop = (scrollHeight.toDouble / total.toDouble * newIndex.toDouble).abs
                }

              case 13 => // Enter
                if state.selectedIndex >= 0 && state.selectedIndex < state.search.size then
                  val (p, a, v, t) = state.search(state.selectedIndex)
                  addArtifact(p, a, v, t)
                  searchInputRef.foreach(_.focus())

              case _ => ()
          }
        ),
        div(
          cls := "close",
          display <-- stateVar.signal.map(state => if state.search.isEmpty then "none" else "inline-block"),
          i(
            cls := "fa fa-close",
            onClick --> Observer[dom.MouseEvent] { _ =>
              stateVar.update(_.copy(query = "", projects = Nil))
            }
          )
        )
      ),

      // Search results
      div.withRef(projectListRef)(
        cls := "results",
        display <-- stateVar.signal.map(state => if state.search.isEmpty then "none" else "block"),
        children <-- Signal.combine(stateVar.signal, scalaTarget).map { case (state, target) =>
          state.search.zipWithIndex.map { case ((project, artifact, version, scalaTarget), index) =>
            renderProject(
              project,
              artifact,
              scalaTarget,
              target,
              selected = cls := (if index == state.selectedIndex then "selected" else ""),
              handlers = Seq(
                onClick --> Observer[dom.MouseEvent](_ => addArtifact(project, artifact, version, scalaTarget)),
                onMouseOver --> Observer[dom.MouseEvent](_ => stateVar.update(_.copy(selectedIndex = index)))
              )
            )
          }
        }
      )
    )

  /**
   * Static version with concrete values.
   */
  def apply(
    libraries: Set[ScalaDependency],
    scalaTarget: SbtScalaTarget,
    removeScalaDependency: Observer[ScalaDependency],
    updateDependencyVersion: Observer[(ScalaDependency, String)],
    addScalaDependency: Observer[(ScalaDependency, Project)],
    isDarkTheme: Boolean,
    language: String
  ): HtmlElement =
    apply(
      Val(libraries),
      Val(scalaTarget),
      removeScalaDependency,
      updateDependencyVersion,
      addScalaDependency,
      Val(isDarkTheme),
      Val(language)
    )
