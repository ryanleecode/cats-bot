import bwapi.{DefaultBWListener, Player}
import cats.effect.IO
import monix.execution.Scheduler.Implicits.global
import monix.execution.annotations.UnsafeBecauseImpure
import monix.reactive.subjects.ConcurrentSubject
import monix.reactive.{MulticastStrategy, Observable}

/**
 * A listener that listeners to BWAPI events and remits them as `Observable` streams.
 *
 * @param onStartCallback Synchronous on start callback that runs on frame 0 during initialization.
 *
 *                        '''UNSAFE WARNING''': this operation can trigger the execution of side
 *                        effects, which breaks referential transparency and is thus not a pure function.
 */
class Listener(@UnsafeBecauseImpure private val onStartCallback: IO[Unit])
  extends DefaultBWListener
    with LifecycleObservable {

  private val onEndSubject = ConcurrentSubject[Unit](MulticastStrategy.publish)
  private val onFrameSubject = ConcurrentSubject[Unit](MulticastStrategy.publish)
  private val onSendTextSubject = ConcurrentSubject[String](MulticastStrategy.publish)
  private val onReceiveTextSubject = ConcurrentSubject[(Player, String)](MulticastStrategy.publish)
  private val onPlayerLeftSubject = ConcurrentSubject[Player](MulticastStrategy.publish)

  override val end: Observable[Unit] = onEndSubject
  override val frame: Observable[Unit] = onFrameSubject
  override val sendText: Observable[String] = onSendTextSubject
  override val receiveText: Observable[(Player, String)] = onReceiveTextSubject
  override val playerLeft: Observable[Player] = onPlayerLeftSubject

  override def onStart(): Unit = onStartCallback.unsafeRunSync()

  override def onEnd(isWinner: Boolean): Unit = onEndSubject.onNext()

  override def onFrame(): Unit = onFrameSubject.onNext()

  override def onSendText(text: String): Unit = onSendTextSubject.onNext(text)

  override def onReceiveText(player: Player, text: String): Unit = {
    onReceiveTextSubject.onNext(player, text)
  }

  override def onPlayerLeft(player: Player): Unit = {
    onPlayerLeftSubject.onNext(player)
  }
}
