package com.stripe.utils

import com.stripe.StripeCustomerId
import com.stripe.users.UserId
import io.chrisdavenport.fuuid.FUUID
import org.scalacheck.{Arbitrary, Gen}

object arbitraries {
  private def fuuidGen: Gen[FUUID] = Gen.uuid.map(FUUID.fromUUID(_))

  implicit val userIdArb: Arbitrary[UserId] = Arbitrary {
    fuuidGen.map(UserId(_))
  }

  implicit val custIdArb: Arbitrary[StripeCustomerId] = Arbitrary {
    fuuidGen.map(f => StripeCustomerId(f.show))
  }
}
