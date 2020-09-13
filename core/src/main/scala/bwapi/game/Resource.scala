package bwapi.game

sealed trait Resource

final case class MineralField private () extends Resource

final case class GasGeyser private () extends Resource
