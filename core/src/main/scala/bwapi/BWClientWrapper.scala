package bwapi

import bwapi.game.GameWrapper
import cats.effect.IO

case class BWClientWrapper(eventListener: BWEventListener) {
  private val bwClient = new BWClient(eventListener)

  def game(): Option[GameWrapper] = Option(bwClient.getGame).map(GameWrapper)

  def startGame(): IO[scala.Unit] = IO(bwClient.startGame())

  def startGame(autoContinue: Boolean): IO[scala.Unit] =
    IO(bwClient.startGame(autoContinue))
}
