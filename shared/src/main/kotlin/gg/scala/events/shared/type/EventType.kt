package gg.scala.events.shared.type

import com.cryptomorin.xseries.XMaterial
import gg.scala.cgs.common.information.CgsGameAwardInfo
import gg.scala.cgs.common.information.CgsGameGeneralInfo
import gg.scala.cgs.common.information.arena.CgsGameArena
import gg.scala.cgs.common.information.mode.CgsGameMode
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.nio.file.Path

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
            override fun getArenas() = listOf(
                object : CgsGameArena
                {
                    override fun getBukkitWorldName() = "bw-test-two"
                    override fun getDescription() = ""

                    override fun getDirectory(): Path? = null
                    override fun getId() = "bw-test-two"

                    override fun getMaterial() = Material.ARROW to 1
                    override fun getName() = "OITC"

                    override fun getPreLobbyLocation() = Location(
                        Bukkit.getWorld("world"),
                        0.5, 150.0, 0.5
                    )

                    override fun getSpectatorLocation() = Location(
                        Bukkit.getWorld(getBukkitWorldName()), 0.5, Bukkit.getWorld(getBukkitWorldName()).getHighestBlockYAt(0, 0) + 15.0, 0.5
                    )
                }
            )
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
            startingCountdownSec = 121,
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
