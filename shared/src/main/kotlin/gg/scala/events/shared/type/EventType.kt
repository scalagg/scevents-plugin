package gg.scala.events.shared.type

import com.cryptomorin.xseries.XMaterial
import gg.scala.cgs.common.information.CgsGameAwardInfo
import gg.scala.cgs.common.information.CgsGameGeneralInfo
import gg.scala.cgs.common.information.arena.CgsGameArena
import gg.scala.cgs.common.information.mode.CgsGameMode
import net.evilblock.cubed.util.CC
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
            .of(Material.LEASH)
            .build()

        override val description =
            "Fight against other players in a fist-fight to claim the winner role."

        override val mode = object : CgsGameMode
        {
            override fun getArenas() = listOf(
                object : CgsGameArena
                {
                    override fun getBukkitWorldName() = "Sumo"
                    override fun getDescription() = ""

                    override fun getDirectory(): Path? = null
                    override fun getId() = "sumo"

                    override fun getMaterial() = Material.ARROW to 1
                    override fun getName() = "sumo"

                    override fun getPreLobbyLocation() = Location(
                        Bukkit.getWorld("sumo"),
                        14.0, 91.000, -49.500, 0.0f, 0.0f
                    )

                    override fun getSpectatorLocation() = Location(
                        Bukkit.getWorld("sumo"),
                        14.0, 91.000, -49.500, 0.0f, 0.0f
                    )
                }
            )
            override fun getDescription() = ""

            override fun getId() = "sumo"

            override fun getMaterial() = Material.LEASH to 1

            override fun getMaxTeams() = 25
            override fun getTeamSize() = 1

            override fun getName() = "Sumo"
        }

        override val info = CgsGameGeneralInfo(
            fancyNameRender = "Sumo",
            gameVersion = 1.0F,
            minimumPlayers = 4,
            startingCountdownSec = 60,
            awards = CgsGameAwardInfo(
                true, 100..125, 20..35
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
            this.customDeathMessage = true
            this.customDeathMessageService = { player ->
                "${CC.D_RED}✘ ${CC.WHITE}${player.name}${CC.RED} was eliminated from the event."
            }
        }
    }

    object WoolShuffle : EventType
    {
        override val icon = ItemBuilder
            .of(XMaterial.RED_WOOL)
            .build()

        override val description =
            "Test your skills and be the first to survive multiple rounds of wool changes."

        override val mode = object : CgsGameMode
        {
            override fun getArenas() = listOf(
                object : CgsGameArena
                {
                    override fun getBukkitWorldName() = "MapHike"
                    override fun getDescription() = ""

                    override fun getDirectory(): Path? = null
                    override fun getId() = "MapHike"

                    override fun getMaterial() = Material.ARROW to 1
                    override fun getName() = "wool_shuffle"

                    private val lobby = Location(
                        Bukkit.getWorld("MapHike"),
                        32.5, 54.5, 62.5, -90.0f, 0.0f
                    )

                    override fun getPreLobbyLocation() = lobby
                    override fun getSpectatorLocation() = lobby
                }
            )
            override fun getDescription() = ""

            override fun getId() = "wool_shuffle"

            override fun getMaterial() = Material.WOOL to 1

            override fun getMaxTeams() = 30
            override fun getTeamSize() = 1

            override fun getName() = "Wool Shuffle"
        }

        override val info = CgsGameGeneralInfo(
            fancyNameRender = "Wool Shuffle",
            gameVersion = 1.0F,
            minimumPlayers = 2,
            startingCountdownSec = 61,
            awards = CgsGameAwardInfo(
                true, 150..200, 10..25
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
            this.customDeathMessage = true
            this.customDeathMessageService = { player ->
                "${CC.D_RED}✘ ${CC.WHITE}${player.name}${CC.RED} was eliminated from the event."
            }
        }
    }

    companion object
    {
        val TYPES: List<EventType> = mutableListOf(Sumo, WoolShuffle /*Bonkers*/)
    }
}
