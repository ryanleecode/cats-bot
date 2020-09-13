package bot.state.lifecycle

import bot.state.WorldState
import bot.state.lifecycle.Action.{EndGame, UpdateWorldState}

object Reducer {
  def apply[A](state: Option[WorldState], action: Action): Option[WorldState] = {
    action match {
      case EndGame                                  => None
      case UpdateWorldState(worldState: WorldState) => Some(worldState)
      case _                                        => state
    }
  }
}
