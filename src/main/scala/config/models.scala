package com.stripe.config

import scala.concurrent.duration.FiniteDuration

final case class ServiceConfig(
  http: HttpConfig,
  postgres: PostgresConfig,
  stripe: StripeConfig
)

final case class HttpConfig(
  port: Int,
  host: String
)

case class PostgresConfig(
  jdbcUrl: String,
  username: String,
  password: String
)

case class StripeConfig(
  secretKey: String,
  signingSecret: String,
  tolerance: FiniteDuration
)
