package bot.geography

import java.util.Comparator

import bwapi.TilePosition
import bwem.Tile
import bwem.util.CheckMode

import scala.collection.mutable

object Geography {
  private val dir4 =
    Array(new TilePosition(0, -1), new TilePosition(-1, 0), new TilePosition(1, 0), new TilePosition(0, 1))

  def bfs(start: TilePosition, find: (Tile, TilePosition) => Boolean, visit: (Tile, TilePosition) => Boolean)(implicit
    map: bwem.BWMap
  ): Option[TilePosition] = {
    if (find(map.getTile(start), start)) {
      return Some(start)
    }

    val visited = new mutable.HashSet[TilePosition]()
    val toVisit = new mutable.ArrayDeque[TilePosition]

    visited.add(start)
    toVisit.append(start)

    while (!toVisit.isEmpty) {
      val current = toVisit.removeLast()
      dir4.foreach({ delta =>
        val next = current.add(delta)
        if (map.getData.getMapData.isValid(next)) {
          val nextTile = map.getData.getTile(next, CheckMode.NO_CHECK)
          if (find(nextTile, next)) {
            return Some(next)
          }
          if (visit(nextTile, next) && !visited.contains(next)) {
            toVisit.prepend(next)
            visited.add(next)
          }
        }
      })
    }

    None
  }
}
