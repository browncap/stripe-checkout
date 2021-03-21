package com.stripe.users

import io.chrisdavenport.fuuid.FUUID
import io.chrisdavenport.fuuid.circe._
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import io.circe.generic.extras.semiauto.{deriveUnwrappedDecoder, deriveUnwrappedEncoder}

import scala.util.control.NoStackTrace

final case class UserId(value: FUUID)
object UserId {
  implicit val decoder: Decoder[UserId] = deriveUnwrappedDecoder
  implicit val encoder: Encoder[UserId] = deriveUnwrappedEncoder
}

// Errors

final case class UserNotFound(value: String) extends NoStackTrace
final case class UserIdAlreadyExists(value: UserId) extends NoStackTrace
