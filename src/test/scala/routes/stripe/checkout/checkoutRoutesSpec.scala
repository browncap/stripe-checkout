package com.stripe.checkout

import cats.effect.IO
import com.stripe.MockCheckout
import com.stripe.checkout.{CheckoutRoutes, CheckoutSessionId}
import com.stripe.users.UserId
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.Json
import org.http4s.{Header, Method, Request, Uri}
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.http4s.implicits._
import io.circe.syntax._
import io.circe.Json
import org.http4s.circe.CirceEntityDecoder._
import com.stripe.utils.arbitraries._

class CheckoutRoutesSpec extends WordSpec with Matchers with ScalaCheckPropertyChecks {

  "return a CheckoutSessionId for /checkout/session" in {
    forAll { (checkoutId: String, userId: UserId) =>

      val result = Json.fromString(checkoutId)

      val mockCheckout = new MockCheckout {
        override def session(userId: UserId): IO[CheckoutSessionId] = IO.pure(CheckoutSessionId(checkoutId))
      }

      implicit val logger = Slf4jLogger.create[IO].unsafeRunSync()
      val routes = CheckoutRoutes.build(mockCheckout).orNotFound
      val req = Request[IO](method = Method.GET, uri = Uri.unsafeFromString("/checkout/session"))
      val routeResult = routes.run(req).unsafeRunSync

      routeResult.status.isSuccess should be(true)
      routeResult.as[Json].unsafeRunSync() should be(result)
    }
  }

}
