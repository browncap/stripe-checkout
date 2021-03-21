package com.stripe.users

import cats.effect.{ContextShift, IO}
import doobie.util.transactor.Transactor
import io.chrisdavenport.fuuid.FUUID
import com.stripe.config.PostgresConfig
import org.scalatest._

import com.stripe.Migrator
import com.stripe.StripeCustomerId

class UserRepositorySpec extends FlatSpec with Matchers with BeforeAndAfterAll  {

  implicit val cs: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.Implicits.global)

  private[this] val jdbcUrl = "jdbc:postgresql://localhost:5432/store"
  private[this] val pgUsername = "postgres"
  private[this] val pgPassword = "password"

  lazy val postgresConfig = PostgresConfig(jdbcUrl, pgUsername, pgPassword)
  lazy val y = transactor.yolo
  lazy val transactor: Transactor[IO] =
    Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",
      jdbcUrl,
      pgUsername,
      pgPassword
    )

  override def beforeAll: Unit = {
    Migrator.migrate[IO](postgresConfig).unsafeRunSync()
  }

  override def afterAll: Unit = {}

  def doobieRepo = PostgresUsersRepository.build[IO](transactor)

  val randomUserId = UserId(FUUID.randomFUUID[IO].unsafeRunSync())
  val randomCustId = StripeCustomerId(FUUID.randomFUUID[IO].unsafeRunSync().show)

  "User repo" should "insert a user and successfully retrieve it w/ a username" in {
    val userId = randomUserId
    val custId = randomCustId

    doobieRepo.createUser(userId, custId).unsafeRunSync()
    doobieRepo.getUserWithUserId(userId).unsafeRunSync() should be(Option(userId))
  }

}
