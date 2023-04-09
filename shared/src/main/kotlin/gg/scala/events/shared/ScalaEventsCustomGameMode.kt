package gg.scala.events.shared

import gg.scala.cgs.common.information.arena.CgsGameArena
import gg.scala.cgs.common.information.mode.CgsGameMode
import org.bukkit.Material

/**
 * @author GrowlyX
 * @since 10/3/2022
 */
object ScalaEventsCustomGameMode : CgsGameMode
{
    override fun getArenas() = listOf<CgsGameArena>()
    override fun getDescription() = ""

    override fun getId() = "custom"

    override fun getMaterial() =
        Material.WOOL to 1

    override fun getMaxTeams() = 4
    override fun getTeamSize() = 2

    override fun getName() = "custom"
}
