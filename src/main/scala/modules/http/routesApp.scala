package com.stripe.http

import cats.effect.Sync
import cats.syntax.all._
import com.stripe.webhook.{WebhookRoutes, WebhookSignature}
import com.stripe.checkout.{Checkout, CheckoutRoutes}
import com.stripe.users.Users
import io.chrisdavenport.log4cats.Logger
import org.http4s.implicits._
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.server.middleware.CORS

trait RoutesApp[F[_]] {
  val app: HttpApp[F]
}

object RoutesApp {
  def build[F[_]](users: Users[F], checkout: Checkout[F], webhookSig: WebhookSignature[F])(implicit F: Sync[F], L: Logger[F]): RoutesApp[F] = {
    new RoutesApp[F] {
      private val ping = Ping.route
      private val checkoutRoutes = CheckoutRoutes.build(checkout)
      private val webhookRoutes = WebhookRoutes.build(webhookSig, users)
      private val routes: HttpRoutes[F] = ping <+> checkoutRoutes <+> webhookRoutes

      override val app: HttpApp[F] = CORS(routes).orNotFound
    }
  }
}
