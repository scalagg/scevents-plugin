package gg.scala.events.plugin.game.module.oitc

import gg.scala.cgs.common.CgsGameEngine
import gg.scala.cgs.common.player.CgsGamePlayer
import gg.scala.cgs.common.player.nametag.CgsGameNametagAdapter
import gg.scala.cgs.common.player.scoreboard.CgsGameScoreboardRenderer
import gg.scala.cgs.common.player.visibility.CgsGameVisibilityAdapter
import gg.scala.cgs.common.runnable.state.StartingStateRunnable
import gg.scala.cgs.common.snapshot.CgsGameSnapshot
import gg.scala.cgs.common.states.CgsGameState
import gg.scala.events.plugin.game.ScalaEventsGameEngine
import gg.scala.events.plugin.game.module.EventModule
import gg.scala.events.plugin.game.module.EventBind
import gg.scala.events.shared.type.EventType
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Service
import gg.scala.flavor.service.ignore.IgnoreAutoScan
import gg.scala.lemon.util.QuickAccess
import gg.scala.lemon.util.task.DiminutionRunnable
import me.lucko.helper.Events
import me.lucko.helper.terminable.composite.CompositeTerminable
import me.lucko.helper.utils.Players
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.EventUtils
import net.evilblock.cubed.util.time.TimeUtil
import net.evilblock.cubed.visibility.VisibilityAction
import org.apache.commons.lang.time.DurationFormatUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
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

    var secondsUntilInvincibilityEnds = -1
    var secondsUntilGameEnds = -1

    object ScoreboardRenderer : CgsGameScoreboardRenderer
    {
        override fun getTitle() = "${CC.B_YELLOW}OITC Event"

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
                lines += "${CC.YELLOW}www.liftgate.io"
            }

            if (state == CgsGameState.STARTED)
            {
                if (secondsUntilInvincibilityEnds != -1)
                {
                    lines += ""
                    lines += "${CC.WHITE}Invincibility ends: ${CC.GREEN}${
                        TimeUtil.formatIntoMMSS(secondsUntilInvincibilityEnds)
                    }"
                } else if (secondsUntilGameEnds != -1)
                {
                    lines += ""
                    lines += "${CC.WHITE}Game ends: ${CC.GREEN}${
                        TimeUtil.formatIntoMMSS(secondsUntilGameEnds)
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
                lines += "${CC.YELLOW}www.liftgate.io"
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
        val spawnLocation = type.info.gameModes
            .first().getArenas().first()
            .getPreLobbyLocation()

        Events
            .subscribe(PlayerJoinEvent::class.java)
            .handler {
                it.player.sendMessage("Welcome to OITC")
            }

        Events
            .subscribe(PlayerDeathEvent::class.java)
            .handler {
                it.drops.clear()

                kotlin.runCatching {
                    kills[it.entity.killer.uniqueId] =
                        kills[it.entity.killer.uniqueId]!! + 1
                }

                it.entity.spigot().respawn()
                it.entity.teleport(spawnLocation)

                it.entity.inventory.addItem(
                    ItemStack(Material.STONE_SWORD)
                )
            }

        Events
            .subscribe(CgsGameEngine.CgsGameStartEvent::class.java)
            .handler {
                Players.all()
                    .forEach { player ->
                        player.teleport(spawnLocation)

                        player.inventory.addItem(
                            ItemStack(Material.STONE_SWORD)
                        )

                        kills[player.uniqueId] = 0
                    }

                val terminable = CompositeTerminable.create()

                Events.subscribe(EntityDamageEvent::class.java)
                    .handler {
                        it.isCancelled = true
                    }
                    .bindWith(terminable)

                secondsUntilInvincibilityEnds = 16

                object : DiminutionRunnable(secondsUntilInvincibilityEnds)
                {
                    override fun getSeconds() = listOf(15, 10, 5, 4, 3, 2, 1)

                    override fun onEnd()
                    {
                        terminable.closeAndReportException()
                        secondsUntilInvincibilityEnds = -1

                        Bukkit.broadcastMessage("${CC.SEC}Invincibility is now disabled!")

                        secondsUntilGameEnds = (20 * 60 * 5) + 1

                        object : DiminutionRunnable(secondsUntilGameEnds)
                        {
                            override fun getSeconds() = listOf(
                                18000, 14400, 10800, 7200, 3600, 2700, 1800,
                                900, 600, 300, 240, 180, 120, 60, 50, 40, 30,
                                15, 10, 5, 4, 3, 2, 1
                            )

                            override fun onEnd()
                            {
                                terminable.closeAndReportException()
                                secondsUntilGameEnds = -1

                                engine.gameState = CgsGameState.ENDED
                            }

                            override fun onRun()
                            {
                                secondsUntilGameEnds = seconds

                                Bukkit.broadcastMessage("${CC.SEC}The event will end in ${CC.PRI}${
                                    DurationFormatUtils.formatDurationWords(seconds * 1000L, true, true)
                                }${CC.SEC}.")
                            }
                        }.runTaskTimerAsynchronously(engine.plugin, 0L, 20L)
                    }

                    override fun onRun()
                    {
                        secondsUntilInvincibilityEnds = seconds

                        Bukkit.broadcastMessage("${CC.SEC}Invincibility will wear off in ${CC.PRI}${
                            DurationFormatUtils.formatDurationWords(seconds * 1000L, true, true)
                        }${CC.SEC}.")
                    }
                }.runTaskTimerAsynchronously(engine.plugin, 0L, 20L)
            }

        Events
            .subscribe(PlayerMoveEvent::class.java)
            .filter(EventUtils::hasPlayerMoved)
            .handler {
                if (it.player.location.y < 20)
                {
                    if (engine.gameState == CgsGameState.STARTED)
                    {
                        it.player.health = 0.0
                        return@handler
                    }

                    it.player.teleport(spawnLocation)
                }
            }
    }
}
