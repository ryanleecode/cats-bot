package bot.state.lifecycle

import bot.state.WorldState

sealed trait Action

final case object Action {
  // --------------------------------------------------------------------------
  // Lifecycle
  // --------------------------------------------------------------------------
  final case class UpdateWorldState(nextState: WorldState) extends Action
  final case object EndGame                                extends Action
}
