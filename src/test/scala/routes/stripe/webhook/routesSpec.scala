 package com.stripe.webhook

 import cats.effect.IO
 import cats.effect.concurrent.Ref
 import com.stripe.{MockUsers, MockWebhookSignature}
 import com.stripe.StripeCustomerId
 import com.stripe.users.UserId
 import io.chrisdavenport.fuuid.FUUID
 import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
 import org.http4s.{Header, Headers, Method, Request, Uri}
 import org.scalatest.{Matchers, WordSpec}
 import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
 import org.http4s.implicits._
 import io.circe.syntax._
 import io.circe.generic.auto._
 import io.circe.parser._
 import io.circe.Json
 import org.http4s.circe.CirceEntityDecoder._
 import org.http4s.circe.CirceEntityEncoder._
 import com.stripe.utils.arbitraries._

 class WebhookRoutesSpec extends WordSpec with Matchers with ScalaCheckPropertyChecks {

   "webhook route should decode stripe event and verify signature" in {
     forAll { (userId: UserId, customerId: StripeCustomerId) =>

       val verifySignature = Ref.of[IO, Option[Unit]](None).unsafeRunSync()
       val mockUsers = new MockUsers {
         override def create(userId: UserId, custId: StripeCustomerId): IO[UserId] = IO.pure(userId)
       }
       val mockWebhookSignature = new MockWebhookSignature {
         override def verify(headers: Headers, body: String): IO[Unit] = verifySignature.set(Some(())).void
       }

       val sampleEvent =
       s"""
       {
        "id": "evt_1Ep24XHssDVaQm2PpwS19Yt0",
        "object": "event",
        "api_version": "2019-03-14",
        "created": 1561420781,
        "data": {
          "object": {
            "id": "cs_test_MlZAaTXUMHjWZ7DcXjusJnDU4MxPalbtL5eYrmS2GKxqscDtpJq8QM0k",
            "object": "checkout.session",
            "billing_address_collection": null,
            "cancel_url": "https://example.com/cancel",
            "client_reference_id": "${userId.value.show}",
            "customer": "${customerId.value}",
            "customer_email": null,
            "display_items": [],
            "mode": "setup",
            "setup_intent": "seti_1EzVO3HssDVaQm2PJjXHmLlM",
            "submit_type": null,
            "subscription": null,
            "success_url": "https://example.com/success"
          }
        },
        "livemode": false,
        "pending_webhooks": 1,
        "request": {
          "id": null,
          "idempotency_key": null
        },
        "type": "checkout.session.completed"
      }"""

       val checkoutCompleted = CheckoutCompleted(
         CheckoutCompletedData(
           CheckoutCompletedDataObject(
             userId,
             customerId
           )
         )
       )

       implicit val logger = Slf4jLogger.create[IO].unsafeRunSync()
       val routes = WebhookRoutes.build(mockWebhookSignature, mockUsers).orNotFound
       val jsonEvent = parse(sampleEvent).toOption.get
       val req = Request[IO](method = Method.POST, uri = Uri.unsafeFromString(s"/webhook/checkout"))
         .withEntity(jsonEvent)
         .withHeaders(Header("Stripe-Signature", "stripe-signature"))
       val routeResult = routes.run(req).unsafeRunSync

       routeResult.status.isSuccess should be(true)
       verifySignature.get.unsafeRunSync() should be (Some(()))
     }
   }
 }
