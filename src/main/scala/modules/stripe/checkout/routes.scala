package com.stripe.checkout

import cats.syntax.all._
import cats.effect.Sync
import com.stripe.users.UserId
import io.chrisdavenport.fuuid.FUUID
import io.chrisdavenport.log4cats.Logger
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.CirceEntityDecoder._

object CheckoutRoutes {
  def build[F[_]](checkout: Checkout[F])(implicit F: Sync[F], L: Logger[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root / "checkout" / "session" =>
        for {
          uid  <- FUUID.randomFUUID
          cid  <- checkout.session(UserId(uid))
          resp <- Ok(cid.value)
        } yield resp

    }
  }
}
