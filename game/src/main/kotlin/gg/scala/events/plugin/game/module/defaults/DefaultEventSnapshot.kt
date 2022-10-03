package gg.scala.events.plugin.game.module.defaults

import gg.scala.cgs.common.snapshot.CgsGameSnapshot

/**
 * @author GrowlyX
 * @since 10/3/2022
 */
object DefaultEventSnapshot : CgsGameSnapshot
{
    override fun getExtraInformation() = listOf<String>()
}
