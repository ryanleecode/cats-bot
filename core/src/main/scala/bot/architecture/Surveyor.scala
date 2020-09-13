package bot.architecture

import bwapi.TilePosition
import eu.timepit.refined.api.Refined
import bwapi.game.{Building}

object Surveyor {
  def findPlacement[G, P, U](area: bwem.Area, unitType: Refined[bwapi.UnitType, Building])(implicit
    game: bwapi.game.GameWrapper
  ): Iterable[TilePosition] = {
    val topLeft         = area.getTopLeft
    val boundingBoxSize = area.getBoundingBoxSize

    val tiles = for {
      i <- topLeft.x to topLeft.x + boundingBoxSize.x
      j <- topLeft.y to topLeft.y + boundingBoxSize.y
    } yield new TilePosition(i, j)

    val buildableTiles = tiles.filter({ tilePosition =>
      game.canBuildHere(tilePosition, unitType.value)
    })

    buildableTiles
  }
}
