package com.stripe.users

import cats.implicits._
import cats.effect.{Bracket, Sync}
import com.stripe.StripeCustomerId
import doobie.free.connection.ConnectionIO
import doobie.util.query.Query0
import doobie.util.transactor.Transactor
import doobie.postgres.sqlstate
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.postgres._
import io.chrisdavenport.fuuid.doobie.implicits._

trait UsersRepository[F[_]]{
  def createUser(userId: UserId, customerId: StripeCustomerId): F[Unit]
  def getUserWithUserId(userId: UserId): F[Option[UserId]]
}

object PostgresUsersRepository {
  def build[F[_]](transactor: Transactor[F])(implicit F: Sync[F], ev: Bracket[F, Throwable]): UsersRepository[F] = new UsersRepository[F] {
    import Queries._

    def createUser(userId: UserId, customerId: StripeCustomerId): F[Unit] = {
      safeInsertUser(userId, customerId).transact(transactor).flatMap(F.fromEither(_))
    }

    def getUserWithUserId(userId: UserId): F[Option[UserId]] = {
      selectUserWithUserId(userId).option.transact(transactor)
    }
  }
}

object Queries {

  def safeInsertUser(userId: UserId, customerId: StripeCustomerId): ConnectionIO[Either[UserIdAlreadyExists, Unit]] =
    sql"INSERT INTO users values (${userId.value}, ${customerId.value})".update.run.void.attemptSomeSqlState {
      case sqlstate.class23.UNIQUE_VIOLATION => UserIdAlreadyExists(userId)
    }

  def selectUserWithUserId(userId: UserId): Query0[UserId] =
    sql"SELECT user_id from users where user_id = ${userId.value}".query[UserId]

}
