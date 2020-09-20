package bot.planning

import bot.planning.CompoundTask.{TvPBuildOrder, TvZBuildOrder}
import bot.planning.PrimitiveTask.DoNothing
import bot.planning.interpreter.PlanSimulator
import bot.state.WorldState
import bwapi.game.PlayerWrapper
import cats.data.NonEmptyList

object Plan {
  def apply(worldState: WorldState, simulator: PlanSimulator): NonEmptyList[PrimitiveTask] = {
    implicit val _simulator = simulator
    val planTree            = TvZBuildOrder()

    NonEmptyList
      .fromList(handlePlanningTask(worldState, planTree)._2)
      .fold(NonEmptyList.one[PrimitiveTask](DoNothing()))(identity)
  }

  private def handlePlanningTask(worldState: WorldState, planningTask: PlanningTask)(implicit
    simulator: PlanSimulator
  ): (WorldState, List[PrimitiveTask]) = {
    val preconditionsSuccessful =
      planningTask.preconditions.forall(preCond => preCond(worldState))

    if (!preconditionsSuccessful) {
      (worldState, List())
    } else {
      planningTask match {
        case compoundTask: CompoundTask =>
          handleCompoundTask(worldState, compoundTask)
        case primitiveTask: PrimitiveTask =>
          handlePrimitiveTask(worldState, primitiveTask)
      }
    }

  }

  private def handleCompoundTask(worldState: WorldState, compoundTask: CompoundTask)(implicit
    simulator: PlanSimulator
  ): (WorldState, List[PrimitiveTask]) = {
    compoundTask.subtasks.foldLeft((worldState, List[PrimitiveTask]()))({ (prev, task) =>
      val (prevWorldState, tasks) = prev
      val (nextWorldState, additionalTasks) =
        handlePlanningTask(prevWorldState, task)

      (nextWorldState, List.concat(tasks, additionalTasks))
    })

  }

  private def handlePrimitiveTask(worldState: WorldState, primitiveTask: PrimitiveTask)(implicit
    simulator: PlanSimulator
  ): (WorldState, List[PrimitiveTask]) = {
    val nextState = simulator.simulate(worldState, primitiveTask)

    (nextState, List(primitiveTask))
  }
}
