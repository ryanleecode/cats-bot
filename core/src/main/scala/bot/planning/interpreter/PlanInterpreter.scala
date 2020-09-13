package bot.planning.interpreter

import bot.planning.PrimitiveTask
import bot.state.WorldState
import cats.effect.IO
import monocle.macros.GenLens
trait PlanInterpreter {
  def interpret(state: WorldState, task: PrimitiveTask): IO[WorldState]
}

object PlanInterpreter extends PlanInterpreter {
  def interpret(state: WorldState, task: PrimitiveTask): IO[WorldState] = {
    task match {
      case PrimitiveTask.BuildUnit(unitType) =>
        val self = state.game.self
        if (self.minerals() >= unitType.mineralPrice()) {
          val trainer = self.units.find { unit =>
            unit.canTrain(unitType, checkCanIssueCommandType = true) && unit.trainingQueue.length < 2
          }
          trainer.map(_.train(unitType)).map(_.map(_ => state)).fold(IO(state))(identity)
        } else {
          IO.pure(state)
        }
      case _ => IO(state)
    }
  }
}

trait PlanSimulator {
  def simulate(state: WorldState, task: PrimitiveTask): WorldState
}

object PlanSimulator extends PlanSimulator {
  def simulate(state: WorldState, task: PrimitiveTask): WorldState = {
    task match {
      case PrimitiveTask.BuildUnit(unitType) =>
        GenLens[WorldState](_.mineralOffset)
          .set(state.mineralOffset + unitType.mineralPrice())(state)
      case _ => state
    }

  }
}
