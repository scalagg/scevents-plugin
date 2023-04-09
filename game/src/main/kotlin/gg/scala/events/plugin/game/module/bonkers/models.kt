package gg.scala.events.plugin.game.module.bonkers

import gg.scala.cgs.common.CgsGameEngine
import gg.scala.cgs.common.player.CgsGamePlayer
import gg.scala.cgs.common.player.handler.CgsPlayerHandler
import gg.scala.cgs.common.player.nametag.CgsGameNametagAdapter
import gg.scala.cgs.common.player.scoreboard.CgsGameScoreboardRenderer
import gg.scala.cgs.common.player.visibility.CgsGameVisibilityAdapter
import gg.scala.cgs.common.runnable.state.StartingStateRunnable
import gg.scala.cgs.common.snapshot.CgsGameSnapshot
import gg.scala.cgs.common.states.CgsGameState
import gg.scala.cgs.common.teams.CgsGameTeam
import gg.scala.cgs.common.teams.CgsGameTeamService
import gg.scala.lemon.LemonConstants
import gg.scala.lemon.util.QuickAccess.username
import gg.scala.lemon.util.task.DiminutionRunnable
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.terminable.composite.CompositeTerminable
import net.evilblock.cubed.nametag.NametagInfo
import net.evilblock.cubed.nametag.NametagProvider
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.EventUtils
import net.evilblock.cubed.util.bukkit.Rainbow
import net.evilblock.cubed.util.bukkit.Tasks
import net.evilblock.cubed.util.bukkit.cuboid.Cuboid
import net.evilblock.cubed.util.math.Numbers
import net.evilblock.cubed.util.time.TimeUtil
import net.evilblock.cubed.visibility.VisibilityAction
import org.apache.commons.lang.time.DurationFormatUtils
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author GrowlyX
 * @since 3/31/2023
 */
data class Account(
    var balance: Int = 500
)

data class TeamData(
    val teamId: Int,
    val teamName: String,
    val color: ChatColor,
    val spawnLocation: Location,
    val region: Cuboid,
    var dtr: Double = 5.0
)
{
    fun colored() = "$color$teamName"
    fun raidable() = dtr <= 0.0
}

fun createRunner(
    seconds: Int,
    end: () -> Unit,
    update: (Int) -> Unit
) = object : DiminutionRunnable(seconds + 1)
{
    override fun getSeconds() = listOf(
        18000, 14400, 10800, 7200, 3600, 2700, 1800,
        900, 600, 300, 240, 180, 120, 60, 50, 40, 30,
        15, 10, 5, 4, 3, 2, 1
    )

    override fun onEnd()
    {
        end()
    }

    override fun onRun()
    {
        update(this.seconds)
    }
}.apply {
    runTaskTimer(BonkersModule.plugin, 0L, 20L)
}

data class GenericMovementEvent(
    val player: Player,
    val to: Location,
    val from: Location
)

object ScoreboardRenderer : CgsGameScoreboardRenderer
{
    private val format = SimpleDateFormat()

    override fun getTitle() = "${CC.B_GOLD}Bunkers"

    override fun render(
        lines: LinkedList<String>,
        player: Player, state: CgsGameState
    )
    {
        lines += ""

        if (state == CgsGameState.STARTING)
        {
            lines += "${CC.GOLD}Game starts in:"
            lines += "${
                StartingStateRunnable.PRE_START_TIME 
            } seconds"
            lines += ""
            lines += "${CC.WHITE}Selected team: ${
                player.team()?.data()?.colored()
            }"
            lines += "${CC.WHITE}Players: ${CC.GOLD}${
                Bukkit.getOnlinePlayers().size
            }/${
                Bukkit.getMaxPlayers()
            }"
        } else if (state.isAfter(CgsGameState.STARTED))
        {
            lines += "Game Time: ${CC.GOLD}${
                TimeUtil.formatIntoMMSS(
                    ((System.currentTimeMillis() - CgsGameEngine.INSTANCE.gameStart) / 1000).toInt()
                )
            }"
            lines += "Balance: ${CC.GOLD}${
                Numbers.format(
                    BonkersModule.playerAccounts[player.uniqueId]?.balance ?: 0
                )
            }"

            player.team()?.data()
                ?.apply {
                    lines += ""
                    lines += "DTR: ${CC.GREEN}${dtr}"
                    lines += "Team: ${colored()}"
                }

            lines += "Kills: ${CC.GOLD}${
                runCatching {
                    CgsGameEngine.INSTANCE
                        .getStatistics(CgsPlayerHandler.find(player)!!)
                        .gameKills.value
                }.getOrNull() ?: 0
            }"
        } else if (state == CgsGameState.WAITING)
        {
            lines += "${CC.GOLD}Waiting:"
            lines += "${
                Bukkit.getMaxPlayers() - Bukkit.getOnlinePlayers().size
            } more players."
            lines += ""
            lines += "${CC.WHITE}Selected team: ${
                player.team()?.data()?.colored()
            }"
            lines += "${CC.WHITE}Players: ${CC.GOLD}${
                Bukkit.getOnlinePlayers().size
            }/${
                Bukkit.getMaxPlayers()
            }"
        }

        lines += ""
        lines += "${CC.GRAY}www.tropic.gg  ${CC.GRAY}   ${CC.GRAY}"
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
        val thing = Bukkit.getPlayer(viewer.uniqueId)
            ?: return null

        return NametagProvider.createNametag(
            thing.team()?.data()?.color?.toString() ?: CC.GRAY,
            ""
        )
    }
}

object GameSnapshot : CgsGameSnapshot
{
    override fun getExtraInformation() = listOf<String>()
}

fun Player.team() = CgsGameTeamService.getTeamOf(this)
fun CgsGameTeam.data() = BonkersModule.mappings.entries
    .firstOrNull {
        it.value.teamId == this.id
    }
    ?.value

fun Player.findClaim() = BonkersModule.claims.values
    .firstOrNull {
        it.contains(this)
    }

fun Player.findClaimWithName() = BonkersModule.claims.entries
    .firstOrNull {
        it.value.contains(this)
    }

fun Player.findClaimContextual(context: String) = BonkersModule.claims.entries
    .filter { it.key.contains(context) }
    .firstOrNull {
        it.value.contains(this)
    }


fun Block.findClaim() = BonkersModule.claims.entries
    .firstOrNull {
        it.value.contains(this)
    }


fun Block.findClaimContextual(context: String) = BonkersModule.claims.entries
    .filter { it.key.contains(context) }
    .firstOrNull {
        it.value.contains(this)
    }

fun Player.colorfulName() = "${this.team()?.data()?.color ?: ChatColor.WHITE}$name"

data class ConquestKothMix(
    val region: Cuboid,
    val seconds: Int,
    val startNewOneAfter: Boolean = false,
    val specialCentral: Boolean = false
)
{
    var currentHolder: UUID? = null
    var previousHolderTeamId: Int? = null

    var futureHolders = LinkedList<UUID>()

    val terminable = CompositeTerminable.create()
    var runner: DiminutionRunnable? = null

    fun Location.stringify() = "($x, $y, $z)"
    fun special() = if (specialCentral) "Central " else ""

    fun createNewRunner() = createRunner(
        seconds = this.seconds,
        end = {
            Bukkit.broadcastMessage(
                "$PREFIX_KOTH${CC.B_GREEN}${special()}Koth ${region.lowerNE.stringify()} was completed!"
            )

            if (currentHolder == null)
            {
                Bukkit.broadcastMessage("$PREFIX_KOTH${CC.WHITE}No one captured.")
            } else
            {
                val holder = Bukkit.getPlayer(currentHolder)
                val holderTeam = holder?.team()?.data()

                Bukkit.broadcastMessage(
                    "$PREFIX_KOTH${CC.WHITE}${
                        Bukkit.getPlayer(currentHolder)?.colorfulName()
                    }${CC.GRAY} captured koth for ${
                        holderTeam?.color
                    }${
                        holderTeam?.teamName
                    }${CC.GRAY}!"
                )
            }

            Bukkit.broadcastMessage(
                "$PREFIX_KOTH${CC.GOLD}Loot is dropping at the center of ${special()}Koth ${region.lowerNE.stringify()} in 5 seconds!"
            )

            Schedulers
                .sync()
                .runLater({
                    // TODO: drop loot
                }, 100L)

            if (startNewOneAfter)
            {
                Schedulers.sync()
                    .runLater({
                        BonkersModule.startNewConquestEvent(BonkersModule.conquests.random())
                    }, 30 * 20)
            }

            if (specialCentral)
            {
                Tasks.sync {
                    CgsGameEngine.INSTANCE.winningTeam = Bukkit.getPlayer(currentHolder)?.team()!!
                    CgsGameEngine.INSTANCE.gameState = CgsGameState.ENDED
                }
            }

            terminable.closeAndReportException()
        },
        update = {
            Bukkit.broadcastMessage(
                "$PREFIX_KOTH${CC.GREEN}${special()}Koth ${region.lowerNE.stringify()} will capture in ${
                    DurationFormatUtils.formatDurationWords(it * 1000L, true, true)
                }"
            )
        }
    ).apply {
        runner?.cancel()
        runner = this
    }

    fun configure()
    {
        Bukkit.broadcastMessage("$PREFIX_KOTH${CC.GREEN}${special()}Koth has started at ${region.lowerNE.stringify()}!")

        Schedulers
            .async()
            .runRepeating({ _ ->
                if (currentHolder == null)
                {
                    if (futureHolders.isEmpty())
                    {
                        // no other players to capture koth
                        return@runRepeating
                    }

                    val newHolder = futureHolders.pop()
                    val bukkitPlayer = Bukkit.getPlayer(newHolder)
                        ?: return@runRepeating

                    currentHolder = newHolder
                    bukkitPlayer.sendMessage(
                        "${CC.GREEN}You are now capping ${special()}Koth for ${
                            bukkitPlayer.team()?.data()?.colored()
                        }${CC.GREEN}!"
                    )

                    Bukkit.broadcastMessage(
                        "$PREFIX_KOTH${CC.WHITE}${
                            bukkitPlayer.team()?.data()?.colored()
                        }${CC.GREEN} is now capping ${special()}Koth!"
                    )

                    if (runner == null)
                    {
                        createNewRunner()
                    } else
                    {
                        runner?.cancel()
                        runner = null

                        Bukkit.broadcastMessage("$PREFIX_KOTH${CC.YELLOW}Koth is continuing under ${
                            bukkitPlayer.team()?.data()?.colored()
                        }'s${CC.YELLOW} capture!")

                        createNewRunner()
                    }
                }
            }, 0L, 10L)
            .bindWith(terminable)

        Events
            .subscribe(PlayerQuitEvent::class.java)
            .handler {
                currentHolder = null
                previousHolderTeamId = it
                    .player?.team()?.data()?.teamId
                futureHolders.remove(it.player.uniqueId)
            }

        fun name() = "${CC.GOLD}${special()}Koth ${CC.GRAY}${region.lowerNE.stringify()}"

        fun handleGenericMovementEvent(it: GenericMovementEvent)
        {
            if (it.player.hasMetadata("spectator"))
            {
                return
            }

            if (region.contains(it.to))
            {
                if (currentHolder != it.player.uniqueId)
                {
                    if (!futureHolders.contains(it.player.uniqueId))
                    {
                        futureHolders.push(it.player.uniqueId)
                        it.player.sendMessage("${CC.AQUA}HEYY GIRLBOSS. ${CC.YELLOW}You entered: ${name()}")

                        if (currentHolder != null)
                        {
                            it.player.sendMessage("${CC.WHITE}You're queued up to capture if ${CC.BOLD}${currentHolder!!.username()}${CC.WHITE} leaves. (#${futureHolders.size})")
                        }
                    }
                }
            } else
            {
                if (currentHolder == it.player.uniqueId)
                {
                    currentHolder = null
                    previousHolderTeamId = it.player.team()?.data()?.teamId

                    val nextPotentialCapture = futureHolders.peekFirst()
                    var nextCaptureSameTeam = false

                    if (nextPotentialCapture != null)
                    {
                        val nextPotentialPlayer = Bukkit
                            .getPlayer(nextPotentialCapture)
                            ?.team()?.data()?.teamId

                        if (nextPotentialPlayer == previousHolderTeamId)
                        {
                            nextCaptureSameTeam = true
                        } else
                        {
                            runner?.cancel()
                            runner = null
                        }
                    } else
                    {
                        runner?.cancel()
                        runner = null
                    }

                    it.player.sendMessage("${CC.BLACK}bestie why u leaving :(, it's giving.... DIVORCE. ${CC.YELLOW}You exited: ${name()}")
                    it.player.sendMessage(
                        "${CC.RED}You are no longer capping for your team!"
                    )

                    if (nextCaptureSameTeam)
                    {
                        it.player.sendMessage("${CC.WHITE}Your team still holds the Koth though SLAY.")
                    }

                    Bukkit.broadcastMessage(
                        "$PREFIX_KOTH${CC.RED}HELLO???? ${
                            it.player.team()?.data()?.colored()
                        }${CC.RED} is no longer controling ${special()}Koth!"
                    )
                }

                if (futureHolders.contains(it.player.uniqueId))
                {
                    futureHolders.remove(it.player.uniqueId)
                    it.player.sendMessage("${CC.YELLOW}You exited: ${name()}")
                }
            }
        }

        Events
            .subscribe(PlayerMoveEvent::class.java)
            .filter(EventUtils::hasPlayerMoved)
            .handler {
                handleGenericMovementEvent(
                    GenericMovementEvent(player = it.player, to = it.to, from = it.from)
                )
            }
            .bindWith(terminable)

        Events
            .subscribe(PlayerTeleportEvent::class.java)
            .handler {
                handleGenericMovementEvent(
                    GenericMovementEvent(player = it.player, to = it.to, from = it.from)
                )
            }
            .bindWith(terminable)
    }
}

fun Player.account() = BonkersModule.playerAccounts[this.uniqueId]!!
