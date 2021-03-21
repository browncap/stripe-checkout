package com.stripe.webhook

import cats.implicits._
import cats.effect.Sync
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import com.stripe.users.Users
import io.chrisdavenport.log4cats.Logger
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.CirceEntityDecoder._

object WebhookRoutes {
  def build[F[_]](webhookSig: WebhookSignature[F], users: Users[F])(implicit F: Sync[F], L: Logger[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    HttpRoutes.of[F] {
      case req@ POST -> Root / "webhook" / "checkout" =>
        for {
          _       <- req.bodyText.compile.string.flatMap(s => webhookSig.verify(req.headers, s.trim))
          req     <- req.as[CheckoutCompleted]
          _       <- users.create(req.data.`object`.client_reference_id, req.data.`object`.customer)
          resp    <- Ok()
        } yield resp

    }
  }
}
