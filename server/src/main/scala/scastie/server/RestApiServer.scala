package scastie.server

import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.olegych.scastie.api._
import com.olegych.scastie.balancer._

class RestApiServer(
  dispatchActor: ActorRef,
  maybeUser: Option[User],
  ip: Option[String] = None
)(implicit executionContext: ExecutionContext)
  extends RestApi {

  implicit val timeout: Timeout = Timeout(20.seconds)

  private def wrap(inputs: Inputs): InputsWithIpAndUser = InputsWithIpAndUser(inputs, UserTrace(ip, maybeUser))

  def run(inputs: Inputs): Future[SnippetId] = {
    dispatchActor
      .ask(RunSnippet(wrap(inputs)))
      .mapTo[SnippetId]
  }

  def format(formatRequest: FormatRequest): Future[FormatResponse] = {
    dispatchActor
      .ask(formatRequest)
      .mapTo[FormatResponse]
  }

  def save(inputs: Inputs): Future[SnippetId] = {
    dispatchActor
      .ask(SaveSnippet(wrap(inputs)))
      .mapTo[SnippetId]
  }

  def update(editInputs: EditInputs): Future[Option[SnippetId]] = {
    import editInputs._
    if (snippetId.isOwnedBy(maybeUser)) {
      dispatchActor
        .ask(UpdateSnippet(snippetId, wrap(inputs)))
        .mapTo[Option[SnippetId]]
    } else {
      Future.successful(None)
    }
  }

  def delete(snippetId: SnippetId): Future[Boolean] = {
    if (snippetId.isOwnedBy(maybeUser)) {
      dispatchActor
        .ask(DeleteSnippet(snippetId))
        .mapTo[Unit]
        .map(_ => true)
    } else {
      Future.successful(false)
    }
  }

  def fork(editInputs: EditInputs): Future[Option[SnippetId]] = {
    import editInputs._
    dispatchActor
      .ask(ForkSnippet(snippetId, wrap(inputs)))
      .mapTo[Option[SnippetId]]
  }

  def fetch(snippetId: SnippetId): Future[Option[FetchResult]] = {
    dispatchActor
      .ask(FetchSnippet(snippetId))
      .mapTo[Option[FetchResult]]
  }

  def fetchUser(): Future[Option[User]] = {
    Future.successful(maybeUser)
  }

  def fetchUserSnippets(): Future[List[SnippetSummary]] = {
    maybeUser match {
      case Some(user) => dispatchActor
          .ask(FetchUserSnippets(user))
          .mapTo[List[SnippetSummary]]
      case _ => Future.successful(Nil)
    }
  }
}
