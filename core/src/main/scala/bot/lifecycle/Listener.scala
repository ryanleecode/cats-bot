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
          data =
            buildableTiles
              .map({ tilePosition =>
                val distanceToMineralField = game
                  .getClosestUnit(tilePosition.toPosition, UnitFilter.IsMineralField)
                  .map({ closestMineralField => -tilePosition.getDistance(closestMineralField.getTilePosition) })

                val distanceToSupplyDepot = game
                  .getClosestUnit(tilePosition.toPosition, (u: bwapi.Unit) => u.getType == UnitType.Terran_Supply_Depot)
                  .map({ supplyDepot => -tilePosition.getDistance(supplyDepot.getTilePosition) })

                Heuristic1(distanceToMineralField, distanceToSupplyDepot)
              })
              .toIterable
          fs =
            FeatureSpec
              .of[Heuristic1]
              .optional(_.distanceToMineralField, Some(0.0))(MinMaxScaler("min-max"))
              .optional(_.distanceToSupplyDepot, Some(0.0))(MinMaxScaler("min-max"))
          fe       = fs.extract(data)
          names    = fe.featureNames
          weights  = Array(0.25, 0.75)
          features = fe.featureValues[Array[Double]]
          product  = new summary.Product()
          xd =
            buildableTiles.toIterable
              .zip(
                features
                  .map({ feature =>
                    val product = new summary.Product()
                    product.evaluate(feature, weights)
                  })
              )
              .reduce({ (a, b) =>
                val (_, s1) = a
                val (_, s2) = b
                if (s1 > s2) a else b
              })
              ._1
        } yield xd

        // println(df)
        /*        val furthestMineral = NonEmptyList
          .fromList(startingBase.getMinerals.asScala.toList)
          .map({ minerals =>
            minerals.foldLeft(minerals.head)({ (a, b) =>
              val aPos = a.getCenter.toTilePosition.toPosition
              val bPos = b.getCenter.toTilePosition.toPosition

              if (
                aPos.getDistance(startingBase.getCenter) >= bPos
                  .getDistance(startingBase.getCenter)
              ) {
                a
              } else {
                b
              }

            })
          })

        for {
          furthestMineral <-
            NonEmptyList
              .fromList(startingBase.getMinerals.asScala.toList)
              .map({ minerals =>
                minerals.foldLeft(minerals.head)({ (a, b) =>
                  val aPos = a.getCenter.toTilePosition.toPosition
                  val bPos = b.getCenter.toTilePosition.toPosition

                  if (
                    aPos.getDistance(startingBase.getCenter) >= bPos
                      .getDistance(startingBase.getCenter)
                  ) {
                    a
                  } else {
                    b
                  }

                })
              })
        } yield none

        val anchorDepot = furthestMineral.map({ mineral =>
          map.breadthFirstSearch(
            mineral.getCenter.toTilePosition,
            new Pred[Tile, TilePosition]() {
              override def test(tile: Tile, position: TilePosition): Boolean = {
                val xd = for {
                  x <- position.x to position.x + 2
                  y <- position.y to position.y + 1
                } yield new TilePosition(x, y)

                val canBuild = xd.foldLeft(true)({ (canBuild, tilePosition) =>
                  canBuild && game.isBuildable(tilePosition, true) /* && bwapi.game
                .getUnitsOnTile(tilePosition)
                .size() == 0*/
                })

                val yolo = position.toPosition.add(new Position(48, 32))

                val squid = canBuild && yolo
                  .getDistance(startingBase.getCenter) > mineral.getCenter
                  .getDistance(startingBase.getCenter)
                game.drawCircleMap(position.toPosition, 3, Color.Orange).unsafeRunSync()
                if (squid) {}

                squid
              }
            },
            new Pred[Tile, TilePosition]() {
              override def test(tile: Tile, position: TilePosition): Boolean = {
                val yolo = position.toPosition.add(new Position(48, 32))
                /*    println(
                  position.getDistance(startingBase.getCenter.toTilePosition),
                  mineral.getCenter.toTilePosition
                    .getDistance(startingBase.getCenter.toTilePosition)
                )*/

                true
              }
            }
          )
        })*/

        /*        furthestMineral.map({ min =>
          game.drawCircleMap(min.getCenter, 3, Color.Orange).unsafeRunSync()
        })*/

        // val all2x2Tiles = Surveyor.findTilesInAreaMatchingShape()

        /*        anchorDepot.map({ pos =>
          game
            .drawBoxMap(
              new TilePosition(pos.x, pos.y).toPosition,
              new TilePosition(pos.x + 3, pos.y + 2).toPosition,
              Color.Purple
            )
            .unsafeRunSync()
        })*/
        /*

        val preferredTile =
          for {
            buildingType <- refineV[Building](UnitType.Terran_Supply_Depot).toOption
            buildableTiles <- NonEmptyList.fromList(
              Surveyor
                .findPlacement(startingArea, buildingType)
                .toList
            )
            _ = buildableTiles.map({ tile =>
              game
                .drawBoxMap(tile.toPosition, tile.add(buildingType.value.tileSize()).toPosition, Color.Teal)
                .unsafeRunSync()
            })
            _ <- for {
              tileDistances <-
                buildableTiles
                  .map({ tile =>
                    for {
                      closestMineralField <- game.getClosestUnit(tile.toPosition, UnitFilter.IsMineralField)
                      distanceToMineralField = tile.getDistance(closestMineralField.getTilePosition)
                    } yield (tile, distanceToMineralField)
                  })
                  .sequence
              minDistance =
                tileDistances
                  .minimumBy({ t =>
                    val (_, distance) = t
                    distance
                  })
                  ._2
              maxDistance =
                tileDistances
                  .maximumBy({ t =>
                    val (_, distance) = t
                    distance
                  })
                  ._2
            } yield tileDistances
            squid = {
              val derp = buildableTiles
                .map({ tile =>
                  for {
                    closestMineralField <- game.getClosestUnit(tile.toPosition, UnitFilter.IsMineralField)
                    distanceToMineralField = tile.getDistance(closestMineralField.getTilePosition)
                  } yield (tile, distanceToMineralField)
                })
                .sequence

              val metrics = buildableTiles.map({ tile =>
                val closestMineral = game.getClosestUnit(tile.toPosition, UnitFilter.IsMineralField)
                val closestDepot =
                  game.getClosestUnit(tile.toPosition, (u: bwapi.Unit) => u.getType == UnitType.Terran_Supply_Depot)

                val mineralDistance = closestMineral
                  .map(_.getTilePosition)
                  .map(tile.getDistance)
                val supplyDepotDistance = closestDepot.map(_.getTilePosition).map(tile.getDistance)

                List(mineralDistance, supplyDepotDistance)
              })

              val xd = metrics.toList.transpose
                .map({ poo =>
                  val inputsZ: List[Double] = poo.map({ o => o.getOrElse(0) })
                  val ds                    = new DescriptiveStatistics(inputsZ.toArray)
                  val variance              = ds.getPopulationVariance
                  val sd                    = Math.sqrt(variance)
                  val mean                  = ds.getMean

                  val normalizedInput = (for {
                    index <- 0 to inputsZ.size - 1
                    x = 1 / inputsZ.get(index).get
                  } yield x).toArray

                  normalizedInput.toList
                })
                .transpose

              val squid = for {
                i <- 0 to xd.length - 1
                val proudct                   = new summary.Product()
                val metricasda: Array[Double] = xd.get(i).get.toArray
                x                             = (buildableTiles.toList.get(i).get, proudct.evaluate(metricasda, Array(0.25, 0.75)))
              } yield x

              squid.toList
            }
            preferredTile =
              squid
                .foldLeft(squid.head)((prev, current) => {
                  val (prevTile, prevScore)       = prev
                  val (currentTile, currentScore) = current

                  if (currentScore <= prevScore) current else prev
                })
                ._1
            _ =
              game
                .drawBoxMap(
                  preferredTile.toPosition,
                  preferredTile.add(buildingType.value.tileSize()).toPosition,
                  Color.Purple
                )
                .unsafeRunSync()
          } yield preferredTile
         */

        /*        val anchorDepot2 =
          NonEmptyList
            .fromList(list.asScala.toList.filter({ tile =>
              tile.toPosition
                .getDistance(
                  startingBase.getCenter
                ) > furthestMineral.get.getCenter
                .getDistance(startingBase.getCenter)
            }))
            .map({ tilez =>
              tilez.foldLeft(tilez.head)({ (a, b) =>
                val aPos = a.toPosition
                val bPos = b.toPosition

                if (
                  aPos.getDistance(furthestMineral.get.getCenter) <= bPos
                    .getDistance(furthestMineral.get.getCenter)
                ) {
                  a
                } else {
                  b
                }

              })
            })

        // println(anchorDepot2)
        anchorDepot2.map({ min =>
          bwapi.game.drawBoxMap(
            min.toPosition,
            min.add(new TilePosition(3, 2)).toPosition,
            Color.Cyan
          )
        })*/

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
      .handleErrorWith({ err => logger[IO].error(err)("on frame failed") })
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
