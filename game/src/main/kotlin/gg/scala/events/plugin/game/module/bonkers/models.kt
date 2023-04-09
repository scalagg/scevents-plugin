package gg.scala.events.plugin.game.module.bonkers

import gg.scala.cgs.common.player.CgsGamePlayer
import gg.scala.cgs.common.player.nametag.CgsGameNametagAdapter
import gg.scala.cgs.common.player.scoreboard.CgsGameScoreboardRenderer
import gg.scala.cgs.common.player.visibility.CgsGameVisibilityAdapter
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
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.EventUtils
import net.evilblock.cubed.util.bukkit.cuboid.Cuboid
import net.evilblock.cubed.util.math.Numbers
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
    val color: ChatColor,
    val spawnLocation: Location,
    val region: Cuboid,
    var points: Int = 0,
    var raidable: Boolean = false
)

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
            }

            CgsGameState.STARTED ->
            {
            }

            CgsGameState.ENDED ->
            {
            }

            CgsGameState.WAITING ->
            {
            }
        }

        lines += "Balance: ${CC.GOLD}${
            Numbers.format(
                BonkersModule.playerAccounts[player.uniqueId]?.balance ?: 0
            )
        }"

        val teamData = player.team()?.data()
        lines += "Your Team: ${
            teamData?.color ?: ChatColor.WHITE
        }${
            Numbers.format(
                teamData?.points ?: 0
            )
        }"

        lines += ""

        BonkersModule.mappings.forEach {
            CgsGameTeamService.teams[it.value.teamId]
                ?.apply {
                    lines += " ${it.value.color}${it.key}: ${CC.WHITE}${
                        Numbers.format(it.value.points)
                    }"
                }
        }

        lines += ""
        lines += "${CC.PRI}${LemonConstants.WEB_LINK}"
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
    val startNewOneAfter: Boolean = false
)
{
    var currentHolder: UUID? = null
    var futureHolders = LinkedList<UUID>()

    val terminable = CompositeTerminable.create()
    var runner: DiminutionRunnable? = null

    fun Location.stringify() = "($x, $y, $z)"

    fun createNewRunner() = createRunner(
        seconds = this.seconds,
        end = {
            Bukkit.broadcastMessage(
                "$PREFIX_KOTH${CC.B_GREEN}Koth ${region.lowerNE.stringify()} was completed!"
            )

            if (currentHolder == null)
            {
                Bukkit.broadcastMessage("$PREFIX_KOTH${CC.WHITE}No one captured.")
            } else
            {
                Bukkit.broadcastMessage(
                    "$PREFIX_KOTH${CC.WHITE}${
                        Bukkit.getPlayer(currentHolder)?.colorfulName()
                    }${CC.GRAY} captured koth!"
                )
            }

            Bukkit.broadcastMessage(
                "$PREFIX_KOTH${CC.GOLD}Loot is dropping at the center of Koth ${region.lowerNE} in 5 seconds!"
            )

            Schedulers
                .sync()
                .runLater({
                    // TODO: drop loot
                }, 100L)

            Schedulers.sync()
                .runLater({
                    BonkersModule.startNewConquestEvent(BonkersModule.conquests.random())
                }, 30 * 20)

            terminable.closeAndReportException()
        },
        update = {
            Bukkit.broadcastMessage(
                "$PREFIX_KOTH${CC.GREEN}Koth ${region.lowerNE.stringify()} will capture in ${
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
        Bukkit.broadcastMessage("$PREFIX_KOTH${CC.GREEN}Koth has started at ${region.lowerNE.stringify()}!")

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
                        "${CC.GREEN}You are now the new capper of Koth!"
                    )

                    createNewRunner()

                    Bukkit.broadcastMessage(
                        "$PREFIX_KOTH${CC.WHITE}${bukkitPlayer.colorfulName()}${CC.GREEN} is now capping Koth!"
                    )
                }
            }, 0L, 10L)
            .bindWith(terminable)

        Events
            .subscribe(PlayerQuitEvent::class.java)
            .handler {
                currentHolder = null
                futureHolders.remove(it.player.uniqueId)
            }

        fun name() = "${CC.GOLD}Koth ${CC.GRAY}${region.lowerNE.stringify()}"

        fun handleGenericMovementEvent(it: GenericMovementEvent)
        {
            if (region.contains(it.to))
            {
                if (currentHolder != it.player.uniqueId)
                {
                    if (!futureHolders.contains(it.player.uniqueId))
                    {
                        futureHolders.push(it.player.uniqueId)
                        it.player.sendMessage("${CC.YELLOW}You entered: ${name()}")

                        if (currentHolder != null)
                        {
                            it.player.sendMessage("${CC.WHITE}You're queued up to become the capper if ${CC.BOLD}${currentHolder!!.username()}${CC.WHITE} leaves.")
                        }
                    }
                }
            } else
            {
                if (currentHolder == it.player.uniqueId)
                {
                    currentHolder = null

                    runner?.cancel()
                    runner = null

                    it.player.sendMessage("${CC.YELLOW}You exited: ${name()}")
                    it.player.sendMessage(
                        "${CC.RED}You are no longer capping!"
                    )

                    Bukkit.broadcastMessage(
                        "$PREFIX_KOTH${CC.RED}${it.player.colorfulName()}${CC.RED} is no longer capping Koth!"
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
