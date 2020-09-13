package bot.state

import bwapi.UnitType
import bwapi.game._

final case class WorldState(mineralOffset: Int, game: GameWrapper) {}

object WorldState {
  def initialState(game: GameWrapper): WorldState = WorldState(0, game)

  /* worldState.g*/
  /*  def race[G, P, U](worldState: WorldState[G, P, U]): bwapi.Race =
    worldState.g.map() Player.race(Game.GameOps(worldState.g).self(worldState.game))*/

  /*  def race[G, P, U](worldState: WorldState)(implicit g: G, game: Game[G, P, U], player: Player[P, U]): bwapi.Race =
    Player.race(Game.GameOps(g).self)

  def completedUnitCount(unitType: UnitType)(worldState: WorldState): Int =
    worldState.game
      .map({ game => game.self.completedUnitCount(unitType) })
      .fold(0)(identity)

  def deadUnitCount(unitType: UnitType)(worldState: WorldState) =
    worldState.game
      .map({ game => game.self.deadUnitCount(unitType) })
      .fold(0)(identity)

  def enemyRace(worldState: WorldState): bwapi.Race =
    worldState.game
      .map({ game => game.enemy.getRace })
      .fold(bwapi.Race.Unknown)(identity)

  def minerals(worldState: WorldState): Int =
    worldState.game
      .map({ game => game.self.minerals() })
      .fold(0)(identity) - worldState.mineralOffset

  def ownedUnits(worldState: WorldState): List[bwapi.Unit] =
    worldState.game
      .map({ game => game.self.getRace })
      .fold(List[bwapi.Unit]())(identity)*/
}
