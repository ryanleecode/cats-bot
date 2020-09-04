import bwapi.{BWClient, Flag}
import cats.effect.Sync
import cats.implicits._
import io.chrisdavenport.log4cats.Logger

object Lifecycle {
  def onStart[F[_] : Sync : Logger]()(implicit lazyClient: F[BWClient]): F[Unit] =
    for {
      _ <- Logger[F].info("Game Started")
      client <- lazyClient
      _ <- Sync[F]
        .delay({
          val game = client.getGame
          game.enableFlag(Flag.UserInput)
        })
        .onError { case e => Logger[F].error(e)("Game Start Initialization Failed") }
    } yield Sync[F]

    def onEnd[F[_] : Sync : Logger]()(implicit client: BWClient): F[Unit] =
      for {
        _ <- Logger[F].info("Game Ended")
      } yield Sync[F]
}