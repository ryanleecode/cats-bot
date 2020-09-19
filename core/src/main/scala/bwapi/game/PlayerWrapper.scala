package bwapi.game

import bwapi.{Race, TilePosition, UnitType}

import collection.JavaConverters._

final case class PlayerWrapper(receiver: bwapi.Player) {
  def startLocation(): TilePosition = receiver.getStartLocation

  def units(): List[BWUnitWrapper] =
    receiver.getUnits.asScala.map(BWUnitWrapper).toList

  def race(): Race = receiver.getRace

  def supplyTotal(): Int = receiver.supplyTotal()

  def supplyUsed(): Int = receiver.supplyUsed()

  def completedUnitCount(unitType: UnitType): Int =
    receiver.completedUnitCount(unitType)

  def deadUnitCount(unitType: UnitType): Int = receiver.deadUnitCount(unitType)

  def minerals(): Int = receiver.minerals()
}
