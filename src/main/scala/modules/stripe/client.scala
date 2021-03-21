package com.stripe

import cats.implicits._
import cats.effect.Sync
import com.stripe.config.StripeConfig
import com.stripe.users.UserId
import io.chrisdavenport.log4cats.Logger
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.Client
import org.http4s.headers.Authorization
import org.http4s.implicits._

import scala.concurrent.ExecutionContext

trait StripeClient[F[_]] {
  def createCheckoutSession(userId: UserId): F[CheckoutSessionResponse]
}

object StripeClient {

  def build[F[_]](config: StripeConfig,
                  client: Client[F])
                 (implicit F: Sync[F], EC: ExecutionContext, L: Logger[F]): StripeClient[F] =
    new StripeClient[F] {

      def createCheckoutSession(userId: UserId): F[CheckoutSessionResponse] = {
        val req = Request[F](
          method = Method.POST,
          uri = uri"https://api.stripe.com/v1/checkout/sessions",
          headers = Headers.of(Authorization(Credentials.Token(AuthScheme.Bearer, config.secretKey)))
        ).withEntity(
          UrlForm(
            "payment_method_types[]" -> "card",
            "mode" -> "setup",
            "success_url" -> "https://example.com/success?session_id={CHECKOUT_SESSION_ID}",
            "cancel_url" -> "https://example.com/cancel",
            "client_reference_id" -> userId.value.show
          )
        )
        client.expect[CheckoutSessionResponse](req)
      }
    }

}
