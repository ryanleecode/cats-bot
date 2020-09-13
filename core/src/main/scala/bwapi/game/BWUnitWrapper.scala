package bwapi.game

import bwapi.{TilePosition, UnitType}
import cats.effect.IO
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto.autoUnwrap

import scala.jdk.CollectionConverters.ListHasAsScala

final case class BWUnitWrapper(private[game] val receiver: bwapi.Unit) {
  def unitType(): UnitType = receiver.getType

  def tilePosition(): TilePosition = receiver.getTilePosition

  def getDistance(unit: BWUnitWrapper): Int = receiver.getDistance(unit.receiver)

  def canTrain(unit: UnitType): Boolean = receiver.canTrain(unit)

  def canTrain(unit: UnitType, checkCanIssueCommandType: Boolean): Boolean =
    receiver.canTrain(unit, checkCanIssueCommandType)

  def canTrain(unit: UnitType, checkCanIssueCommandType: Boolean, checkCommandibility: Boolean): Boolean =
    receiver.canTrain(unit, checkCanIssueCommandType, checkCommandibility)

  def trainingQueue(): List[UnitType] = receiver.getTrainingQueue.asScala.toList

  def train(unitType: UnitType): IO[Unit] = IO { receiver.train(unitType) }
}
