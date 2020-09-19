package bot.architecture.heuristics

import bwapi.TilePosition

final case class ClosestDistance[A](to: bwapi.Position, datapoints: List[A])

object ClosestDistance {
  def f[A](to: bwapi.Position, datapoints: List[A], fa: A => TilePosition) {}
}
