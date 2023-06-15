package scastie.endpoints

import com.olegych.scastie.api._
import sttp.tapir._

object SnippetIdUtils {
  type EmbeddedSnippetId = SnippetId
  type NormalSnippetId   = SnippetId

  // TODO: Migrate to opaque types
  type MaybeEmbeddedSnippet = Either[NormalSnippetId, EmbeddedSnippetId]

  sealed trait FrontPageSnippet { def content: String }
  case class EmbeddedSnippet(content: String)  extends FrontPageSnippet
  case class UniversalSnippet(content: String) extends FrontPageSnippet

  implicit class MaybeEmbeddedSnippedExtensions(maybeEmbeddedSnippet: MaybeEmbeddedSnippet) {

    def extractSnippetId: SnippetId = maybeEmbeddedSnippet match {
      case Left(snippetId)  => snippetId
      case Right(snippetId) => snippetId
    }

    def path: List[String] = extractSnippetId.path

    def user: String      = extractSnippetId.user.fold("")(_.login)
    def snippetId: String = extractSnippetId.base64UUID
    def rev: String       = extractSnippetId.user.fold("")(_.update.toString)
  }

  def toMaybeSnippetId(paths: List[String]): DecodeResult[MaybeEmbeddedSnippet] = paths match {
    case init :+ last if last.endsWith(".js") => toSnippetId(init :+ last.stripSuffix(".js")).map(Right(_))
    case other                                => toSnippetId(other).map(Left(_))
  }

  def toSnippetId(paths: List[String]): DecodeResult[SnippetId] = DecodeResult.fromOption(paths match {
    case user :: base64UUID :: rev :: Nil =>
      rev.toIntOption.map(rev => SnippetId(base64UUID, Some(SnippetUserPart(user, rev))))
    case user :: base64UUID :: Nil => Some(SnippetId(base64UUID, Some(SnippetUserPart(user))))
    case base64UUID :: Nil         => Some(SnippetId(base64UUID, None))
    case _                         => None
  })

}
