package com.stripe

import cats.implicits._
import cats.effect.{Async, Blocker, ConcurrentEffect, ContextShift, ExitCode, Resource, Timer}
import com.stripe.StripeClient
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import org.http4s.HttpApp
import org.http4s.server.blaze.BlazeServerBuilder
import com.stripe.config.{ConfigService, PostgresConfig}
import com.stripe.http.RoutesApp
import com.stripe.checkout.Checkout
import com.stripe.webhook.WebhookSignature
import com.stripe.users.{PostgresUsersRepository, Users}
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext

object Server {

  def serve[F[_]](implicit F: ConcurrentEffect[F], CS: ContextShift[F], T: Timer[F]): Resource[F, ExitCode] = {
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    for {
      implicit0(l: SelfAwareStructuredLogger[F]) <- Resource.liftF(Slf4jLogger.create[F])
      c   <- BlazeClientBuilder[F](ec).resource
      cfg <- Resource.liftF(ConfigService.getConfig)
      trans <- buildTransactor[F](cfg.postgres)
      stripe = StripeClient.build(cfg.stripe, c)
      userRepo = PostgresUsersRepository.build(trans)
      usersSvc = Users.build[F](userRepo)
      checkout = Checkout.build[F](stripe)
      webhookSig = WebhookSignature.build(cfg.stripe)
      svc = RoutesApp.build[F](usersSvc, checkout, webhookSig).app
      server <- Resource.liftF(buildServer(ec)(svc))
    } yield server
  }

  private def buildServer[F[_]: ConcurrentEffect : Timer](ec: ExecutionContext)(services: HttpApp[F]): F[ExitCode] =
    BlazeServerBuilder[F](ec)
      .bindHttp(8080, "localhost")
      .withHttpApp(services)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)

  private def buildTransactor[F[_]: ContextShift : Async](postgresConfig: PostgresConfig): Resource[F, Transactor[F]] = {
    for {
      ce             <- ExecutionContexts.fixedThreadPool[F](10)
      ec             <- ExecutionContexts.cachedThreadPool
      transactor     <-
        HikariTransactor.newHikariTransactor[F](
          "org.postgresql.Driver",
          postgresConfig.jdbcUrl,
          postgresConfig.username,
          postgresConfig.password,
          ce,
          Blocker.liftExecutionContext(ec))
    } yield transactor
  }

}
