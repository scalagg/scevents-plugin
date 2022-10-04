package gg.scala.events.shared.type

import com.cryptomorin.xseries.XMaterial
import gg.scala.cgs.common.information.CgsGameAwardInfo
import gg.scala.cgs.common.information.CgsGameGeneralInfo
import gg.scala.cgs.common.information.arena.CgsGameArena
import gg.scala.cgs.common.information.mode.CgsGameMode
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * @author GrowlyX
 * @since 10/3/2022
 */
sealed interface EventType
{
    val icon: ItemStack
    val description: String

    val info: CgsGameGeneralInfo
    val mode: CgsGameMode

    object OneInTheChamber : EventType
    {
        override val icon = ItemBuilder
            .of(XMaterial.ARROW)
            .build()

        override val description =
            "First to kill the most players win!"

        override val mode = object : CgsGameMode
        {
            override fun getArenas() = listOf<CgsGameArena>()
            override fun getDescription() = "oitc."

            override fun getId() = "oitc"

            override fun getMaterial() = Material.ARROW to 1

            override fun getMaxTeams() = 30
            override fun getTeamSize() = 1

            override fun getName() = "OITC"
        }

        override val info = CgsGameGeneralInfo(
            fancyNameRender = "One In The Chamber",
            gameVersion = 1.0F,
            minimumPlayers = 2,
            startingCountdownSec = 61,
            awards = CgsGameAwardInfo(
                false, 0..100, 0..100
            ),
            preStartVoting = false,
            disqualifyOnLogout = true,
            spectateOnDeath = true,
            showTabHearts = false,
            showNameHearts = true,
            usesCustomArenaWorld = true,
            gameModes = listOf(mode)
        )
    }

    companion object
    {
        val TYPES = mutableListOf<EventType>(OneInTheChamber)
    }
}
