package gg.scala.events.plugin.game.module.bonkers

import gg.scala.cgs.common.CgsGameEngine
import gg.scala.cgs.common.player.handler.CgsPlayerHandler
import gg.scala.commons.tablist.TablistPopulator
import gg.scala.flavor.service.Service
import io.github.nosequel.tab.shared.entry.TabElement
import org.bukkit.ChatColor
import org.bukkit.entity.Player
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
        /*var y = 3

        layout.add(0,y++,"${ChatColor.GOLD}Player Info:")
        layout.add(0,y++,"${ChatColor.GRAY}Kills: ${ChatColor.WHITE}${
            runCatching {
                CgsGameEngine.INSTANCE
                    .getStatistics(CgsPlayerHandler.find(player)!!)
                    .gameKills.value
            }.getOrNull() ?: 0
        }")

        y++

        if (player.location.world != null) {
            layout.add(0,y++,"${ChatColor.GOLD}Location:")
            layout.add(0,y++,"${ChatColor.RED}${Bunkers.instance.teamHandler.findByLocation(player.location).getDisplayName()}")
            layout.add(0,y++,"${ChatColor.GRAY}${player.location.blockX}, ${player.location.blockZ} [${this.getCardinalDirection(player)}${ChatColor.GRAY}]")
        }

        val team = Bunkers.instance.teamHandler.findById(player.uniqueId)

        if (team != null) {
            y = 3

            layout.add(1,y++,"${ChatColor.GOLD}Team Info:")
            layout.add(1,y++,"${ChatColor.GRAY}DTR: ${team.getDTRDisplay()}")
            layout.add(1,y++,"${ChatColor.GRAY}Online: ${ChatColor.WHITE}${team.findOnlineMembers().size}/${team.members.size}")
        }

        y = 3

        layout.add(2,y++,"${ChatColor.GOLD}Game Info:")

        if (GameEngine.instance.gameHandler.getState().isPastOrCurrently(GameServer.State.IN_PROGRESS)) {
            layout.add(2,y++,"${ChatColor.GRAY}Time: ${ChatColor.WHITE}${FormatUtil.formatIntoMMSS(GameEngine.instance.gameHandler.getGameTime())}")
        } else {

            var map = "Voting"

            if (GameEngine.instance.gameHandler.getState().isPastOrCurrently(GameServer.State.COUNTDOWN)) {
                map = GameEngine.instance.gameHandler.map.id
            }

            layout.add(2,y++,"${ChatColor.GRAY}Map: ${ChatColor.WHITE}$map")
        }

        layout.add(2,y++,"${ChatColor.GRAY}Players: ${ChatColor.WHITE}${GameEngine.instance.gameHandler.getPlayers().size}")

        y++

        if (GameEngine.instance.gameHandler.getState().isPastOrCurrently(GameServer.State.COUNTDOWN)) {
            val captureZone = Bunkers.instance.eventHandler.getCaptureZone()

            layout.add(2,y++,"${ChatColor.GOLD}${GameEngine.instance.gameHandler.map.id}:")
            layout.add(2,y++,"${ChatColor.GRAY}${captureZone.blockX}, ${captureZone.blockZ}")

            val time = if (Bunkers.instance.eventHandler.isActive()) {
                Bunkers.instance.eventHandler.service.remaining.get() * 1000L
            } else {
                (EventHandler.KOTH_START_TIME * 1000L) - (GameEngine.instance.gameHandler.getGameTime())
            }

            layout.add(2,y++,"${ChatColor.GRAY}${FormatUtil.formatIntoMMSS(time)}")
        }

        y = 7

        var x = 1

        Bunkers.instance.teamHandler.findPlayerTeams().sortedByDescending{it.isMember(player)}.forEach{

            val next: Int = if (y == 7) {
                0
            } else {
                if (x >= 2) {
                    0
                } else {
                    x + 1
                }
            }

            layout.add(x,y++,"${it.getDisplayName()} Team ${ChatColor.GRAY}[${it.getDTRDisplay()}${ChatColor.GRAY}]")

            it.members.forEach{member -> layout.add(x,y++,this.getDisplayName(player,member,it))}

            y = 14
            x = next
        }*/
    }
}
