package com.olegych.scastie.api

import io.circe._
import io.circe.generic.semiauto._

object User {
  // low tech solution
  val admins: Set[String] = Set(
    "Duhemm",
    "heathermiller",
    "julienrf",
    "jvican",
    "MasseGuillaume",
    "olafurpg",
    "OlegYch"
  )

  implicit val userCodec: Codec[User] = deriveCodec[User]
}
case class User(login: String, name: Option[String], avatar_url: String) {
  def isAdmin: Boolean = User.admins.contains(login)
}

object SnippetUserPart {
  implicit val snippetUserPartCodec: Codec[SnippetUserPart] = deriveCodec[SnippetUserPart]
}

case class SnippetUserPart(login: String, update: Int = 0)

object SnippetId {
  implicit val snippetIdEncoder: Encoder[SnippetId] = deriveEncoder[SnippetId]
  implicit val snippetIdDecoder: Decoder[SnippetId] = deriveDecoder[SnippetId]
}

case class SnippetId(base64UUID: String, user: Option[SnippetUserPart]) {
  def isOwnedBy(user2: Option[User]): Boolean = {
    (user, user2) match {
      case (Some(SnippetUserPart(snippetLogin, _)), Some(User(userLogin, _, _))) =>
        snippetLogin == userLogin
      case _ => false
    }
  }

  override def toString: String = url

  def url: String = {
    this match {
      case SnippetId(uuid, None) => uuid
      case SnippetId(uuid, Some(SnippetUserPart(login, update))) =>
        s"$login/$uuid/$update"
    }
  }

  def scalaJsUrl(end: String): String = {
    val middle = url
    s"/api/${Shared.scalaJsHttpPathPrefix}/$middle/$end"
  }
}
