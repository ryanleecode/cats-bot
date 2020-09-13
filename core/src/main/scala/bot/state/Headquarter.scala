package bot.state

import bwapi.UnitType

sealed trait Headquarter {
  val unit: bwapi.Unit
}

object Headquarter {
  case class TerranHQ(override val unit: bwapi.Unit)  extends Headquarter
  case class ProtossHQ(override val unit: bwapi.Unit) extends Headquarter
  case class ZergHQ(override val unit: bwapi.Unit)    extends Headquarter

  def apply[A](unit: bwapi.Unit): Option[Headquarter] = {
    if (isTerranHQ(unit)) {
      Some(TerranHQ(unit))
    } else if (isZergHQ(unit)) {
      Some(ZergHQ(unit))
    } else if (isProtossHQ(unit)) {
      Some(ProtossHQ(unit))
    } else {
      None
    }
  }

  def is[A](unit: bwapi.Unit): Boolean = {
    isTerranHQ(unit) ||
    isProtossHQ(unit) ||
    isZergHQ(unit)
  }

  def isTerranHQ[A](unit: bwapi.Unit): Boolean = {
    unit.getType == UnitType.Terran_Command_Center
  }

  def isProtossHQ[A](unit: bwapi.Unit): Boolean = {
    unit.getType == UnitType.Protoss_Nexus
  }

  def isZergHQ[A](unit: bwapi.Unit): Boolean = {
    unit.getType == UnitType.Zerg_Hatchery ||
    unit.getType == UnitType.Zerg_Lair ||
    unit.getType == UnitType.Zerg_Hive
  }
}
