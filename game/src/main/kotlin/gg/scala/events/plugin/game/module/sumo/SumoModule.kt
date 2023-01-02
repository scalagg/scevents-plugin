package gg.scala.events.plugin.game.module.sumo

import gg.scala.cgs.common.CgsGameEngine
import gg.scala.cgs.common.player.CgsGamePlayer
import gg.scala.cgs.common.player.nametag.CgsGameNametagAdapter
import gg.scala.cgs.common.player.scoreboard.CgsGameScoreboardRenderer
import gg.scala.cgs.common.player.visibility.CgsGameVisibilityAdapter
import gg.scala.cgs.common.runnable.state.StartingStateRunnable
import gg.scala.cgs.common.snapshot.CgsGameSnapshot
import gg.scala.cgs.common.states.CgsGameState
import gg.scala.events.plugin.game.ScalaEventsGameEngine
import gg.scala.events.plugin.game.ScalaEventsGamePlugin
import gg.scala.events.plugin.game.module.EventBind
import gg.scala.events.plugin.game.module.EventModule
import gg.scala.events.shared.type.EventType
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Service
import gg.scala.flavor.service.ignore.IgnoreAutoScan
import gg.scala.lemon.util.task.DiminutionRunnable
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.utils.Players
import net.evilblock.cubed.nametag.NametagInfo
import net.evilblock.cubed.nametag.NametagProvider
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.EventUtils
import net.evilblock.cubed.util.nms.MinecraftReflection
import net.evilblock.cubed.util.time.TimeUtil
import net.evilblock.cubed.visibility.VisibilityAction
import org.apache.commons.lang.time.DurationFormatUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.metadata.FixedMetadataValue
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @author GrowlyX
 * @since 10/3/2022
 */
@Service
@IgnoreAutoScan
@EventBind(EventType.Sumo::class)
object SumoModule : EventModule
{
    @Inject
    lateinit var engine: ScalaEventsGameEngine

    @Inject
    lateinit var plugin: ScalaEventsGamePlugin

    private var currentlyPlaying: Pair<UUID, UUID>? = null
    private var winner: String? = null

    private operator fun <A, B> Pair<A, B>.contains(uniqueId: B): Boolean
    {
        return first == uniqueId || second == uniqueId
    }

    private fun <A> Pair<A, A>.forEach(function: (A) -> Unit)
    {
        first.apply(function)
        second.apply(function)
    }

    private fun findAliveParticipants() = Players.all()
        .filter {
            !it.hasMetadata("spectator")
        }

    object ScoreboardRenderer : CgsGameScoreboardRenderer
    {
        override fun getTitle() = "${CC.B_PRI}Events ${CC.GRAY}(BETA)"

        override fun render(
            lines: LinkedList<String>,
            player: Player, state: CgsGameState
        )
        {
            lines += "${CC.GRAY}${CC.STRIKE_THROUGH}------------------"

            when (state)
            {
                CgsGameState.STARTING ->
                {
                    lines += "${CC.WHITE}Event starting soon..."
                    lines += "${CC.GOLD}${Players.all().size}/30"
                    lines += ""
                    lines += "${CC.WHITE}Starts in: ${CC.GOLD}${
                        TimeUtil.formatIntoMMSS(StartingStateRunnable.PRE_START_TIME)
                    }"
                }

                CgsGameState.STARTED ->
                {
                    lines += "${CC.WHITE}Currently playing:"

                    if (currentlyPlaying == null)
                    {
                        lines += "${CC.GOLD}N/A"
                    } else
                    {
                        val first = Bukkit
                            .getPlayer(
                                currentlyPlaying!!.first
                            )

                        val second = Bukkit
                            .getPlayer(
                                currentlyPlaying!!.second
                            )

                        fun formatIntoLine(player: Player?): String
                        {
                            if (player == null)
                            {
                                return "${CC.GOLD}N/A ${CC.GRAY}(0ms)"
                            }

                            return "${CC.GOLD}${player.name} ${CC.GRAY}(${MinecraftReflection.getPing(player)}ms)"
                        }

                        lines += formatIntoLine(first)
                        lines += "${CC.WHITE}vs."
                        lines += formatIntoLine(second)
                    }

                    lines += ""
                    lines += "${CC.WHITE}Players: ${CC.GOLD}${Players.all().size}/30"
                }
                CgsGameState.ENDED ->
                {
                    lines += "Thanks for playing!"
                    lines += "${CC.GOLD}${findAliveParticipants().firstOrNull()?.name ?: "N/A"} won!"
                }
                CgsGameState.WAITING -> {
                    lines += "${CC.WHITE}Waiting for players..."
                    lines += "${CC.GOLD}${Players.all().size}/30"
                }
            }

            lines += ""
            lines += "${CC.GOLD}tropic.gg"
            lines += "${CC.GRAY}${CC.STRIKE_THROUGH}------------------"
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
        ): NametagInfo?
        {
            if (currentlyPlaying == null)
            {
                return null
            }

            if (target.uniqueId in currentlyPlaying!!)
            {
                return NametagProvider.createNametag(CC.RED, "")
            }

            return null
        }
    }

    object GameSnapshot : CgsGameSnapshot
    {
        override fun getExtraInformation(): List<String>
        {
            val active = findAliveParticipants().firstOrNull()?.name

            return listOf(
                "${CC.PINK} ${if (active == null) "N/A" else winner} won the Sumo event!"
            )
        }
    }

    override val gameSnapshot = GameSnapshot
    override val nametagProvider = NametagAdapter
    override val type = EventType.Sumo
    override val scoreboardFormatter = ScoreboardRenderer
    override val visibilityAdapter = VisibilityAdapter

    override fun configure()
    {
        val lobby = Location(
            Bukkit.getWorld("world"),
            294143.5, 75.000, 49050.5, 90.0F, 0.0F
        )

        Events
            .subscribe(PlayerJoinEvent::class.java)
            .handler {
                it.player.sendMessage("${CC.GRAY}${CC.STRIKE_THROUGH}--------------------------------------")
                it.player.sendMessage("${CC.GREEN}Welcome to the ${CC.GOLD}Sumo${CC.GREEN} event!")
                it.player.sendMessage("${CC.YELLOW}Please wait for others to join...")
                it.player.sendMessage("${CC.GRAY}${CC.STRIKE_THROUGH}--------------------------------------")

                if (engine.gameState == CgsGameState.STARTING)
                {
                    it.player.teleport(lobby)
                }
            }

        Events
            .subscribe(PlayerMoveEvent::class.java)
            .filter(EventUtils::hasPlayerMoved)
            .filter {
                it.player.hasMetadata("frozenaf")
            }
            .handler {
                val location = it.player.location.clone()
                    .apply {
                        x = x.toInt() + 0.500
                        z = z.toInt() + 0.500
                    }

                it.player.teleport(location)
            }

        Events
            .subscribe(EntityDamageByEntityEvent::class.java)
            .handler {
                if (this.currentlyPlaying == null)
                {
                    it.isCancelled = true
                    return@handler
                }

                if (it.damager != null)
                {
                    if (it.damager.uniqueId !in this.currentlyPlaying!!)
                    {
                        it.isCancelled = true
                        return@handler
                    }
                }

                it.damage = 0.0
            }

        var currentRound = 1

        val a = Location(
            Bukkit.getWorld("world"),
            294111.500, 73.000, 49050.500, -90.0F, 0.0F
        )

        val b = Location(
            Bukkit.getWorld("world"),
            294121.500, 73.000, 49050.500, 90.0F, 0.0F
        )

        val locations = listOf(a, b)

        fun startNewRound()
        {
            val active = findAliveParticipants()

            if (active.size <= 1)
            {
                winner = active.firstOrNull()?.name
                return
            }

            val first = active.random()

            val second = active
                .filter {
                    it.uniqueId != first.uniqueId
                }
                .random()

            Bukkit.broadcastMessage(
                "${CC.B_GOLD}✮ ${CC.YELLOW}Round #${currentRound++}: ${CC.GREEN}${first.name} ${CC.WHITE}vs. ${CC.RED}${second.name}"
            )

            currentlyPlaying = first.uniqueId to second.uniqueId

            var index = 0

            currentlyPlaying!!.forEach {
                val bukkit = Bukkit.getPlayer(it)
                bukkit.setMetadata(
                    "frozenaf",
                    FixedMetadataValue(plugin, true)
                )

                bukkit.teleport(locations[index++])
            }

            object : DiminutionRunnable(6)
            {
                override fun getSeconds() = listOf(
                    18000, 14400, 10800, 7200, 3600, 2700, 1800,
                    900, 600, 300, 240, 180, 120, 60, 50, 40, 30,
                    15, 10, 5, 4, 3, 2, 1
                )

                override fun onEnd()
                {
                    currentlyPlaying!!.forEach {
                        val bukkit = Bukkit.getPlayer(it)
                        bukkit.removeMetadata("frozenaf", plugin)
                    }

                    Bukkit.broadcastMessage("${CC.GREEN}The match has started!")
                }

                override fun onRun()
                {
                    Bukkit.broadcastMessage(
                        "${CC.SEC}The match starts in ${CC.PRI}${
                            DurationFormatUtils.formatDurationWords(seconds * 1000L, true, true)
                        }${CC.SEC}."
                    )
                }
            }.runTaskTimer(plugin, 0L, 20L)
        }

        fun playerLoses(loser: Player)
        {
            Schedulers
                .sync()
                .runLater({
                    val opposite = currentlyPlaying!!
                        .toList()
                        .filterNot { not ->
                            loser.player.uniqueId == not
                        }

                    val bukkit = Bukkit.getPlayer(opposite.first())
                    bukkit.teleport(lobby)

                    loser.sendMessage("${CC.RED}✘ ${CC.RED}Use ${CC.WHITE}/hub${CC.RED} to return to the lobby.")

                    Schedulers
                        .sync()
                        .runLater({
                            startNewRound()
                        }, 1L, TimeUnit.SECONDS)
                }, 3L, TimeUnit.SECONDS)
        }

        listOf(BlockBreakEvent::class.java, BlockPlaceEvent::class.java, FoodLevelChangeEvent::class.java)
            .forEach {
                Events
                    .subscribe(it)
                    .handler { event ->
                        event.isCancelled = true
                    }
            }

        Events
            .subscribe(PlayerInteractEvent::class.java)
            .filter(PlayerInteractEvent::hasBlock)
            .handler { event ->
                event.isCancelled = true
            }

        Events
            .subscribe(PlayerMoveEvent::class.java)
            .filter(EventUtils::hasPlayerMoved)
            .filter {
                currentlyPlaying != null && it.player.uniqueId in currentlyPlaying!!
            }
            .handler {
                if (it.to.block.isLiquid)
                {
                    val deadAlready = currentlyPlaying!!
                        .toList().any { id ->
                            val bukkit = Bukkit.getPlayer(id)
                            bukkit.hasMetadata("spectator")
                        }

                    if (deadAlready)
                    {
                        return@handler
                    }

                    it.player.health = 0.0
                    it.player.teleport(lobby)

                    playerLoses(it.player)
                }
            }

        Events
            .subscribe(PlayerQuitEvent::class.java)
            .filter {
                currentlyPlaying != null && it.player.uniqueId in currentlyPlaying!!
            }
            .handler {
                if (Bukkit.getOnlinePlayers().size - 1 > 1)
                {
                    playerLoses(it.player)
                    return@handler
                }
            }

        Events
            .subscribe(CgsGameEngine.CgsGameStartEvent::class.java)
            .handler {
                startNewRound()
            }
    }
}
