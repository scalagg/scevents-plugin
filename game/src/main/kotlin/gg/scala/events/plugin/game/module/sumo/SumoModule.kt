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
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.utils.Players
import net.evilblock.cubed.nametag.NametagInfo
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.EventUtils
import net.evilblock.cubed.util.bukkit.Tasks
import net.evilblock.cubed.util.nms.MinecraftReflection
import net.evilblock.cubed.util.text.TextUtil
import net.evilblock.cubed.util.time.TimeUtil
import net.evilblock.cubed.visibility.VisibilityAction
import net.evilblock.cubed.visibility.VisibilityHandler
import org.apache.commons.lang.time.DurationFormatUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Sound
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
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @author GrowlyX
 * @since 10/3/2022
 */
object SumoModule : EventModule
{
    val plugin by lazy {
        JavaPlugin.getPlugin(ScalaEventsGamePlugin::class.java)
    }

    private var currentlyPlaying: Pair<UUID, UUID>? = null
    private var winner: String? = null

    override val bitchRequiredSwmLoadedWorld: String?
        get() = "sumo"

    private operator fun Pair<*, *>.contains(element: Any): Boolean
    {
        return first == element || second == element
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
        override fun getTitle() = "${CC.B_PRI}SUMO"

        override fun render(
            lines: LinkedList<String>,
            player: Player, state: CgsGameState
        )
        {
            lines += ""

            when (state)
            {
                CgsGameState.STARTING ->
                {
                    lines += "${CC.WHITE}Event starting soon..."
                    lines += "${CC.PRI}${Players.all().size}/30"
                    lines += ""
                    lines += "${CC.WHITE}Starts in: ${CC.PRI}${
                        TimeUtil.formatIntoMMSS(StartingStateRunnable.PRE_START_TIME)
                    }"
                }

                CgsGameState.STARTED ->
                {
                    lines += "${CC.WHITE}Currently playing:"

                    if (currentlyPlaying == null)
                    {
                        lines += "${CC.PRI}N/A"
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
                                return "${CC.PRI}N/A ${CC.GRAY}(0ms)"
                            }

                            return "${CC.PRI}${player.name} ${CC.GRAY}(${MinecraftReflection.getPing(player)}ms)"
                        }

                        lines += formatIntoLine(first)
                        lines += "${CC.WHITE}vs."
                        lines += formatIntoLine(second)
                    }

                    lines += ""
                    lines += "${CC.WHITE}Players: ${CC.PRI}${Players.all().size}/30"
                }
                CgsGameState.ENDED ->
                {
                    lines += "Thanks for playing!"
                    lines += "${CC.PRI}${findAliveParticipants().firstOrNull()?.name ?: "N/A"} won!"
                }
                CgsGameState.WAITING -> {
                    lines += "${CC.WHITE}Waiting for players..."
                    lines += "${CC.PRI}${Players.all().size}/30"
                }
            }

            lines += ""
            lines += "${CC.GRAY}tropic.gg  ${CC.GRAY}   ${CC.GRAY}"
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
            return null
        }
    }

    object GameSnapshot : CgsGameSnapshot
    {
        override fun getExtraInformation() = listOf<String>()
    }

    override val gameSnapshot = GameSnapshot
    override val nametagProvider = NametagAdapter
    override val type = EventType.Sumo
    override val scoreboardFormatter = ScoreboardRenderer
    override val visibilityAdapter = VisibilityAdapter

    override fun configure()
    {
        val lobby = Location(
            Bukkit.getWorld("Sumo"),
            -15.5, 48.000, 34.5, -180.0F, 0.0F
        )

        Events
            .subscribe(PlayerJoinEvent::class.java)
            .handler {
                it.player.sendMessage("${CC.GRAY}${CC.STRIKE_THROUGH}${"-".repeat(53)}")
                TextUtil.sendCenteredMessage(it.player, "${CC.GREEN}Welcome to the ${CC.GOLD}Sumo${CC.GREEN} event!")
                TextUtil.sendCenteredMessage(it.player, "${CC.GRAY}Please wait for others to join...")
                it.player.sendMessage("${CC.GRAY}${CC.STRIKE_THROUGH}${"-".repeat(53)}")

                it.player.removeMetadata("frozenaf", plugin)

                Tasks.delayed(1L) {
                    VisibilityHandler.update(it.player)
                }

                if (CgsGameEngine.INSTANCE.gameState == CgsGameState.STARTING)
                {
                    it.player.teleport(lobby)
                }
            }

        Events
            .subscribe(PlayerMoveEvent::class.java)
            .filter {
                EventFilters
                    .ignoreSameBlockAndY<PlayerMoveEvent>()
                    .test(it)
            }
            .filter {
                it.player.hasMetadata("frozenaf")
            }
            .handler {
                it.player.teleport(it.from)
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
            Bukkit.getWorld("Sumo"),
            -22.5, 47.0, 5.5, -90f, 0.0f
        )

        val b = Location(
            Bukkit.getWorld("Sumo"),
            -8.5, 47.0, 5.5, 90f, 0.0f
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
                        bukkit.playSound(bukkit.location, Sound.ORB_PICKUP, 1.0F, 1.0F)
                    }

                    Bukkit.broadcastMessage("${CC.GREEN}The match has started!")
                }

                override fun onRun()
                {
                    currentlyPlaying!!.forEach {
                        val bukkit = Bukkit.getPlayer(it)
                        bukkit.playSound(bukkit.location, Sound.NOTE_STICKS, 1.0F, 1.0F)
                    }

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

                    loser.sendMessage("${CC.D_RED}✘ ${CC.RED}Use ${CC.WHITE}/hub${CC.RED} to return to the lobby.")

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
