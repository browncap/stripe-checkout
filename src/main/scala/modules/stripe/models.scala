package com.stripe

import cats.Show
import com.stripe.checkout.CheckoutSessionId
import io.circe.Decoder
import io.circe.generic.semiauto._
import io.circe.generic.extras.semiauto.deriveUnwrappedDecoder

final case class StripeCustomerId(value: String)
object StripeCustomerId {
  implicit val decoder: Decoder[StripeCustomerId] = deriveUnwrappedDecoder
  implicit val show: Show[StripeCustomerId] = Show.show(_.value)
}

final case class CheckoutSessionResponse(id: CheckoutSessionId)
object CheckoutSessionResponse {
  implicit val decoder: Decoder[CheckoutSessionResponse] = deriveDecoder[CheckoutSessionResponse]
}
