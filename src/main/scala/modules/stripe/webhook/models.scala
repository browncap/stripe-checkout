package com.stripe.webhook

import com.stripe.StripeCustomerId
import com.stripe.users.UserId
import io.circe.Decoder
import io.circe.generic.semiauto._

import scala.concurrent.duration.FiniteDuration
import scala.util.control.NoStackTrace

case class Tolerance(value: FiniteDuration)

case class CheckoutCompletedDataObject(client_reference_id: UserId, customer: StripeCustomerId)
object CheckoutCompletedDataObject {
  implicit def decoder: Decoder[CheckoutCompletedDataObject] = deriveDecoder
}

case class CheckoutCompletedData(`object`: CheckoutCompletedDataObject)
object CheckoutCompletedData {
  implicit def decoder: Decoder[CheckoutCompletedData] = deriveDecoder
}

case class CheckoutCompleted(data: CheckoutCompletedData)
object CheckoutCompleted {
  implicit def decoder: Decoder[CheckoutCompleted] = deriveDecoder
}

// Errors

case object SignatureNotFound extends RuntimeException with NoStackTrace
case object InvalidSignature extends RuntimeException with NoStackTrace
case object SignatureMismatch extends RuntimeException with NoStackTrace
case object CurrentTimeOutsideTolerance extends RuntimeException with NoStackTrace
