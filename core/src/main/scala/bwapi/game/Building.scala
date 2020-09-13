package bwapi.game

import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.refineV

final case class Building private ()

object Building {
  def unitType(building: Refined[BWUnitWrapper, Building]): Refined[bwapi.UnitType, Building] =
    refineV[Building](building.unitType).toOption.get

  implicit val unitTypePredicate: Validate.Plain[bwapi.UnitType, Building] =
    Validate.fromPredicate[bwapi.UnitType, Building](
      unitType => unitType.isBuilding,
      unit => s"($unit is not a building)",
      Building()
    )
}
