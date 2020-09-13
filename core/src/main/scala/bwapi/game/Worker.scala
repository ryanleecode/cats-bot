package bwapi.game

import bwapi.TilePosition
import cats.effect.IO
import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.refineV
import eu.timepit.refined.auto.autoUnwrap
import cats.implicits._

final case class Worker private ()

object Worker {
  def gather(resource: Refined[BWUnitWrapper, Resource])(worker: Refined[BWUnitWrapper, Worker]): IO[Unit] =
    IO {
      worker.receiver.gather(resource.receiver)
    }

  def build(buildingType: Refined[bwapi.UnitType, Building], location: TilePosition)(
    worker: Refined[BWUnitWrapper, Worker]
  ): IO[Unit] =
    IO {
      worker.receiver.build(buildingType, location)
    }

  implicit def workerPredicate: Validate.Plain[BWUnitWrapper, Worker] =
    Validate
      .fromPredicate[BWUnitWrapper, Worker](
        unit => unit.receiver.getType.isWorker,
        unit => s"($unit is not a worker)",
        Worker()
      )
}

object Workers {
  def apply[U](list: List[U])(implicit validate: Validate.Plain[U, Worker]): List[Refined[U, Worker]] =
    list.mapFilter[Refined[U, Worker]]({ unit => refineV[Worker](unit).toOption })

}
