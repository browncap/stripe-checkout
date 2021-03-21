package com.stripe

import cats.implicits._
import cats.effect.Sync
import org.flywaydb.core.Flyway
import com.stripe.config.PostgresConfig

object Migrator {
  def migrate[F[_]: Sync](postgres: PostgresConfig): F[Unit] =
    Sync[F].delay {
      val flyway = Flyway.configure
        .locations("migrations")
        .baselineVersion("0")
        .schemas("public")
        .validateOnMigrate(true)
        .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
        .load

      flyway.clean()
      flyway.repair()
      flyway.baseline()
      flyway.migrate()
    }.void
}
