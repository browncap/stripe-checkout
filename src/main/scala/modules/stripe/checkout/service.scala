package com.stripe.checkout

import cats.effect.Sync
import cats.implicits._
import com.stripe.StripeClient
import com.stripe.users.UserId

trait Checkout[F[_]] {
  def session(userId: UserId): F[CheckoutSessionId]
}

object Checkout {
  def build[F[_]](stripeClient: StripeClient[F])(implicit F: Sync[F]): Checkout[F] = {
    new Checkout[F] {

      override def session(userId: UserId): F[CheckoutSessionId] = stripeClient.createCheckoutSession(userId).map(_.id)
    }
  }
}
