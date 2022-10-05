package gg.scala.events.plugin.game.module.oitc

import gg.scala.cgs.common.CgsGameEngine
import gg.scala.cgs.common.player.CgsGamePlayer
import gg.scala.cgs.common.player.nametag.CgsGameNametagAdapter
import gg.scala.cgs.common.player.scoreboard.CgsGameScoreboardRenderer
import gg.scala.cgs.common.player.visibility.CgsGameVisibilityAdapter
import gg.scala.cgs.common.runnable.state.StartingStateRunnable
import gg.scala.cgs.common.snapshot.CgsGameSnapshot
import gg.scala.cgs.common.states.CgsGameState
import gg.scala.cgs.common.teams.CgsGameTeamService
import gg.scala.events.plugin.game.ScalaEventsGameEngine
import gg.scala.events.plugin.game.module.EventBind
import gg.scala.events.plugin.game.module.EventModule
import gg.scala.events.shared.type.EventType
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Service
import gg.scala.flavor.service.ignore.IgnoreAutoScan
import gg.scala.lemon.util.QuickAccess
import gg.scala.lemon.util.task.DiminutionRunnable
import me.lucko.helper.Events
import me.lucko.helper.utils.Players
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.EventUtils
import net.evilblock.cubed.util.bukkit.Tasks
import net.evilblock.cubed.util.bukkit.cuboid.Cuboid
import net.evilblock.cubed.util.time.TimeUtil
import net.evilblock.cubed.visibility.VisibilityAction
import org.apache.commons.lang.time.DurationFormatUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Arrow
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.metadata.FixedMetadataValue
import java.util.*

/**
 * @author GrowlyX
 * @since 10/3/2022
 */
@Service
@IgnoreAutoScan
@EventBind(EventType.OneInTheChamber::class)
object OneInTheChamberModule : EventModule
{
    @Inject
    lateinit var engine: ScalaEventsGameEngine

    val kills = mutableMapOf<UUID, Int>()

    var secondsUntilInvincibilityEnds = object : DiminutionRunnable(16)
    {
        override fun getSeconds() = listOf(15, 10, 5, 4, 3, 2, 1)

        override fun onEnd()
        {
            Players.all()
                .forEach { player ->
                    player.removeMetadata(
                        "invincible", engine.plugin
                    )
                }

            Bukkit.broadcastMessage("${CC.B_GREEN}Invincibility is now disabled! Good luck!")

            secondsUntilGameEnds.runTaskTimerAsynchronously(engine.plugin, 0L, 20L)
        }

        override fun onRun()
        {
            Bukkit.broadcastMessage(
                "${CC.SEC}Invincibility will wear off in ${CC.PRI}${
                    DurationFormatUtils.formatDurationWords(seconds * 1000L, true, true)
                }${CC.SEC}."
            )
        }
    }

    var secondsUntilGameEnds = object : DiminutionRunnable(301)
    {
        override fun getSeconds() = listOf(
            18000, 14400, 10800, 7200, 3600, 2700, 1800,
            900, 600, 300, 240, 180, 120, 60, 50, 40, 30,
            15, 10, 5, 4, 3, 2, 1
        )

        override fun onEnd()
        {
            val winningPlayer = kills.entries
                .map {
                    Bukkit.getPlayer(it.key) to it.value
                }
                .filter { it.first != null }
                .maxByOrNull { it.second }
                ?.first

            Tasks.sync {
                if (winningPlayer != null)
                {
                    engine.winningTeam = CgsGameTeamService
                        .getTeamOf(winningPlayer)
                        ?: CgsGameTeamService.teams.values.first()
                } else
                {
                    engine.winningTeam = CgsGameTeamService.teams.values.first()
                }

                engine.gameState = CgsGameState.ENDED
            }
        }

        override fun onRun()
        {
            Bukkit.broadcastMessage(
                "${CC.SEC}The event will end in ${CC.PRI}${
                    DurationFormatUtils.formatDurationWords(seconds * 1000L, true, true)
                }${CC.SEC}."
            )
        }
    }

    object ScoreboardRenderer : CgsGameScoreboardRenderer
    {
        override fun getTitle() = "${CC.B_AQUA}OITC Event"

        override fun render(
            lines: LinkedList<String>,
            player: Player, state: CgsGameState
        )
        {
            if (state == CgsGameState.STARTING)
            {
                lines += ""
                lines += "${CC.WHITE}Starts in: ${CC.GREEN}${
                    TimeUtil.formatIntoMMSS(StartingStateRunnable.PRE_START_TIME)
                }"
                lines += ""
                lines += "${CC.AQUA}www.minequest.gg"
            }

            if (state == CgsGameState.STARTED)
            {
                if (player.hasMetadata("spectator"))
                {
                    lines += ""
                }

                if (secondsUntilInvincibilityEnds.seconds > 0)
                {
                    lines += ""
                    lines += "${CC.WHITE}Invincibility ends: ${CC.GREEN}${
                        TimeUtil.formatIntoMMSS(secondsUntilInvincibilityEnds.seconds)
                    }"
                } else if (secondsUntilGameEnds.seconds > 0)
                {
                    lines += ""
                    lines += "${CC.WHITE}Game ends: ${CC.GREEN}${
                        TimeUtil.formatIntoMMSS(secondsUntilGameEnds.seconds)
                    }"
                }

                lines += ""
                lines += "${CC.WHITE}Top 3 kills:"

                var index = 0

                val entries = kills.entries
                    .sortedByDescending { it.value }
                    .take(3)

                for (entry in entries)
                {
                    lines += "${CC.GREEN}#${++index}. ${
                        QuickAccess.coloredName(entry.key)
                    }${CC.WHITE}: ${entry.value}"
                }

                lines += ""
                lines += "${CC.AQUA}www.minequest.gg"
            }
        }
    }

    object VisibilityAdapter : CgsGameVisibilityAdapter
    {
        override fun computeVisibility(
            viewer: CgsGamePlayer,
            target: CgsGamePlayer
        ) = VisibilityAction.NEUTRAL
    }

    object NametagAdapter : CgsGameNametagAdapter
    {
        override fun computeNametag(
            viewer: CgsGamePlayer,
            target: CgsGamePlayer
        ) = null
    }

    object GameSnapshot : CgsGameSnapshot
    {
        override fun getExtraInformation(): List<String>
        {
            val info = mutableListOf<String>()

            var index = 0
            val entries = kills.entries
                .sortedByDescending { it.value }
                .take(3)

            for (entry in entries)
            {
                info += "  ${CC.GREEN}#${++index}. ${
                    QuickAccess.coloredName(entry.key)
                }${CC.WHITE}: ${entry.value}"
            }

            return info
        }
    }

    override val gameSnapshot = GameSnapshot
    override val nametagProvider = NametagAdapter
    override val type = EventType.OneInTheChamber
    override val scoreboardFormatter = ScoreboardRenderer
    override val visibilityAdapter = VisibilityAdapter

    override fun configure()
    {
        val cuboid = Cuboid(
            Location(Bukkit.getWorld("world"), 121.232, 74.63, 47.179),
            Location(Bukkit.getWorld("world"), 182.782, 63.63, -8.179),
        )

        val locations = cuboid.getBlockLocations()

        Events
            .subscribe(PlayerJoinEvent::class.java)
            .handler {
                it.player.inventory.clear()
                it.player.sendMessage("Welcome to OITC")
            }

        fun populateInventory(player: Player)
        {
            player.inventory.clear()
            player.inventory.addItem(
                ItemStack(Material.STONE_SWORD)
            )
            player.inventory.addItem(
                ItemStack(Material.BOW)
            )
            player.inventory.addItem(
                ItemStack(Material.ARROW)
            )
        }

        Events
            .subscribe(EntityDamageByEntityEvent::class.java)
            .handler { event ->
                val player = event.entity
                val arrow = event.damager

                if (arrow is Arrow && player is Player)
                {
                    val shooter = arrow.shooter

                    if (shooter is Player)
                    {
                        if (arrow.getName() != shooter.name)
                        {
                            // it kills.
                            event.damage = 100.0
                        }
                    }
                }
            }

        Events
            .subscribe(PlayerDeathEvent::class.java)
            .handler {
                it.drops.clear()

                kotlin.runCatching {
                    kills[it.entity.killer.uniqueId] =
                        kills[it.entity.killer.uniqueId]!! + 1

                    if (it.entity.killer.isOnline)
                    {
                        it.entity.killer.inventory.addItem(
                            ItemStack(Material.ARROW)
                        )
                    }
                }

                Tasks.delayed(1L) {
                    val thing = locations.random()
                    thing.y = (Bukkit.getWorld("world")
                        .getHighestBlockAt(thing)
                        .y + 3).toDouble()

                    it.entity.spigot().respawn()
                    it.entity.teleport(
                        locations.random()
                    )

                    populateInventory(it.entity)
                }
            }

        Events
            .subscribe(CgsGameEngine.CgsGameStartEvent::class.java)
            .handler {
                Players.all()
                    .forEach { player ->
                        val thing = locations.random()
                        thing.y = (Bukkit.getWorld("world")
                            .getHighestBlockAt(thing)
                            .y + 3).toDouble()

                        player.teleport(
                            locations.random()
                        )
                        populateInventory(player)

                        kills[player.uniqueId] = 0

                        player.setMetadata(
                            "invincible",
                            FixedMetadataValue(engine.plugin, true)
                        )
                    }

                secondsUntilInvincibilityEnds.runTaskTimerAsynchronously(engine.plugin, 0L, 20L)
            }
    }
}
