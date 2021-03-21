package com.stripe.webhook

import java.time.Instant

import cats.effect.{ContextShift, IO, Timer}
import org.http4s.{Header, Headers, Method, Request}
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import com.stripe.config.StripeConfig
import com.stripe.webhook.{CurrentTimeOutsideTolerance, InvalidSignature, SignatureMismatch, SignatureNotFound, StripeSignature, WebhookSignature}
import com.stripe.webhook.StripeSignature.{Signature, SignatureTimestamp}

import scala.concurrent.duration._
import scala.language.postfixOps

class StripeSignatureSpec extends WordSpec with Matchers with ScalaCheckPropertyChecks with enumeratum.ScalacheckInstances {

  implicit val cs: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.Implicits.global)
  implicit val t: Timer[IO] = IO.timer(scala.concurrent.ExecutionContext.Implicits.global)

  val testSignatureValue =
    "t=1577569768,v1=57b093a94c485f30e408bbf9702271751b1d7b90ce5eaab1b166e9e538c24a0b,v0=a062dc0980e938ad3e75d5b83ea56ad257861e817dc5587ed650ce2d0f74f5b3"

  val defaultSigningSecret = "whsec_ycky6ryEXFBaoQtO2wRdEXVZp74So1Rl"

  "StripeSignature" should {
    "properly convert a normal signature to the model" in {
      val result =
        StripeSignature(
          Signature("57b093a94c485f30e408bbf9702271751b1d7b90ce5eaab1b166e9e538c24a0b"),
          SignatureTimestamp(Instant.ofEpochMilli(1577569768L)))

      StripeSignature.pieces(testSignatureValue) should be(Some(result))
    }
  }

  "StripeSignatureVerifier" should {
    "successfully verify a valid request" in {
      val signingSecret = "whsec_A2EdfajGiGHx9jFmkP7ia6j25VXh9IWV"

      val ssv = WebhookSignature.build[IO](StripeConfig("secretKey", signingSecret, 106580 days))

      val payload = "{\n  \"id\": \"evt_YEEz4JGN6UwGkCjdqr7wfPqL\",\n  \"object\": \"event\",\n  \"api_version\": \"2017-06-05\",\n  \"created\": 1509023721,\n  \"data\": {\n    \"object\": {\n      \"id\": \"my-first-test-plan\",\n      \"object\": \"plan\",\n      \"amount\": 599,\n      \"created\": 1509023721,\n      \"currency\": \"gbp\",\n      \"interval\": \"month\",\n      \"interval_count\": 1,\n      \"livemode\": false,\n      \"metadata\": {\n      },\n      \"name\": \"My first test plan\",\n      \"statement_descriptor\": \"My first test plan\",\n      \"trial_period_days\": 31\n    }\n  },\n  \"livemode\": false,\n  \"pending_webhooks\": 1,\n  \"request\": {\n    \"id\": \"req_6UwGkCjdqr7wfPqLAq\",\n    \"idempotency_key\": null\n  },\n  \"type\": \"plan.created\"\n}"
      val header = "t=1509023726,v1=e786c1d46f2fa3bbd57cf974e89533ecb89ba2ea006c8ef672da3e7cb2c1101d,v0=6d9e8a0b0de1d3f363c85f02842950c056dbb453d70863d4d2900214396052ac"

      val request =
        Request[IO](
          method = Method.POST,
          headers = Headers.of(Header("Stripe-Signature", header))).withEntity(payload)

      ssv.verify(request.headers, request.bodyAsText.compile.string.map(_.trim).unsafeRunSync()).unsafeRunSync() should be(())
    }

    "successfully verify a valid request with different info" in {
      val ssv = WebhookSignature.build[IO](StripeConfig("secretKey", defaultSigningSecret, 106580 days))

      val request =
        Request[IO](
          method = Method.POST,
          headers = Headers.of(Header("Stripe-Signature", testSignatureValue))).withEntity(testString)

      ssv.verify(request.headers, request.bodyAsText.compile.string.map(_.trim).unsafeRunSync()).unsafeRunSync() should be(())
    }

    "fail if no Stripe-Signature header exists" in {
      val ssv = WebhookSignature.build[IO](StripeConfig("secretKey", defaultSigningSecret, 106580 days))

      val request =
        Request[IO](
          method = Method.POST,
          headers = Headers.of(Header("Nope", testSignatureValue))).withEntity(testString)

      ssv.verify(request.headers, request.bodyAsText.compile.string.map(_.trim).unsafeRunSync()).attempt.unsafeRunSync() should be(Left(SignatureNotFound))
    }

    "fail if bad a signature exists in the header - no v1" in {
      val ssv = WebhookSignature.build[IO](StripeConfig("secretKey", defaultSigningSecret, 106580 days))

      val badSignatureValue =
        "t=1577559210,v0=894487bcea14a9fc9d873a96b22351d6846940cb1f67376347f78a32f4b8cbab,v0=c2f34cbfab2b6dd329ab43e3ec4838e93164361c63fc019a4df69953628bd56c"

      val request =
        Request[IO](
          method = Method.POST,
          headers = Headers.of(Header("Stripe-Signature", badSignatureValue))).withEntity(testString)

      ssv.verify(request.headers, request.bodyAsText.compile.string.map(_.trim).unsafeRunSync()).attempt.unsafeRunSync() should be(Left(InvalidSignature))
    }

    "return SignatureMismatch if the body has been changed" in {
      val ssv = WebhookSignature.build[IO](StripeConfig("secretKey", defaultSigningSecret, 106580 days))

      val request =
        Request[IO](
          method = Method.POST,
          headers = Headers.of(Header("Stripe-Signature", testSignatureValue))).withEntity(doesntMatchSignature)

      val result = ssv.verify(request.headers, request.bodyAsText.compile.string.map(_.trim).unsafeRunSync()).attempt.unsafeRunSync()

      result should be(Left(SignatureMismatch))
    }

    "fail if the tolerance check fails" in {
      val ssv = WebhookSignature.build[IO](StripeConfig("secretKey", defaultSigningSecret, 0 minutes))

      val request =
        Request[IO](
          method = Method.POST,
          headers = Headers.of(Header("Stripe-Signature", testSignatureValue))).withEntity(testString)

      val result = ssv.verify(request.headers, request.bodyAsText.compile.string.map(_.trim).unsafeRunSync()).attempt.unsafeRunSync()

      result should be(Left(CurrentTimeOutsideTolerance))
    }
  }

  val testString =
    """
      |{
      |  "id": "evt_1FunA3KZrwLxsXzPANND8HSc",
      |  "object": "event",
      |  "api_version": "2019-12-03",
      |  "created": 1577569767,
      |  "data": {
      |    "object": {
      |      "id": "pi_1FunA2KZrwLxsXzPVTRdS9lz",
      |      "object": "payment_intent",
      |      "amount": 2000,
      |      "amount_capturable": 0,
      |      "amount_received": 2000,
      |      "application": null,
      |      "application_fee_amount": null,
      |      "canceled_at": null,
      |      "cancellation_reason": null,
      |      "capture_method": "automatic",
      |      "charges": {
      |        "object": "list",
      |        "data": [
      |          {
      |            "id": "ch_1FunA2KZrwLxsXzPdMyYXVTO",
      |            "object": "charge",
      |            "amount": 2000,
      |            "amount_refunded": 0,
      |            "application": null,
      |            "application_fee": null,
      |            "application_fee_amount": null,
      |            "balance_transaction": "txn_1FunA3KZrwLxsXzP5vydhlfU",
      |            "billing_details": {
      |              "address": {
      |                "city": null,
      |                "country": null,
      |                "line1": null,
      |                "line2": null,
      |                "postal_code": null,
      |                "state": null
      |              },
      |              "email": null,
      |              "name": null,
      |              "phone": null
      |            },
      |            "captured": true,
      |            "created": 1577569766,
      |            "currency": "usd",
      |            "customer": null,
      |            "description": "(created by Stripe CLI)",
      |            "destination": null,
      |            "dispute": null,
      |            "disputed": false,
      |            "failure_code": null,
      |            "failure_message": null,
      |            "fraud_details": {
      |            },
      |            "invoice": null,
      |            "livemode": false,
      |            "metadata": {
      |            },
      |            "on_behalf_of": null,
      |            "order": null,
      |            "outcome": {
      |              "network_status": "approved_by_network",
      |              "reason": null,
      |              "risk_level": "normal",
      |              "risk_score": 16,
      |              "seller_message": "Payment complete.",
      |              "type": "authorized"
      |            },
      |            "paid": true,
      |            "payment_intent": "pi_1FunA2KZrwLxsXzPVTRdS9lz",
      |            "payment_method": "pm_1FunA2KZrwLxsXzPBRDYF78M",
      |            "payment_method_details": {
      |              "card": {
      |                "brand": "visa",
      |                "checks": {
      |                  "address_line1_check": null,
      |                  "address_postal_code_check": null,
      |                  "cvc_check": null
      |                },
      |                "country": "US",
      |                "exp_month": 12,
      |                "exp_year": 2020,
      |                "fingerprint": "UjHgV6BrDB9joUHr",
      |                "funding": "credit",
      |                "installments": null,
      |                "last4": "4242",
      |                "network": "visa",
      |                "three_d_secure": null,
      |                "wallet": null
      |              },
      |              "type": "card"
      |            },
      |            "receipt_email": null,
      |            "receipt_number": null,
      |            "receipt_url": "https://pay.stripe.com/receipts/acct_1FmUMjKZrwLxsXzP/ch_1FunA2KZrwLxsXzPdMyYXVTO/rcpt_GRgXkaXq7Lvc4yZlzOcAQcETFC4LD0U",
      |            "refunded": false,
      |            "refunds": {
      |              "object": "list",
      |              "data": [
      |
      |              ],
      |              "has_more": false,
      |              "total_count": 0,
      |              "url": "/v1/charges/ch_1FunA2KZrwLxsXzPdMyYXVTO/refunds"
      |            },
      |            "review": null,
      |            "shipping": {
      |              "address": {
      |                "city": "San Francisco",
      |                "country": "US",
      |                "line1": "510 Townsend St",
      |                "line2": null,
      |                "postal_code": "94103",
      |                "state": "CA"
      |              },
      |              "carrier": null,
      |              "name": "Jenny Rosen",
      |              "phone": null,
      |              "tracking_number": null
      |            },
      |            "source": null,
      |            "source_transfer": null,
      |            "statement_descriptor": null,
      |            "statement_descriptor_suffix": null,
      |            "status": "succeeded",
      |            "transfer_data": null,
      |            "transfer_group": null
      |          }
      |        ],
      |        "has_more": false,
      |        "total_count": 1,
      |        "url": "/v1/charges?payment_intent=pi_1FunA2KZrwLxsXzPVTRdS9lz"
      |      },
      |      "client_secret": "pi_1FunA2KZrwLxsXzPVTRdS9lz_secret_ZzDd1paCaeQAqVw4DjJyw0yEr",
      |      "confirmation_method": "automatic",
      |      "created": 1577569766,
      |      "currency": "usd",
      |      "customer": null,
      |      "description": "(created by Stripe CLI)",
      |      "invoice": null,
      |      "last_payment_error": null,
      |      "livemode": false,
      |      "metadata": {
      |      },
      |      "next_action": null,
      |      "on_behalf_of": null,
      |      "payment_method": "pm_1FunA2KZrwLxsXzPBRDYF78M",
      |      "payment_method_options": {
      |        "card": {
      |          "installments": null,
      |          "request_three_d_secure": "automatic"
      |        }
      |      },
      |      "payment_method_types": [
      |        "card"
      |      ],
      |      "receipt_email": null,
      |      "review": null,
      |      "setup_future_usage": null,
      |      "shipping": {
      |        "address": {
      |          "city": "San Francisco",
      |          "country": "US",
      |          "line1": "510 Townsend St",
      |          "line2": null,
      |          "postal_code": "94103",
      |          "state": "CA"
      |        },
      |        "carrier": null,
      |        "name": "Jenny Rosen",
      |        "phone": null,
      |        "tracking_number": null
      |      },
      |      "source": null,
      |      "statement_descriptor": null,
      |      "statement_descriptor_suffix": null,
      |      "status": "succeeded",
      |      "transfer_data": null,
      |      "transfer_group": null
      |    }
      |  },
      |  "livemode": false,
      |  "pending_webhooks": 2,
      |  "request": {
      |    "id": "req_4WojcNl7163ktm",
      |    "idempotency_key": null
      |  },
      |  "type": "payment_intent.succeeded"
      |}
    """.stripMargin

  val doesntMatchSignature =
    """
      |{
      |  "id": "evt_1FunA3KZrwLxsXzPANND8HSC",
      |  "object": "event",
      |  "api_version": "2019-12-03",
      |  "created": 1577569767,
      |  "data": {
      |    "object": {
      |      "id": "pi_1FunA2KZrwLxsXzPVTRdS9lz",
      |      "object": "payment_intent",
      |      "amount": 2000,
      |      "amount_capturable": 0,
      |      "amount_received": 2000,
      |      "application": null,
      |      "application_fee_amount": null,
      |      "canceled_at": null,
      |      "cancellation_reason": null,
      |      "capture_method": "automatic",
      |      "charges": {
      |        "object": "list",
      |        "data": [
      |          {
      |            "id": "ch_1FunA2KZrwLxsXzPdMyYXVTO",
      |            "object": "charge",
      |            "amount": 2000,
      |            "amount_refunded": 0,
      |            "application": null,
      |            "application_fee": null,
      |            "application_fee_amount": null,
      |            "balance_transaction": "txn_1FunA3KZrwLxsXzP5vydhlfU",
      |            "billing_details": {
      |              "address": {
      |                "city": null,
      |                "country": null,
      |                "line1": null,
      |                "line2": null,
      |                "postal_code": null,
      |                "state": null
      |              },
      |              "email": null,
      |              "name": null,
      |              "phone": null
      |            },
      |            "captured": true,
      |            "created": 1577569766,
      |            "currency": "usd",
      |            "customer": null,
      |            "description": "(created by Stripe CLI)",
      |            "destination": null,
      |            "dispute": null,
      |            "disputed": false,
      |            "failure_code": null,
      |            "failure_message": null,
      |            "fraud_details": {
      |            },
      |            "invoice": null,
      |            "livemode": false,
      |            "metadata": {
      |            },
      |            "on_behalf_of": null,
      |            "order": null,
      |            "outcome": {
      |              "network_status": "approved_by_network",
      |              "reason": null,
      |              "risk_level": "normal",
      |              "risk_score": 16,
      |              "seller_message": "Payment complete.",
      |              "type": "authorized"
      |            },
      |            "paid": true,
      |            "payment_intent": "pi_1FunA2KZrwLxsXzPVTRdS9lz",
      |            "payment_method": "pm_1FunA2KZrwLxsXzPBRDYF78M",
      |            "payment_method_details": {
      |              "card": {
      |                "brand": "visa",
      |                "checks": {
      |                  "address_line1_check": null,
      |                  "address_postal_code_check": null,
      |                  "cvc_check": null
      |                },
      |                "country": "US",
      |                "exp_month": 12,
      |                "exp_year": 2020,
      |                "fingerprint": "UjHgV6BrDB9joUHr",
      |                "funding": "credit",
      |                "installments": null,
      |                "last4": "4242",
      |                "network": "visa",
      |                "three_d_secure": null,
      |                "wallet": null
      |              },
      |              "type": "card"
      |            },
      |            "receipt_email": null,
      |            "receipt_number": null,
      |            "receipt_url": "https://pay.stripe.com/receipts/acct_1FmUMjKZrwLxsXzP/ch_1FunA2KZrwLxsXzPdMyYXVTO/rcpt_GRgXkaXq7Lvc4yZlzOcAQcETFC4LD0U",
      |            "refunded": false,
      |            "refunds": {
      |              "object": "list",
      |              "data": [
      |
      |              ],
      |              "has_more": false,
      |              "total_count": 0,
      |              "url": "/v1/charges/ch_1FunA2KZrwLxsXzPdMyYXVTO/refunds"
      |            },
      |            "review": null,
      |            "shipping": {
      |              "address": {
      |                "city": "San Francisco",
      |                "country": "US",
      |                "line1": "510 Townsend St",
      |                "line2": null,
      |                "postal_code": "94103",
      |                "state": "CA"
      |              },
      |              "carrier": null,
      |              "name": "Jenny Rosen",
      |              "phone": null,
      |              "tracking_number": null
      |            },
      |            "source": null,
      |            "source_transfer": null,
      |            "statement_descriptor": null,
      |            "statement_descriptor_suffix": null,
      |            "status": "succeeded",
      |            "transfer_data": null,
      |            "transfer_group": null
      |          }
      |        ],
      |        "has_more": false,
      |        "total_count": 1,
      |        "url": "/v1/charges?payment_intent=pi_1FunA2KZrwLxsXzPVTRdS9lz"
      |      },
      |      "client_secret": "pi_1FunA2KZrwLxsXzPVTRdS9lz_secret_ZzDd1paCaeQAqVw4DjJyw0yEr",
      |      "confirmation_method": "automatic",
      |      "created": 1577569766,
      |      "currency": "usd",
      |      "customer": null,
      |      "description": "(created by Stripe CLI)",
      |      "invoice": null,
      |      "last_payment_error": null,
      |      "livemode": false,
      |      "metadata": {
      |      },
      |      "next_action": null,
      |      "on_behalf_of": null,
      |      "payment_method": "pm_1FunA2KZrwLxsXzPBRDYF78M",
      |      "payment_method_options": {
      |        "card": {
      |          "installments": null,
      |          "request_three_d_secure": "automatic"
      |        }
      |      },
      |      "payment_method_types": [
      |        "card"
      |      ],
      |      "receipt_email": null,
      |      "review": null,
      |      "setup_future_usage": null,
      |      "shipping": {
      |        "address": {
      |          "city": "San Francisco",
      |          "country": "US",
      |          "line1": "510 Townsend St",
      |          "line2": null,
      |          "postal_code": "94103",
      |          "state": "CA"
      |        },
      |        "carrier": null,
      |        "name": "Jenny Rosen",
      |        "phone": null,
      |        "tracking_number": null
      |      },
      |      "source": null,
      |      "statement_descriptor": null,
      |      "statement_descriptor_suffix": null,
      |      "status": "succeeded",
      |      "transfer_data": null,
      |      "transfer_group": null
      |    }
      |  },
      |  "livemode": false,
      |  "pending_webhooks": 2,
      |  "request": {
      |    "id": "req_4WojcNl7163ktm",
      |    "idempotency_key": null
      |  },
      |  "type": "payment_intent.succeeded"
      |}
    """.stripMargin
}
