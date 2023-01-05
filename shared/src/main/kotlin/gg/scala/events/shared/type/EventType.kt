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

    object Sumo : EventType
    {
        override val icon = ItemBuilder
            .of(XMaterial.LEAD)
            .build()

        override val description =
            "Fight against other players in a fist-fight to claim the winner role."

        override val mode = object : CgsGameMode
        {
            override fun getArenas() = listOf(
                object : CgsGameArena
                {
                    override fun getBukkitWorldName() = "world"
                    override fun getDescription() = ""

                    override fun getDirectory(): Path? = null
                    override fun getId() = "world"

                    override fun getMaterial() = Material.ARROW to 1
                    override fun getName() = "sumo"

                    override fun getPreLobbyLocation() = Location(
                        Bukkit.getWorld("world"),
                        294143.5, 74.000, 49050.5, 90.0F, 0.0F
                    )

                    override fun getSpectatorLocation() = Location(
                        Bukkit.getWorld("world"),
                        294143.5, 74.000, 49050.5, 90.0F, 0.0F
                    )
                }
            )
            override fun getDescription() = ""

            override fun getId() = "sumo"

            override fun getMaterial() = Material.LEASH to 1

            override fun getMaxTeams() = 30
            override fun getTeamSize() = 1

            override fun getName() = "Sumo"
        }

        override val info = CgsGameGeneralInfo(
            fancyNameRender = "Sumo",
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
            showNameHearts = false,
            usesCustomArenaWorld = true,
            gameModes = listOf(mode)
        ).apply {
            this.configureSpectatorChat = false
        }
    }

    companion object
    {
        val TYPES = mutableListOf(Sumo)
    }
}
