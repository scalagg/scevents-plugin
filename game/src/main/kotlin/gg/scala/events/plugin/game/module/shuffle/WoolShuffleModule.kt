package gg.scala.events.plugin.game.module.shuffle

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
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.EventUtils
import net.evilblock.cubed.util.nms.MinecraftReflection
import net.evilblock.cubed.util.time.TimeUtil
import net.evilblock.cubed.visibility.VisibilityAction
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
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @author GrowlyX
 * @since 10/3/2022
 */
@Service
@IgnoreAutoScan
@EventBind(EventType.WoolShuffle::class)
object WoolShuffleModule : EventModule
{
    @Inject
    lateinit var engine: ScalaEventsGameEngine

    @Inject
    lateinit var plugin: ScalaEventsGamePlugin

    private var winner: String? = null

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
                    lines += "${CC.WHITE}Current color:"
                    lines += "${CC.RED}Selecting..."
                    lines += ""
                    lines += "${CC.WHITE}Time left: ${CC.GREEN}5s"
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
            Bukkit.getWorld("world"),
            294143.5, 75.000, 49050.5, 90.0F, 0.0F
        )

        Events
            .subscribe(PlayerJoinEvent::class.java)
            .handler {
                it.player.sendMessage("${CC.GRAY}${CC.STRIKE_THROUGH}--------------------------------------")
                it.player.sendMessage("${CC.GREEN}Welcome to the ${CC.GOLD}Wool Shuffle${CC.GREEN} event!")
                it.player.sendMessage("${CC.YELLOW}Please wait for others to join...")
                it.player.sendMessage("${CC.GRAY}${CC.STRIKE_THROUGH}--------------------------------------")

                if (engine.gameState == CgsGameState.STARTING)
                {
                    it.player.teleport(lobby)
                }
            }

        Events
            .subscribe(EntityDamageByEntityEvent::class.java)
            .handler {
                it.isCancelled = true
            }

        var currentRound = 1

        fun startNewRound()
        {
            val active = findAliveParticipants()

            if (active.size <= 1)
            {
                winner = active.firstOrNull()?.name
                return
            }

            Bukkit.broadcastMessage(
                "${CC.B_GOLD}✮ ${CC.YELLOW}Round #${currentRound++}: ${CC.GREEN}Selecting color..."
            )

            object : DiminutionRunnable(6)
            {
                override fun getSeconds() = listOf(
                    18000, 14400, 10800, 7200, 3600, 2700, 1800,
                    900, 600, 300, 240, 180, 120, 60, 50, 40, 30,
                    15, 10, 5, 4, 3, 2, 1
                )

                override fun onEnd()
                {
                    Bukkit.broadcastMessage(
                        "${CC.B_GOLD}✮ ${CC.YELLOW}Round #${currentRound++}: ${CC.WHITE}The color ${CC.PINK}Light Purple${CC.WHITE} was selected!"
                    )
                }

                override fun onRun()
                {

                }
            }.runTaskTimer(plugin, 0L, 20L)
        }

        fun playerLoses(loser: Player)
        {
            Schedulers
                .sync()
                .runLater({
                    loser.sendMessage("${CC.D_RED}✘ ${CC.RED}Use ${CC.WHITE}/hub${CC.RED} to return to the lobby.")
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
            .subscribe(PlayerQuitEvent::class.java)
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
