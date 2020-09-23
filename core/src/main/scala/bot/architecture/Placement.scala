package bot.architecture

import bwapi.{Position, TilePosition, UnitFilter, UnitType}
import bwapi.game.GameWrapper
import bwem.{Area, BWMap}

import scala.jdk.CollectionConverters.ListHasAsScala

case class Feature(candidateFn: TilePosition => Double, amplifier: Double, weight: Int)

object Placement {
  val mediumBuildingSize = new TilePosition(3, 2)
  val largeBuildingSize  = new TilePosition(4, 3)

  def distanceToNearestMineralField(topLeftAnchor: TilePosition, tileSize: TilePosition)(implicit
    game: GameWrapper
  ): Double = {
    val origin = getOrigin(topLeftAnchor, tileSize)

    game
      .getClosestUnit(origin, UnitFilter.IsMineralField)
      .map({ closestMineralField => origin.getDistance(closestMineralField.position) })
      .getOrElse(Double.NaN)
  }

  def distanceToEdgeOfMap(topLeftAnchor: TilePosition, tileSize: TilePosition)(implicit game: GameWrapper): Double = {
    val origin = getOrigin(topLeftAnchor, tileSize).toTilePosition

    val leftDistance   = origin.getDistance(new TilePosition(0, origin.y))
    val upDistance     = origin.getDistance(new TilePosition(origin.x, 0))
    val rightDistance  = origin.getDistance(new TilePosition(game.mapWidth, origin.y))
    val bottomDistance = origin.getDistance(new TilePosition(origin.x, game.mapHeight))

    Array(leftDistance, upDistance, rightDistance, bottomDistance).min
  }

  def mediumBuildingsInRectangleCount(topLeftAnchor: TilePosition, tileSize: TilePosition)(implicit
    game: GameWrapper
  ): Int = {
    game
      .getUnitsInRectangle(
        topLeftAnchor.subtract(new TilePosition(1, 1)).toPosition,
        topLeftAnchor.add(tileSize.add(new TilePosition(1, 1))).toPosition
      )
      .count({ unit => unit.unitType.isBuilding && unit.unitType.tileSize() == mediumBuildingSize })
  }

  def distanceToClosestLargeBuilding(topLeftAnchor: TilePosition, tileSize: TilePosition)(implicit
    game: GameWrapper
  ): Double = {
    val origin = getOrigin(topLeftAnchor, tileSize)

    game
      .getClosestUnit(origin, (t: bwapi.Unit) => t.getType.tileSize() == largeBuildingSize)
      .map({ closestLargeBuilding => origin.getDistance(closestLargeBuilding.position) })
      .getOrElse(Double.NaN)
  }

  def distanceToClosestChokepoint(topLeftAnchor: TilePosition, tileSize: TilePosition, area: Area): Double = {
    val origin = getOrigin(topLeftAnchor, tileSize)

    area.getChokePoints.asScala
      .map({ chokePoint => chokePoint.getCenter.getDistance(origin.toWalkPosition) })
      .min
  }

  private def getOrigin(topLeftAnchor: TilePosition, tileSize: TilePosition): Position = {
    topLeftAnchor.toPosition.add(tileSize.toPosition.divide(2))
  }
}
