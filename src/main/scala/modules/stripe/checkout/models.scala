package com.stripe.checkout

import cats.Show
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import io.circe.generic.extras.semiauto.{deriveUnwrappedDecoder, deriveUnwrappedEncoder}

final case class CheckoutSessionId(value: String)
object CheckoutSessionId {
  implicit val decoder: Decoder[CheckoutSessionId] = deriveUnwrappedDecoder
  implicit val encoder: Encoder[CheckoutSessionId] = deriveUnwrappedEncoder
  implicit val show: Show[CheckoutSessionId] = Show.show(_.value)
}
