import bwapi.{BWClient, BWClientConfiguration}
import cats.effect.{ExitCode, IO, IOApp, Sync}
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global

object Main extends IOApp {
  var clientOption: Option[BWClient] = None

  implicit val lazyClientIO: IO[BWClient] = IO {
    clientOption.get
  }

  implicit def logger[F[_] : Sync]: SelfAwareStructuredLogger[F]
  = Slf4jLogger.getLoggerFromName("Bot")

  override def run(args: List[String]): IO[ExitCode] = {
    val config = new BWClientConfiguration()
    config.debugConnection = true
    config.async = true
    config.autoContinue = true
    config.logVerbosely = false

    val listener = new Listener(Lifecycle.onStart[IO])
    implicit val client = new BWClient(listener)
    clientOption = Option(client)

    listener.end.doOnNextF(_ => Lifecycle.onEnd[Task]).subscribe()

    lazyClientIO.unsafeRunSync().startGame(config)
    IO(ExitCode.Success)
  }
}
