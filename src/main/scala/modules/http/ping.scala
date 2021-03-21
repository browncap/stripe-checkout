package com.stripe.http

import cats.implicits._
import cats.effect.Sync
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import io.chrisdavenport.log4cats.Logger

object Ping {
  def route[F[_]](implicit F: Sync[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root / "ping" =>
        Ok("pong")
    }
  }
}
