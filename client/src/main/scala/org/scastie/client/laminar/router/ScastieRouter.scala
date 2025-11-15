package org.scastie.client.laminar.router

import com.raquo.waypoint.*
import org.scastie.api.{SnippetId, SnippetUserPart, ScalaTargetType}
import org.scastie.client.Page
import urldsl.vocabulary.UrlMatching
import org.scalajs.dom

/**
 * Waypoint router for Scastie - Laminar version
 *
 * Simplified routing based on the React router in Routing.scala
 */
object ScastieRouter:

  // Route definitions
  val homeRoute: Route[Page, Unit] =
    Route.static(Page.Home, root)

  val embeddedRoute: Route[Page, Unit] =
    Route.static(Page.Embedded, root / "embedded")

  // Anonymous snippet route
  val anonSnippetRoute: Route[Page, String] =
    Route(
      encode = {
        case Page.AnonymousResource(uuid) => uuid
        case _ => ""
      },
      decode = arg => Page.AnonymousResource(arg),
      pattern = root / segment[String]
    )

  // User snippet route
  val userSnippetRoute: Route[Page, (String, String)] =
    Route(
      encode = {
        case Page.UserResource(login, uuid) => (login, uuid)
        case _ => ("", "")
      },
      decode = { case (login, uuid) => Page.UserResource(login, uuid) },
      pattern = root / segment[String] / segment[String]
    )

  // User snippet with update route
  val userSnippetUpdateRoute: Route[Page, (String, String, Int)] =
    Route(
      encode = {
        case Page.UserResourceUpdated(login, uuid, update) => (login, uuid, update)
        case _ => ("", "", 0)
      },
      decode = { case (login, uuid, update) => Page.UserResourceUpdated(login, uuid, update) },
      pattern = root / segment[String] / segment[String] / segment[Int]
    )

  // Old snippet ID route
  val oldSnippetRoute: Route[Page, Int] =
    Route(
      encode = {
        case Page.OldSnippetIdPage(id) => id
        case _ => 0
      },
      decode = id => Page.OldSnippetIdPage(id),
      pattern = root / segment[Int]
    )

  // All routes
  val routes = List(
    homeRoute,
    embeddedRoute,
    anonSnippetRoute,
    userSnippetRoute,
    userSnippetUpdateRoute,
    oldSnippetRoute
  )

  /** Create router instance */
  def create(
    origin: String = dom.window.location.origin.get,
    initialPage: Page = Page.Home
  ): Router[Page] =
    new Router[Page](
      routes = routes,
      getPageTitle = pageToTitle,
      serializePage = pageToPath,
      deserializePage = pathToPage
    )(
      popStateEvents = windowEvents(_.onPopState),
      owner = unsafeWindowOwner
    )

  /** Convert page to title */
  private def pageToTitle(page: Page): String = page match
    case Page.Home => "Scastie"
    case Page.Embedded => "Scastie - Embedded"
    case Page.AnonymousResource(uuid) => s"Scastie - $uuid"
    case Page.UserResource(login, uuid) => s"Scastie - $login/$uuid"
    case Page.UserResourceUpdated(login, uuid, update) => s"Scastie - $login/$uuid/$update"
    case Page.OldSnippetIdPage(id) => s"Scastie - Snippet $id"
    case Page.TargetTypePage(targetType, _) => s"Scastie - $targetType"
    case _ => "Scastie"

  /** Convert page to path */
  private def pageToPath(page: Page): String = page match
    case Page.Home => "/"
    case Page.Embedded => "/embedded"
    case Page.AnonymousResource(uuid) => s"/$uuid"
    case Page.UserResource(login, uuid) => s"/$login/$uuid"
    case Page.UserResourceUpdated(login, uuid, update) => s"/$login/$uuid/$update"
    case Page.OldSnippetIdPage(id) => s"/$id"
    case Page.TargetTypePage(targetType, Some(code)) => s"/?target=${targetType.toString.toLowerCase}&c=$code"
    case Page.TargetTypePage(targetType, None) => s"/?target=${targetType.toString.toLowerCase}"
    case _ => "/"

  /** Convert path to page (simplified) */
  private def pathToPage(path: String): Page =
    // This is a simplified version - full parsing would need query params, etc.
    if path == "/" || path.isEmpty then
      Page.Home
    else
      Page.Home

  /** Convert SnippetId to Page */
  def snippetIdToPage(snippetId: SnippetId): Page =
    snippetId match
      case SnippetId(uuid, None) =>
        Page.AnonymousResource(uuid)
      case SnippetId(uuid, Some(SnippetUserPart(login, 0))) =>
        Page.UserResource(login, uuid)
      case SnippetId(uuid, Some(SnippetUserPart(login, update))) =>
        Page.UserResourceUpdated(login, uuid, update)
