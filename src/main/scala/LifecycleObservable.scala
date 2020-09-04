import bwapi.Player
import monix.reactive.Observable

trait LifecycleObservable {
  val end: Observable[Unit]
  val frame: Observable[Unit]
  val sendText: Observable[String]
  val receiveText: Observable[(Player, String)]
  val playerLeft: Observable[Player]
}
