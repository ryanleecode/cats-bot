package bot.lifecycle

import bot.architecture.Surveyor
import bot.planning.Plan
import bot.planning.interpreter.{PlanInterpreter, PlanSimulator}
import bot.state.WorldState
import bot.state.lifecycle.{Action, Reducer}
import bwapi.{BWClientWrapper, Color, DefaultBWListener, Flag, Position, TilePosition, UnitFilter, UnitType}
import bwem.util.Pred
import bwem.{BWEM, BWMap, Tile}
import cats.data.NonEmptyList
import cats.effect.{IO, Sync}
import eu.timepit.refined.refineV
import bwapi.game.{Building, GameWrapper, Worker, Workers}
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import monocle.macros.GenLens
import org.apache.commons.math3.stat.descriptive.summary
import cats.implicits._
import com.spotify.featran.FeatureSpec
import com.spotify.featran.transformers.MinMaxScaler
import eu.timepit.refined.api.Refined
import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.distribution.RealDistribution
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import mouse.all._
import com.spotify.featran._
import com.spotify.featran.transformers._
import breeze.linalg._
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric._
import breeze.linalg._
import breeze.numerics.pow

import scala.jdk.CollectionConverters.ListHasAsScala

case class Heuristic1(distanceToMineralField: Option[Double], distanceToSupplyDepot: Option[Double])

final class Listener() extends DefaultBWListener {
  private def logger[F[_]: Sync]: SelfAwareStructuredLogger[F] =
    Slf4jLogger.getLoggerFromName("Bot")
  private val client = BWClientWrapper(this)

  private def getGame(): IO[GameWrapper] = {
    IO.fromOption(client.game())(new RuntimeException("game is not set"))
  }

  private var bwmap: Option[BWMap] = None

  private def bwmapIO(): IO[BWMap] = {
    IO.fromOption(bwmap)(new RuntimeException("bwmap is null"))
  }

  private var worldState: Option[WorldState] = None

  private implicit def dispatch(action: Action): IO[Unit] =
    IO { worldState = Reducer(worldState, action) }

  def startGame(autoContinue: Boolean): IO[Unit] =
    client.startGame(autoContinue)

  override def onStart(): Unit = {
    val task = for {
      _    <- logger[IO].info("Game Started")
      game <- getGame
      _ <- IO {
        game.enableFlag(Flag.UserInput).unsafeRunSync()
        game.setLocalSpeed(0).unsafeRunSync()
        game.setFrameSkip(0).unsafeRunSync()

        val bwem = new BWEM(game.receiver)
        bwem.initialize()
        bwem.getMap.enableAutomaticPathAnalysis()
        bwem.getMap.assignStartingLocationsToSuitableBases()
        bwmap = Option(bwem.getMap)

      }
    } yield ()

    task
      .handleErrorWith({ err => logger[IO].error(err)("on start failed") })
      .unsafeRunSync()
  }

  override def onFrame(): Unit = {
    val task = for {
      game <- getGame
      _    <- logger[IO].trace(s"Frame ${game.getFrameCount}")
      map  <- bwmapIO
      _ <- IO {
        implicit val g = game

        game.drawTextScreen(200, 0, s"FPS: ${game.getFPS}").unsafeRunSync()
        game.drawTextScreen(200, 20, s"Average FPS: ${game.getAverageFPS.toInt}").unsafeRunSync()
        val startingArea = map.getNearestArea(game.self.startLocation)
        game
          .drawBoxMap(startingArea.getTopLeft.toPosition, startingArea.getBottomRight.toPosition, Color.Red)
          .unsafeRunSync()
        val startingBase = startingArea.getBases.asScala
          .filter({ base => base.isStartingLocation })
          .head

        val loc = startingBase.getLocation
        game.drawBoxMap(loc.toPosition, loc.add(new TilePosition(4, 3)).toPosition, Color.Blue).unsafeRunSync()

        game.drawCircleMap(startingBase.getCenter, 3, Color.Red).unsafeRunSync()

        val preferredTile = for {
          buildingType <- refineV[Building](UnitType.Terran_Supply_Depot).toOption
          buildableTiles <- NonEmptyList.fromList(
            Surveyor
              .findPlacement(startingArea, buildingType)
              .toList
          )
          mat = DenseMatrix(
            buildableTiles
              .map({ tilePosition =>
                val distanceToMineralField = game
                  .getClosestUnit(tilePosition.toPosition, UnitFilter.IsMineralField)
                  .map({ closestMineralField => tilePosition.getDistance(closestMineralField.tilePosition) })
                  .getOrElse(Double.NaN)

                val distanceToSupplyDepot = game
                  .getClosestUnit(tilePosition.toPosition, (u: bwapi.Unit) => u.getType == UnitType.Terran_Supply_Depot)
                  .map({ supplyDepot => tilePosition.getDistance(supplyDepot.tilePosition) })
                  .getOrElse(Double.NaN)

                val supplyDepotToLeft = game
                  .getUnitsOnTile(tilePosition.subtract(new TilePosition(UnitType.Terran_Supply_Depot.tileWidth(), 0)))
                  .filter({ unit =>
                    unit.unitType.isBuilding &&
                    unit.unitType.tileWidth() == UnitType.Terran_Supply_Depot.tileWidth() && unit.unitType
                      .tileHeight() == UnitType.Terran_Supply_Depot.tileHeight()
                  })
                  .headOption
                  .fold(Double.NaN)(_ => 1.0)

                val supplyDepotToRight = game
                  .getUnitsOnTile(tilePosition.add(new TilePosition(UnitType.Terran_Supply_Depot.tileWidth(), 0)))
                  .filter({ unit =>
                    unit.unitType.isBuilding &&
                    unit.unitType.tileWidth() == UnitType.Terran_Supply_Depot.tileWidth() && unit.unitType
                      .tileHeight() == UnitType.Terran_Supply_Depot.tileHeight()
                  })
                  .headOption
                  .fold(Double.NaN)(_ => 1.0)

                val supplyDepotToBottom = game
                  .getUnitsOnTile(tilePosition.add(new TilePosition(0, UnitType.Terran_Supply_Depot.tileHeight())))
                  .filter({ unit =>
                    unit.unitType.isBuilding &&
                    unit.unitType.tileWidth() == UnitType.Terran_Supply_Depot.tileWidth() && unit.unitType
                      .tileHeight() == UnitType.Terran_Supply_Depot.tileHeight()
                  })
                  .headOption
                  .fold(Double.NaN)(_ => 1.0)

                val supplyDepotToTop = game
                  .getUnitsOnTile(tilePosition.subtract(new TilePosition(0, UnitType.Terran_Supply_Depot.tileHeight())))
                  .filter({ unit =>
                    unit.unitType.isBuilding &&
                    unit.unitType.tileWidth() == UnitType.Terran_Supply_Depot.tileWidth() && unit.unitType
                      .tileHeight() == UnitType.Terran_Supply_Depot.tileHeight()
                  })
                  .headOption
                  .fold(Double.NaN)(_ => 1.0)

                val supplyDepotToBottomRight = game
                  .getUnitsOnTile(
                    tilePosition.add(
                      new TilePosition(
                        UnitType.Terran_Supply_Depot.tileWidth(),
                        UnitType.Terran_Supply_Depot.tileHeight()
                      )
                    )
                  )
                  .filter({ unit =>
                    unit.unitType.isBuilding &&
                    unit.unitType.tileWidth() == UnitType.Terran_Supply_Depot.tileWidth() && unit.unitType
                      .tileHeight() == UnitType.Terran_Supply_Depot.tileHeight()
                  })
                  .headOption
                  .fold(Double.NaN)(_ => 1.0)

                val supplyDepotToTopRight = game
                  .getUnitsOnTile(
                    tilePosition
                      .add(new TilePosition(UnitType.Terran_Supply_Depot.tileWidth(), 0))
                      .subtract(new TilePosition(0, UnitType.Terran_Supply_Depot.tileHeight()))
                  )
                  .filter({ unit =>
                    unit.unitType.isBuilding &&
                    unit.unitType.tileWidth() == UnitType.Terran_Supply_Depot.tileWidth() && unit.unitType
                      .tileHeight() == UnitType.Terran_Supply_Depot.tileHeight()
                  })
                  .headOption
                  .fold(Double.NaN)(_ => 1.0)

                val isAtEdgeOfMap =
                  if (
                    tilePosition.x == 0 || tilePosition.y == 0 || tilePosition.x + UnitType.Terran_Supply_Depot
                      .tileWidth() == game.mapWidth || tilePosition.y + UnitType.Terran_Supply_Depot
                      .tileHeight() == game.mapHeight
                  ) 1.0
                  else 0.0

                /*    val distanceToCliff = Option(
                  map
                    .*/
                // println(distanceToCliff)
                /*    if (!supplyDepotToTopRight.isNaN) {
                  println(supplyDepotToTopRight)
                }*/

                // println(supplyDepotToBottomRight, supplyDepotToTopRight)
                Array(
                  distanceToMineralField,
                  // distanceToSupplyDepot,
                  isAtEdgeOfMap,
                  supplyDepotToLeft,
                  supplyDepotToRight,
                  supplyDepotToBottom,
                  supplyDepotToTop,
                  supplyDepotToBottomRight,
                  supplyDepotToTopRight
                )
              })
              .toList: _*
          )
          _ = mat(::, 0) := (min(mat(::, 0)) /:/ mat(::, 0)) ^:^ 0.01
          // _  = mat(::, 1) := (min(mat(::, 1)) /:/ mat(::, 1)) ^:^ 0.2
          _  = mat(::, 1) := (mat(::, 1) /:/ max(mat(::, 1))) ^:^ 0.01
          _  = mat(::, 2) := (mat(::, 2) /:/ max(mat(::, 2))) ^:^ 0.01
          _  = mat(::, 3) := (mat(::, 3) /:/ max(mat(::, 3))) ^:^ 0.01
          _  = mat(::, 4) := (mat(::, 4) /:/ max(mat(::, 4))) ^:^ 0.01
          _  = mat(::, 5) := (mat(::, 5) /:/ max(mat(::, 5))) ^:^ 0.01
          _  = mat(::, 6) := (mat(::, 6) /:/ max(mat(::, 6))) ^:^ 0.01
          _  = mat(::, 7) := (mat(::, 7) /:/ max(mat(::, 7))) ^:^ 0.93
          xd = mat.mapValues(v => if (v.isNaN) 1.0 else v)
          yolo = xd(*, ::)
            .map(_.reduce({ (e1, e2) => e1 * e2 }))
          squid = buildableTiles.toList.zip(yolo.toArray.toList)
        } yield squid
          .reduce({ (a, b) =>
            val (t1, s1) = a
            val (t2, s2) = b

            if (s1 > s2) a else b
          })
          ._1

        val worker = Workers(game.self.units) |> (_.headOption)

        game
          .drawBoxMap(
            preferredTile.get.toPosition,
            preferredTile.get.add(new TilePosition(3, 2)).toPosition,
            Color.Cyan
          )
          .unsafeRunSync()

        if (
          game.self.supplyTotal() - game.self.supplyUsed() <= 2 && game.self
            .supplyTotal() <= 400
        ) {
          val buildingType = refineV[Building](UnitType.Terran_Supply_Depot).toOption.get
          worker.map(b => Worker.build(buildingType, preferredTile.get)(b).unsafeRunSync())

        }

        Workers(game.self.units).foreach({ worker =>
          if (worker.isIdle) {
            game
              .getClosestUnit(worker.position, UnitFilter.IsMineralField)
              .map({ mineralField =>
                worker.returnCargo(true).unsafeRunSync()
                worker.gather(mineralField, true).unsafeRunSync()
              })
          }

        })
      }

      _ <- IO {
        /*    val mainTile     = bwapi.game.self.getStartLocation
        val mainPosition = new Position(mainTile).add(new Position(64, 48))
        val mainArea     = map.getArea(mainTile)

        bwapi.game.drawBox(
          CoordinateType.Map,
          mainTile.x * 32,
          mainTile.y * 32,
          (mainTile.x * 32) + (32 * 4),
          (mainTile.y * 32) + (32 * 3),
          Color.Blue
        )

        // Main Start Blocks
        val race = bwapi.game.self.getRace

        val start        = mainPosition
        val tileStart    = new TilePosition(start)
        val tileBest     = TilePosition.Invalid
        val distanceBest = Double.MaxValue
        val piecesBest   = List

        for (i <- 10 to 1) {
          for (j <- 10 to 1) {
            for (x <- tileStart.x - 15 to tileStart.x + 15) {
              for (y <- tileStart.y - 15 to tileStart.y + 15) {
                val tile = new TilePosition(x, y)
                val blockCenter =
                  new Position(tile).add(new Position(i * 16, j * 16))
                val dist           = blockCenter.getDistance(start)
                val blockFacesLeft = blockCenter.x < mainPosition.x
                val blockFacesUp   = blockCenter.y < mainPosition.y
              }
            }
          }
        }*/
      }
      nextWorldState <- IO.pure(
        worldState.getOrElse(WorldState.initialState(game)) |> GenLens[WorldState](_.game)
          .set(game)
      )
      // nextWorldState <- IO(GenLens[WorldState](_.game).set(Option(game))(worldState))
      nextWorldState <- {
        val plan = Plan(nextWorldState, PlanSimulator)
        plan
          .map(task => PlanInterpreter.interpret(nextWorldState, task))
          .sequence
          .map({ state => state.last })
      }
      _ <- IO { worldState = Option(nextWorldState) }
    } yield ()

    task
      .handleErrorWith({ err =>
        println(err)
        logger[IO].error(err)("on frame failed")
      })
      .unsafeRunSync()
  }

  override def onEnd(isWinner: Boolean): Unit = {
    val task = for {
      _ <- logger[IO].info("Game Ended")
      _ <- dispatch(Action.EndGame)
    } yield ()

    task
      .handleErrorWith({ err => logger[IO].error(err)("on end failed") })
      .unsafeRunSync()
  }

  override def onUnitComplete(unit: bwapi.Unit) {

    if (unit.getType.isWorker) {
      var closestMineral: bwapi.Unit = null
      var closestDistance            = Integer.MAX_VALUE
      for (mineral <- getGame().unsafeRunSync().getMinerals) {
        val distance = unit.getDistance(mineral)
        if (distance < closestDistance) {
          closestMineral = mineral
          closestDistance = distance
        }
      }
      // Gather the closest mineral
      unit.gather(closestMineral)
    }
  }
}
