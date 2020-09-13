package bot.lifecycle

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.Duration

object GameSpeed extends Enumeration {
  val Fastest: Int = Duration.create(42, TimeUnit.MILLISECONDS).toMillis.toInt
  val Faster: Int  = Duration.create(48, TimeUnit.MILLISECONDS).toMillis.toInt
  val Fast: Int    = Duration.create(56, TimeUnit.MILLISECONDS).toMillis.toInt
  val Normal: Int  = Duration.create(67, TimeUnit.MILLISECONDS).toMillis.toInt
  val Slow: Int    = Duration.create(83, TimeUnit.MILLISECONDS).toMillis.toInt
  val Slower: Int  = Duration.create(111, TimeUnit.MILLISECONDS).toMillis.toInt
  val Slowest: Int = Duration.create(167, TimeUnit.MILLISECONDS).toMillis.toInt
}
