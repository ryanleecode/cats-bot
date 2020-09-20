package bwapi.game

import bwapi.{Position, TilePosition, UnitType}
import cats.effect.IO

import scala.jdk.CollectionConverters.ListHasAsScala

final case class BWUnitWrapper(private[game] val receiver: bwapi.Unit) {
  def unitType: UnitType = receiver.getType

  def tilePosition: TilePosition = receiver.getTilePosition

  def position: Position = receiver.getPosition

  def isIdle: Boolean = receiver.isIdle

  def getDistance(unit: BWUnitWrapper): Int = receiver.getDistance(unit.receiver)

  def canTrain(unit: UnitType): Boolean = receiver.canTrain(unit)

  def canTrain(unit: UnitType, checkCanIssueCommandType: Boolean): Boolean =
    receiver.canTrain(unit, checkCanIssueCommandType)

  def canTrain(unit: UnitType, checkCanIssueCommandType: Boolean, checkCommandibility: Boolean): Boolean =
    receiver.canTrain(unit, checkCanIssueCommandType, checkCommandibility)

  def trainingQueue(): List[UnitType] = receiver.getTrainingQueue.asScala.toList

  def train(unitType: UnitType): IO[Unit] = IO { receiver.train(unitType) }

  def gather(target: BWUnitWrapper): IO[Unit] = IO { receiver.gather(target.receiver) }

  def gather(target: BWUnitWrapper, shiftQueueCommand: Boolean): IO[Unit] =
    IO { receiver.gather(target.receiver, shiftQueueCommand) }

  def returnCargo(): IO[Unit] = IO { receiver.returnCargo() }

  def returnCargo(shiftQueueCommand: Boolean): IO[Unit] = IO { receiver.returnCargo(shiftQueueCommand) }
}
