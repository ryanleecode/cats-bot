package bot.architecture.heuristics
import com.spotify.featran.transformers.{OneDimensional, Settings, SettingsBuilder, Transformer}
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric._
import monocle.macros.GenLens

case class Feature(values: List[Option[Double]], weight: Int Refined Positive) {
  def normalize(normalizer: List[Option[Double]] => List[Option[Double]]): Unit = {
    GenLens[Feature](_.values).set(normalizer(values))
  }
}

object Normalization {
  def beneficial(values: List[Option[Double]]): Iterable[Option[Double]] = {
    val maxOption = values.max

    values.map(_.flatMap({ x => maxOption.map({ max => x / max }) }))
  }
  def nonBeneficial(values: List[Option[Double]]): Iterable[Option[Double]] = {
    val minOption = values.min

    values.map(_.flatMap({ x => minOption.map({ min => min / x }) }))
  }
}
