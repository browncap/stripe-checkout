package com.stripe.webhook

import java.time.Instant
import java.util.concurrent.TimeUnit

import cats.data.{Kleisli, OptionT}
import cats.effect.{Clock, Sync}
import cats.implicits._
import com.stripe.webhook.StripeSignature.{Signature, SignatureTimestamp}
import com.stripe.config.StripeConfig
import org.apache.commons.codec.binary.Hex
import org.http4s.{Headers, Request, Response}
import org.http4s.server.Middleware
import org.http4s.util.CaseInsensitiveString
import tsec.mac.MAC
import tsec.mac.jca.HMACSHA256
import cats.tagless.Derive

trait WebhookSignature[F[_]] {
  def verify(headers: Headers, body: String): F[Unit]
}

object WebhookSignature {
  def build[F[_]](stripeConfig: StripeConfig)(implicit F: Sync[F], C: Clock[F]): WebhookSignature[F] = {
    new WebhookSignature[F] {

      implicit val functorK = Derive.functorK[WebhookSignature]
      implicit val instrument = Derive.instrument[WebhookSignature]

      def verify(headers: Headers, body: String): F[Unit] = {
        for {
          headerSig     <- F.fromOption(headers.get(CaseInsensitiveString("Stripe-Signature")), SignatureNotFound)
          signature     <- F.fromOption(StripeSignature.pieces(headerSig.value), InvalidSignature)
          signedPayload =  s"${signature.ts.value.toEpochMilli}.$body"
          hmac256Sig    =  MAC.apply[HMACSHA256](Hex.decodeHex(signature.signature.value))
          key           <- HMACSHA256.buildKey[F](stripeConfig.signingSecret.getBytes)
          bool          <- HMACSHA256.verifyBool[F](signedPayload.getBytes, hmac256Sig, key)
          _             <- if (bool) F.unit else F.raiseError(SignatureMismatch)
          _             <-
            C.realTime(TimeUnit.SECONDS)
              .ensure(CurrentTimeOutsideTolerance) { now =>
                signature.ts.value.toEpochMilli > (now - stripeConfig.tolerance.toSeconds)
              }.void
        } yield ()
      }
    }
  }

  def middleware[F[_]: Sync : Clock](stripeConfig: StripeConfig): Middleware[OptionT[F, ?], Request[F], Response[F], Request[F], Response[F]] = {
    val webhookSignature: WebhookSignature[F] = build[F](stripeConfig)

    kleisli => Kleisli { req =>
      for {
        _        <- OptionT.liftF(req.bodyText.compile.string.map(s => webhookSignature.verify(req.headers, s.trim)))
        response <- kleisli(req)
      } yield response
    }
  }
}

case class StripeSignature(signature: Signature, ts: SignatureTimestamp)
object StripeSignature {
  //ex: Stripe-Signature: t=1577559210,v1=894487bcea14a9fc9d873a96b22351d6846940cb1f67376347f78a32f4b8cbab,v0=c2f34cbfab2b6dd329ab43e3ec4838e93164361c63fc019a4df69953628bd56c

  case class Signature(value: String)
  case class SignatureTimestamp(value: Instant)

  def pieces(s: String): Option[StripeSignature] = {
    val split: List[String] = s.split(',').toList

    for {
      ts <- split.find(_.startsWith("t")).flatMap(_.split("=").toList.lift(1))
      v1 <- split.find(_.startsWith("v1")).flatMap(_.split("=").toList.lift(1))
    } yield StripeSignature(Signature(v1), SignatureTimestamp(Instant.ofEpochMilli(ts.toLong)))
  }
}
