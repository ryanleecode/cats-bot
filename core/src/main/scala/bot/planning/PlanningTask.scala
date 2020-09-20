package bot.planning

import bot.planning.interpreter.PlanSimulator
import bot.state.WorldState
import bwapi.UnitType
import bwapi.game._
import mouse.all._

sealed trait PlanningTask {
  val name: String
  val preconditions: List[WorldState => Boolean]
}

sealed trait CompoundTask extends PlanningTask {
  val subtasks: List[PlanningTask]
}

sealed trait PrimitiveTask extends PlanningTask {}

case object PrimitiveTask {
  final case class DoNothing() extends PrimitiveTask {
    override val name          = DoNothing.getClass.getSimpleName
    override val preconditions = List()
  }

  final case class BuildUnit(unitType: UnitType) extends PrimitiveTask {
    override val name = BuildUnit.getClass.getSimpleName
    override val preconditions = List({ worldState =>
      worldState.game.self.minerals >= unitType.mineralPrice()
    })
  }
}
case object CompoundTask {
  final case class BuildUnits(unitType: UnitType, limit: Int = 1) extends CompoundTask {
    override val name = BuildUnits.getClass.getSimpleName
    override val preconditions = List({ worldState =>
      val playerUnits = worldState.game.self.units()
      val count = playerUnits.count({ unit =>
        unit.unitType == unitType
      })

      count < limit
    })
    override val subtasks = List(PrimitiveTask.BuildUnit(unitType))
  }

  final case class BuildUnitsWithCompletion(unitType: UnitType, limit: Int = 1) extends CompoundTask {
    override val name: String = BuildUnitsWithCompletion.getClass.getSimpleName
    override val preconditions = List({ worldState =>
      val self = worldState.game.self

      self.completedUnitCount(unitType) + self.deadUnitCount(unitType) < limit
    })
    override val subtasks = List(BuildUnits(unitType, limit))
  }

  final case class TvPBuildOrder() extends CompoundTask {
    override val name = TvPBuildOrder.getClass.getSimpleName
    override val preconditions = List({ worldState =>
      worldState.game.enemy.race == bwapi.Race.Protoss
    })
    override val subtasks = List(
      BuildUnitsWithCompletion(UnitType.Terran_SCV, 9),
      BuildUnits(UnitType.Terran_Supply_Depot),
      BuildUnitsWithCompletion(UnitType.Terran_SCV, 200)
    )
  }

  final case class TvZBuildOrder() extends CompoundTask {
    override val name = TvZBuildOrder.getClass.getSimpleName
    override val preconditions = List({ worldState =>
      worldState.game.enemy.race == bwapi.Race.Zerg
    })
    override val subtasks = List(
      BuildUnitsWithCompletion(UnitType.Terran_SCV, 9),
      BuildUnits(UnitType.Terran_Supply_Depot),
      BuildUnitsWithCompletion(UnitType.Terran_SCV, 200)
    )
  }

}
