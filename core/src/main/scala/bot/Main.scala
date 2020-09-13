package bot

import bot.lifecycle.Listener
import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val listener = new Listener()

    listener.startGame(true).map(_ => ExitCode.Success)
  }
}
