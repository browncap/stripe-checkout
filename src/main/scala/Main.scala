package com.stripe

import cats.effect._

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    Server.serve[IO].use(IO(_))
}
