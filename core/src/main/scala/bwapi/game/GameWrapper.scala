package bwapi.game

import bwapi.{Color, CoordinateType, Flag, Position, Text, TilePosition, UnitFilter, UnitType}
import cats.effect.IO
import eu.timepit.refined.api.Refined

import collection.JavaConverters._

final case class GameWrapper(receiver: bwapi.Game) {
  def self: PlayerWrapper = PlayerWrapper(receiver.self())

  def enemy: PlayerWrapper = PlayerWrapper(receiver.enemy())

  def mapWidth: Int = receiver.mapWidth

  def mapHeight: Int = receiver.mapHeight

  def enableFlag(flag: Flag): IO[Unit] = IO { receiver.enableFlag(flag) }

  def setLocalSpeed(speed: Int): IO[Unit] = IO { receiver.setLocalSpeed(speed) }

  def setFrameSkip(frameSkip: Int): IO[Unit] = IO { receiver.setFrameSkip(frameSkip) }

  def getFrameCount: Int = receiver.getFrameCount

  def getFPS: Int = receiver.getFPS

  def getAverageFPS: Double = receiver.getAverageFPS

  def allUnits(): List[bwapi.Unit] = receiver.getAllUnits.asScala.toList

  def isBuildable(tileX: Int, tileY: Int): Boolean = receiver.isBuildable(tileX, tileY)

  def isBuildable(tileX: Int, tileY: Int, includeBuildings: Boolean): Boolean =
    receiver.isBuildable(tileX, tileY, includeBuildings)

  def isBuildable(position: TilePosition): Boolean = receiver.isBuildable(position)

  def isBuildable(position: TilePosition, includeBuildings: Boolean): Boolean =
    receiver.isBuildable(position, includeBuildings)

  def canBuildHere(tilePosition: TilePosition, unitType: UnitType): Boolean =
    receiver.canBuildHere(tilePosition, unitType)

  def getMinerals: List[bwapi.Unit] = receiver.getMinerals.asScala.toList

  def getUnitsInRectangle(left: Int, top: Int, right: Int, bottom: Int): List[BWUnitWrapper] =
    receiver.getUnitsInRectangle(left, top, right, bottom).asScala.map(BWUnitWrapper).toList

  def getUnitsInRectangle(left: Int, top: Int, right: Int, bottom: Int, pred: UnitFilter): List[BWUnitWrapper] =
    receiver.getUnitsInRectangle(left, top, right, bottom, pred).asScala.map(BWUnitWrapper).toList

  def getUnitsInRectangle(leftTop: Position, rightBottom: Position): List[BWUnitWrapper] =
    receiver.getUnitsInRectangle(leftTop, rightBottom).asScala.map(BWUnitWrapper).toList

  def getUnitsInRectangle(leftTop: Position, rightBottom: Position, pred: UnitFilter): List[BWUnitWrapper] =
    receiver.getUnitsInRectangle(leftTop, rightBottom, pred).asScala.map(BWUnitWrapper).toList

  def getUnitsInRadius(x: Int, y: Int, radius: Int): List[BWUnitWrapper] =
    receiver.getUnitsInRadius(x, y, radius).asScala.map(BWUnitWrapper).toList

  def getUnitsInRadius(x: Int, y: Int, radius: Int, pred: UnitFilter): List[BWUnitWrapper] =
    receiver.getUnitsInRadius(x, y, radius, pred).asScala.map(BWUnitWrapper).toList

  def getUnitsInRadius(center: Position, radius: Int): List[BWUnitWrapper] =
    receiver.getUnitsInRadius(center, radius).asScala.map(BWUnitWrapper).toList

  def getUnitsInRadius(center: Position, radius: Int, pred: UnitFilter): List[BWUnitWrapper] =
    receiver.getUnitsInRadius(center, radius, pred).asScala.map(BWUnitWrapper).toList

  def getClosestUnit(center: Position): Option[BWUnitWrapper] =
    Option(receiver.getClosestUnit(center)).map(BWUnitWrapper)

  def getClosestUnit(center: Position, pred: UnitFilter): Option[BWUnitWrapper] =
    Option(receiver.getClosestUnit(center, pred)).map(BWUnitWrapper)

  def getClosestUnit(center: Position, radius: Int): Option[BWUnitWrapper] =
    Option(receiver.getClosestUnit(center, radius)).map(BWUnitWrapper)

  def getBuildLocation(buildingType: Refined[bwapi.UnitType, Building], desiredLocation: TilePosition) =
    receiver.getBuildLocation(buildingType.value, desiredLocation)

  def drawTextScreen(x: Int, y: Int, string: String, colors: Text*): IO[Unit] =
    IO {
      receiver.drawTextScreen(x, y, string, colors: _*)
    }

  def drawTextScreen(p: Position, string: String, colors: Text*): IO[Unit] =
    IO {
      receiver.drawTextScreen(p, string, colors: _*)
    }

  def drawCircleMap(x: Int, y: Int, radius: Int, color: Color): IO[Unit] =
    IO {
      receiver.drawCircleMap(x, y, radius, color)
    }

  def drawCircleMap(x: Int, y: Int, radius: Int, color: Color, isSolid: Boolean): IO[Unit] =
    IO {
      receiver.drawCircleMap(x, y, radius, color, isSolid)
    }

  def drawCircleMap(p: Position, radius: Int, color: Color): IO[Unit] =
    IO {
      receiver.drawCircleMap(p: Position, radius, color)
    }

  def drawCircleMap(p: Position, radius: Int, color: Color, isSolid: Boolean): IO[Unit] =
    IO {
      receiver.drawCircleMap(p: Position, radius, color, isSolid)
    }

  def drawBox(ctype: CoordinateType, left: Int, top: Int, right: Int, bottom: Int, color: Color): IO[Unit] =
    IO {
      receiver.drawBox(ctype, left, top, right, bottom, color)
    }

  def drawBox(
    ctype: CoordinateType,
    left: Int,
    top: Int,
    right: Int,
    bottom: Int,
    color: Color,
    isSolid: Boolean
  ): IO[Unit] =
    IO {
      receiver.drawBox(ctype, left, top, right, bottom, color, isSolid)
    }

  def drawBoxMap(left: Int, top: Int, right: Int, bottom: Int, color: Color): IO[Unit] =
    IO {
      receiver.drawBoxMap(left, top, right, bottom, color)
    }

  def drawBoxMap(left: Int, top: Int, right: Int, bottom: Int, color: Color, isSolid: Boolean): IO[Unit] =
    IO {
      receiver.drawBoxMap(left, top, right, bottom, color, isSolid)
    }

  def drawBoxMap(leftTop: Position, rightBottom: Position, color: Color): IO[Unit] =
    IO {
      receiver.drawBoxMap(leftTop, rightBottom, color)
    }

  def drawBoxMap(leftTop: Position, rightBottom: Position, color: Color, isSolid: Boolean): IO[Unit] =
    IO {
      receiver.drawBoxMap(leftTop, rightBottom, color, isSolid)
    }

  def getUnitsOnTile(tile: TilePosition): List[BWUnitWrapper] =
    receiver.getUnitsOnTile(tile).asScala.map(BWUnitWrapper).toList

  def getUnitsOnTile(tileX: Int, tileY: Int): List[BWUnitWrapper] =
    receiver.getUnitsOnTile(tileX, tileY).asScala.map(BWUnitWrapper).toList

  def getUnitsOnTile(tileX: Int, tileY: Int, pred: UnitFilter): List[BWUnitWrapper] =
    receiver.getUnitsOnTile(tileX, tileY, pred).asScala.map(BWUnitWrapper).toList
}
