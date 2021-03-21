package com.stripe

import cats.effect.IO
import com.stripe.webhook.WebhookSignature
import com.stripe.StripeCustomerId
import com.stripe.checkout.{Checkout, CheckoutSessionId}
import com.stripe.users.{UserId, Users}
import org.http4s.Headers

class MockCheckout extends Checkout[IO] {
  def session(userId: UserId): IO[CheckoutSessionId] = ???
}

class MockWebhookSignature extends WebhookSignature[IO] {
  def verify(headers: Headers, body: String): IO[Unit] = ???
}

class MockUsers extends Users[IO] {
  def create(userId: UserId, custId: StripeCustomerId): IO[UserId] = ???
}
