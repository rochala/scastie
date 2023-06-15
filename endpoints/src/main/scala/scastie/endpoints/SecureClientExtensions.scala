package scastie.endpoints

import sttp.tapir._

object SecureClientExtensions {
  implicit class SecureScalaJSClient[I, E, O, R](endpoint: Endpoint[_ <: OAuthEndpoints.Session, I, E, O, R]) {
    def clientEndpoint: Endpoint[Option[String], I, E, O, Any] =
      endpoint.copy(securityInput = header[Option[String]](OAuthEndpoints.xsrfHeaderName))
  }
}
