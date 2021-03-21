package com.stripe.config

import cats.effect._
import cats.implicits._
import pureconfig._
import pureconfig.generic.auto._
import com.typesafe.config.ConfigFactory

object ConfigService {
  def getConfig[F[_]](implicit F: Sync[F]): F[ServiceConfig] = SetupConfig.loadConfig[F]
}

private[config] object SetupConfig {

  def loadConfig[F[_]](implicit F: Sync[F]): F[ServiceConfig] = for {
    classLoader <- Sync[F].delay(ConfigFactory.load(getClass().getClassLoader()))
    out <- Sync[F].delay(ConfigSource.fromConfig(classLoader).at("com.service").loadOrThrow[ServiceConfig])
  } yield out

}
