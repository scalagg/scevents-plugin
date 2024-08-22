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
import gg.scala.events.plugin.game.module.ModuleSize
import gg.scala.events.plugin.game.module.team.Team
import gg.scala.events.shared.type.EventType
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Service
import gg.scala.flavor.service.ignore.IgnoreAutoScan
import gg.scala.lemon.LemonConstants
import gg.scala.lemon.util.QuickAccess.username
import gg.scala.lemon.util.task.DiminutionRunnable
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.utils.Players
import net.evilblock.cubed.nametag.NametagInfo
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.EventUtils
import net.evilblock.cubed.util.nms.MinecraftReflection
import net.evilblock.cubed.util.text.TextUtil
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
 * @author TastyCake & Penis
 * @date 8/22/2024
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

    var currentlyPlaying: Pair<Team, Team>? = null

    object ScoreboardRenderer : CgsGameScoreboardRenderer
    {
        override fun getTitle() = "${CC.B_PRI}Sumo"

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
                    lines += "${CC.PRI}${Players.all().size}/25"
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
                        val first = currentlyPlaying!!.first.players
                        val second = currentlyPlaying!!.second.players

                        fun formatIntoLine(player: UUID): String
                        {
                            val bukkitPlayer = Bukkit.getPlayer(player)
                                ?: return "${CC.PRI}${CC.STRIKE_THROUGH}${player.username()} ${CC.GRAY}(0ms)"

                            return "${CC.PRI}${bukkitPlayer.name} ${CC.GRAY}(${
                                MinecraftReflection.getPing(bukkitPlayer)
                            }ms)"
                        }

                        first.forEach {
                            lines += formatIntoLine(it)
                        }
                        lines += "${CC.WHITE}vs."
                        second.forEach {
                            lines += formatIntoLine(it)
                        }
                    }

                    lines += ""
                    lines += "${CC.WHITE}Players: ${CC.PRI}${Players.all().size}/25"
                }

                CgsGameState.ENDED ->
                {
                    lines += "Thanks for playing!"
                    lines += "${CC.PRI}${findAliveParticipants().firstOrNull()?.name ?: "N/A"} won!"
                }

                CgsGameState.WAITING ->
                {
                    lines += "${CC.WHITE}Waiting for players..."
                    lines += "${CC.PRI}${Players.all().size}/25"
                }
            }

            lines += ""
            lines += "${CC.GRAY}${LemonConstants.WEB_LINK}  ${CC.GRAY}   ${CC.GRAY}"
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

    private fun findAliveParticipants() = Players.all()
        .filter {
            !it.hasMetadata("spectator")
        }

    var winner: String? = null

    override val gameSnapshot = GameSnapshot
    override val nametagProvider = NametagAdapter
    override val type = EventType.Sumo
    override val scoreboardFormatter = ScoreboardRenderer
    override val visibilityAdapter = VisibilityAdapter

    override fun configure(moduleSize: ModuleSize)
    {
        val lobby = Location(
            Bukkit.getWorld("sumo"),
            14.0, 91.000, -49.500, 0.0f, 0.0f
        )

        Events
            .subscribe(PlayerJoinEvent::class.java)
            .handler {
                it.player.sendMessage("${CC.GRAY}${CC.STRIKE_THROUGH}${"-".repeat(53)}")
                TextUtil.sendCenteredMessage(it.player, "${CC.GREEN}Welcome to the ${CC.GOLD}Sumo${CC.GREEN} event!")
                TextUtil.sendCenteredMessage(it.player, "${CC.GRAY}Please wait for others to join...")
                it.player.sendMessage("${CC.GRAY}${CC.STRIKE_THROUGH}${"-".repeat(53)}")

                it.player.removeMetadata("frozenaf", plugin)

                if (engine.gameState == CgsGameState.STARTING)
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
                    if (!contains(it.damager.uniqueId))
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
            13.500, 88.000, -18.500, 180.000f, 0.0f
        )

        val b = Location(
            Bukkit.getWorld("Sumo"),
            13.500, 88.000, -30.500, 0.00f, 0.0f
        )

        val locations = listOf(a, b)

        fun startNewRound()
        {
            val active = findAliveParticipants().toMutableList()
            active.shuffle()

            if (active.size <= moduleSize.playersEachTeam)
            {
                winner = active.firstOrNull()?.name
                return
            }

            val first = active.take(moduleSize.playersEachTeam)
            val second = active.take(moduleSize.playersEachTeam)

            val firstPlayers = first.joinToString(", ") { it.name }
            val secondPlayers = second.joinToString(", ") { it.name }

            Bukkit.broadcastMessage(
                "${CC.B_GOLD}✮ ${CC.YELLOW}Round #${currentRound++}: ${CC.GREEN}${firstPlayers} ${CC.WHITE}vs. ${CC.RED}${secondPlayers}"
            )

            currentlyPlaying = Team(first.map(Player::getUniqueId)) to Team(second.map(Player::getUniqueId))

            var index = 0

            currentlyPlaying!!.forEach {
                for (player in it.players)
                {
                    val bukkit = Bukkit.getPlayer(player)
                    bukkit.setMetadata(
                        "frozenaf",
                        FixedMetadataValue(plugin, true)
                    )

                    bukkit.teleport(locations[index])
                }
                index++
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
                        for (player in it.players)
                        {
                            val bukkit = Bukkit.getPlayer(player)
                            bukkit.removeMetadata("frozenaf", plugin)
                            bukkit.playSound(bukkit.location, Sound.ORB_PICKUP, 1.0F, 1.0F)
                        }
                    }

                    Bukkit.broadcastMessage("${CC.GREEN}The match has started!")
                }

                override fun onRun()
                {
                    currentlyPlaying!!.forEach {
                        for (player in it.players)
                        {
                            val bukkit = Bukkit.getPlayer(player)
                            bukkit.playSound(bukkit.location, Sound.NOTE_STICKS, 1.0F, 1.0F)
                        }
                    }

                    Bukkit.broadcastMessage(
                        "${CC.SEC}The match starts in ${CC.PRI}${
                            DurationFormatUtils.formatDurationWords(seconds * 1000L, true, true)
                        }${CC.SEC}."
                    )
                }
            }.runTaskTimer(plugin, 0L, 20L)
        }

        fun teamLose(loser: Team)
        {
            Schedulers
                .sync()
                .runLater({
                    val opposite = getWinner(loser)

                    for (player in opposite.players)
                    {
                        val bukkit = Bukkit.getPlayer(player)
                        bukkit.teleport(lobby)
                    }

                    loser.players.forEach {
                        Bukkit.getPlayer(it)
                            .sendMessage("${CC.D_RED}✘ ${CC.RED}Use ${CC.WHITE}/hub${CC.RED} to return to the lobby.") // Instead of message command give the player an item if there team is to ve
                    }

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
                currentlyPlaying != null && contains(it.player.uniqueId)
            }
            .handler {
                if (it.to.block.isLiquid)
                {
                    val team = getTeam(it.player.uniqueId)

                    if (bothDead(team))
                    {
                        return@handler
                    }

                    it.player.health = 0.0
                    it.player.teleport(lobby)

                    teamLose(team)
                }
            }

        Events
            .subscribe(PlayerQuitEvent::class.java)
            .filter {
                currentlyPlaying != null && contains(it.player.uniqueId)
            }
            .handler {
                if (Bukkit.getOnlinePlayers().size - 1 > 1)
                {
                    teamLose(getTeam(it.player.uniqueId))
                    return@handler
                }
            }

        Events
            .subscribe(CgsGameEngine.CgsGameStartEvent::class.java)
            .handler {
                startNewRound()
            }
    }

    fun getWinner(team: Team): Team
    {
        return if (currentlyPlaying!!.first == team) currentlyPlaying!!.second else currentlyPlaying!!.first
    }

    fun <A> Pair<A, A>.forEach(function: (A) -> Unit)
    {
        first.apply(function)
        second.apply(function)
    }

    fun contains(uuid: UUID): Boolean
    {
        if (currentlyPlaying == null) return false
        var result = false
        currentlyPlaying!!.forEach {
            if (it.players.contains(uuid)) result = true
        }

        return result
    }

    fun getTeam(uuid: UUID): Team
    {
        return if (currentlyPlaying!!.first.players.contains(uuid)) currentlyPlaying!!.first else currentlyPlaying!!.second
    }

    fun bothDead(team: Team): Boolean
    {
        return currentlyPlaying!!.first.players.any {
            val bukkit = Bukkit.getPlayer(it)
            bukkit.hasMetadata("spectator")
        } || currentlyPlaying!!.second.players.any {
            val bukkit = Bukkit.getPlayer(it)
            bukkit.hasMetadata("spectator")
        }
    }
}
