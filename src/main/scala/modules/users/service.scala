package com.stripe.users

import cats.effect.Sync
import io.chrisdavenport.fuuid.FUUID
import cats.implicits._
import com.stripe.StripeCustomerId

trait Users[F[_]] {
  def create(userId: UserId, custId: StripeCustomerId): F[UserId]
}

object Users {
  def build[F[_]](repo: UsersRepository[F])(implicit F: Sync[F]): Users[F] = {
    new Users[F] {

      override def create(userId: UserId, custId: StripeCustomerId): F[UserId] = for {
        userId <- FUUID.randomFUUID
        _      <- repo.createUser(UserId(userId), custId)
      } yield UserId(userId)
    }
  }
}
