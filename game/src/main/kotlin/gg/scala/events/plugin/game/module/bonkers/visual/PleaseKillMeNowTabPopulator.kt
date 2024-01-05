package gg.scala.events.plugin.game.module.bonkers.visual

import com.google.common.collect.HashBasedTable
import gg.scala.cgs.common.CgsGameEngine
import gg.scala.cgs.common.player.handler.CgsPlayerHandler
import gg.scala.cgs.common.states.CgsGameState
import gg.scala.cgs.common.teams.CgsGameTeam
import gg.scala.cgs.common.teams.CgsGameTeamService
import gg.scala.commons.tablist.TablistPopulator
import gg.scala.events.plugin.game.module.bonkers.data
import gg.scala.events.plugin.game.module.bonkers.findClaimContextual
import gg.scala.events.plugin.game.module.bonkers.team
import gg.scala.lemon.util.QuickAccess.username
import io.github.nosequel.tab.shared.entry.TabElement
import net.evilblock.cubed.util.time.TimeUtil
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 5/23/2023
 */
object PleaseKillMeNowTabPopulator : TablistPopulator
{
    override fun displayPreProcessor(player: Player) =
        CompletableFuture.completedFuture(true)

    override fun populate(player: Player, layout: TabElement)
    {
        var y = 3

        layout.add(0, y++, "${ChatColor.GOLD}Player Info:")
        layout.add(
            0, y++, "${ChatColor.GRAY}Kills: ${ChatColor.WHITE}${
                runCatching {
                    CgsGameEngine.INSTANCE
                        .getStatistics(CgsPlayerHandler.find(player)!!)
                        .gameKills.value
                }.getOrNull() ?: 0
            }"
        )

        y++

        if (player.location.world != null)
        {
            layout.add(0, y++, "${ChatColor.GOLD}Location:")
            layout.add(
                0,
                y++,
                "${ChatColor.RED}${
                    player
                        .findClaimContextual("base")
                        ?.key ?: "Warzone"
                }"
            )
            layout.add(
                0,
                y++,
                "${ChatColor.GRAY}${player.location.blockX}, ${player.location.blockZ} [${
                    getCardinalDirection(player)
                }${ChatColor.GRAY}]"
            )
        }

        val team = player.team()?.data()

        if (team != null)
        {
            y = 3

            layout.add(1, y++, "${ChatColor.GOLD}Team Info:")
            layout.add(1, y++, "${ChatColor.GRAY}DTR: ${team.dtr}")
            layout.add(
                1,
                y++,
                "${ChatColor.GRAY}Online: ${ChatColor.WHITE}${player.team()!!.alive.size}/${player.team()!!.participants.size}"
            )
            layout.add(1, y++, "${ChatColor.GRAY}Balance: $${team.balance}")
        }

        y = 3

        layout.add(2, y++, "${ChatColor.GOLD}Game Info:")

        if (CgsGameEngine.INSTANCE.gameState.isAfter(CgsGameState.STARTED))
        {
            layout.add(
                2,
                y++,
                "${ChatColor.GRAY}Time: ${ChatColor.WHITE}${
                    TimeUtil.formatIntoMMSS(
                        ((System.currentTimeMillis() - CgsGameEngine.INSTANCE.gameStart) / 1000).toInt()
                    )
                }"
            )
        } else
        {
            layout.add(
                2, y++, "${ChatColor.GRAY}Map: ${ChatColor.WHITE}${
                    CgsGameEngine.INSTANCE.gameArena!!.getName()
                }"
            )
        }

        layout.add(
            2,
            y++,
            "${ChatColor.GRAY}Players: ${ChatColor.WHITE}${
                Bukkit.getOnlinePlayers().count { !it.hasMetadata("spectator") }
            }"
        )

        /*y++

        if (CgsGameEngine.INSTANCE.gameState.isAfter(CgsGameState.STARTED))
        {
            val captureZone = Bunkers.instance.eventHandler.getCaptureZone()

            layout.add(2, y++, "${ChatColor.GOLD}${GameEngine.instance.gameHandler.map.id}:")
            layout.add(2, y++, "${ChatColor.GRAY}${captureZone.blockX}, ${captureZone.blockZ}")

            val time = if (Bunkers.instance.eventHandler.isActive())
            {
                Bunkers.instance.eventHandler.service.remaining.get() * 1000L
            } else
            {
                (EventHandler.KOTH_START_TIME * 1000L) - (GameEngine.instance.gameHandler.getGameTime())
            }

            layout.add(2, y++, "${ChatColor.GRAY}${FormatUtil.formatIntoMMSS(time)}")
        }*/

        y = 7

        var x = 1

        CgsGameTeamService.teams.values
            .sortedByDescending {
                it.participants.contains(player.uniqueId)
            }
            .forEach {
                val next: Int = if (y == 7)
                {
                    0
                } else
                {
                    if (x >= 2)
                    {
                        0
                    } else
                    {
                        x + 1
                    }
                }

                layout.add(
                    x,
                    y++,
                    "${it.data()!!.colored()} Team ${ChatColor.GRAY}[${
                        it.data()!!.dtr
                    }${ChatColor.GRAY}]"
                )

                it.participants.forEach { member -> layout.add(x, y++, getDisplayName(player, member, it)) }

                y = 14
                x = next
            }
    }

    private fun getCardinalDirection(player: Player):String {

        var rot = (player.location.yaw - 90) % 360.toDouble()

        if (rot < 0) {
            rot += 360.0
        }

        return getDirection(rot)
    }

    private fun getDirection(rot: Double): String {

        return when {
            0 <= rot && rot < 22.5 -> Direction.WEST.getDisplay()
            22.5 <= rot && rot < 67.5 -> "${Direction.NORTH.getDisplay()}${Direction.WEST.getDisplay()}"
            67.5 <= rot && rot < 112.5 -> Direction.NORTH.getDisplay()
            112.5 <= rot && rot < 157.5 -> "${Direction.NORTH.getDisplay()}${Direction.EAST.getDisplay()}"
            157.5 <= rot && rot < 202.5 -> Direction.EAST.getDisplay()
            202.5 <= rot && rot < 247.5 -> "${Direction.SOUTH.getDisplay()}${Direction.EAST.getDisplay()}"
            247.5 <= rot && rot < 292.5 -> Direction.SOUTH.getDisplay()
            292.5 <= rot && rot < 337.5 -> "${Direction.SOUTH.getDisplay()}${Direction.WEST.getDisplay()}"
            337.5 <= rot && rot < 360.0 -> Direction.WEST.getDisplay()
            else -> ""
        }

    }
    private fun getDisplayName(viewer: Player, thing: UUID, team: CgsGameTeam): String {
        val player = Bukkit.getPlayer(thing)

        if (player != null)
        {
            if (viewer.hasMetadata("spectator")) {
                return "${ChatColor.GRAY}${ChatColor.STRIKETHROUGH}${player.name}"
            }

            return "${team.data()!!.color}${player.name}"
        }

        val name = thing.username()

        if (team.eliminated.contains(thing)) {
            return "${ChatColor.DARK_GRAY}${ChatColor.STRIKETHROUGH}${name}"
        }

        return "${ChatColor.GRAY}$name"
    }

    enum class Direction(private val display: String) {

        EAST("E"),
        WEST("W"),
        SOUTH("S"),
        NORTH("N");

        fun getDisplay():String {
            return "${DIRECTIONS.get("western", this) ?: ChatColor.WHITE}${this.display}"
        }

    }

    val DIRECTIONS = HashBasedTable.create<String, Direction,ChatColor>()

    init {
        //TODO LOL
        DIRECTIONS.put("western", Direction.WEST,ChatColor.BLUE)
        DIRECTIONS.put("western", Direction.EAST,ChatColor.YELLOW)
        DIRECTIONS.put("western", Direction.SOUTH,ChatColor.RED)
        DIRECTIONS.put("western", Direction.NORTH,ChatColor.GREEN)
    }

}
