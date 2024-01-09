package gg.scala.events.plugin.game.module.bonkers

import gg.scala.cgs.common.CgsGameEngine
import gg.scala.cgs.common.player.CgsGamePlayer
import gg.scala.cgs.common.player.handler.CgsGameDisqualificationHandler
import gg.scala.cgs.common.player.handler.CgsPlayerHandler
import gg.scala.cgs.common.player.nametag.CgsGameNametagAdapter
import gg.scala.cgs.common.player.scoreboard.CgsGameScoreboardRenderer
import gg.scala.cgs.common.player.visibility.CgsGameVisibilityAdapter
import gg.scala.cgs.common.runnable.state.StartingStateRunnable
import gg.scala.cgs.common.snapshot.CgsGameSnapshot
import gg.scala.cgs.common.states.CgsGameState
import gg.scala.cgs.common.teams.CgsGameTeam
import gg.scala.cgs.common.teams.CgsGameTeamService
import gg.scala.events.plugin.game.module.bonkers.teams.timers.CombatTimer
import gg.scala.events.plugin.game.module.bonkers.teams.timers.TeamHomeTimer
import gg.scala.lemon.LemonConstants
import gg.scala.lemon.animated.AnimatedScoreboardTitle
import gg.scala.lemon.util.task.DiminutionRunnable
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.terminable.composite.CompositeTerminable
import net.evilblock.cubed.nametag.NametagInfo
import net.evilblock.cubed.nametag.NametagProvider
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.Color
import net.evilblock.cubed.util.bukkit.EventUtils
import net.evilblock.cubed.util.bukkit.Tasks
import net.evilblock.cubed.util.bukkit.cuboid.Cuboid
import net.evilblock.cubed.util.math.Numbers
import net.evilblock.cubed.util.time.TimeUtil
import net.evilblock.cubed.visibility.VisibilityAction
import org.apache.commons.lang.time.DurationFormatUtils
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Sound
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
    var dtr: Double = 5.0,
    var balance: Int = 0,
    var teamFocus: Int? = null,
    var teamRally: Location? = null,
    val ownedChests: MutableList<Location> = mutableListOf(),
    var voteKickTarget: Player? = null,
    var voteKickVotes: MutableMap<UUID, Boolean> = mutableMapOf()
)
{
    fun colored() = "$color$teamName"
    fun raidable() = dtr <= 0.0

    fun startVoteKick()
    {
        createRunner(
            60,
            end = {
                val team = CgsGameTeamService.teams[teamId]
                    ?: return@createRunner

                if (voteKickTarget == null)
                {
                    team.participants
                        .mapNotNull {
                            Bukkit.getPlayer(it)
                        }
                        .forEach {
                            it.sendMessage("${CC.GOLD}[Team] ${CC.GOLD}Votekick has been cancelled because the target has disconnected!")
                        }

                    return@createRunner
                }

                val inFavor = voteKickVotes.values.count { it }
                val requiredFavoredVotes = if (team.participants.size % 2 == 0)
                {
                    (team.participants.size / 2) + 1
                } else
                {
                    (team.participants.size + 1) / 2
                }

                if (inFavor >= requiredFavoredVotes)
                {
                    val oldDtr = dtr

                    val previouslyRaidable = raidable()
                    dtr -= 1.0

                    team.participants.forEach { id ->
                        Bukkit.getPlayer(id)?.apply {
                            sendMessage("${CC.RED}Teammate Voted Off: ${CC.WHITE}${voteKickTarget!!.name}")
                            sendMessage("${CC.YELLOW}DTR: ${CC.WHITE}$oldDtr -> $dtr")

                            if (raidable() && !previouslyRaidable)
                            {
                                playSound(location, Sound.ENDERDRAGON_GROWL, 1.0f, 1.0f)
                            }
                        }
                    }

                    CgsGameDisqualificationHandler.disqualifyPlayer(
                        player = voteKickTarget!!,
                        broadcastNotification = true, setSpectator = true
                    )
                } else
                {
                    team.participants
                        .mapNotNull {
                            Bukkit.getPlayer(it)
                        }
                        .forEach {
                            it.sendMessage("${CC.GOLD}[Team] ${CC.GOLD}${voteKickTarget!!.displayName}${CC.SEC} has ${CC.RED}not ${CC.GOLD}been voted off!")
                        }
                }

                voteKickVotes.clear()
                voteKickTarget = null
            },
        )
    }
}

fun createRunner(
    seconds: Int,
    end: () -> Unit = { },
    update: (Int) -> Unit = {}
) = object : DiminutionRunnable(seconds + 1)
{
    var tickingSeconds = seconds

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
        tickingSeconds--
    }
}.apply {
    runTaskTimer(BonkersModule.plugin, 0L, 20L)
}

data class GenericMovementEvent(
    val player: Player,
    val to: Location,
    val from: Location
)

object AnimatedBunkersTitle : AnimatedScoreboardTitle()
{
    private val internal = mutableListOf<String>()

    init
    {
        for (i in 0..50)
        {
            internal.add("${CC.B_PRI}BUNKERS")
        }

        internal.add(
            Color.translate(
                "&8&lB&3&lU&b&lNKERS"
            )
        )

        internal.add(
            Color.translate(
                "&8&lBU&3&lN&b&lKERS"
            )
        )

        internal.add(
            Color.translate(
                "&8&lBUN&3&lK&b&lERS"
            )
        )

        internal.add(
            Color.translate(
                "&8&lBUNK&3&lE&b&lRS"
            )
        )

        internal.add(
            Color.translate(
                "&8&lBUNKE&3&lR&b&lS"
            )
        )
        internal.add(
            Color.translate(
                "&8&lBUNKER&3&lS"
            )
        )

        internal.add(
            Color.translate(
                "&b&lBUNKERS&3&l"
            )
        )

        for (i in 0..5)
        {
            internal.add(
                Color.translate(
                    "&8&lBUNKERS"
                )
            )
        }

        for (i in 0..4)
        {
            internal.add(
                Color.translate(
                    "&b&lBUNKERS"
                )
            )
        }

        for (i in 0..5)
        {
            internal.add(
                Color.translate(
                    "&8&lBUNKERS"
                )
            )
        }

        initialize()
    }

    override fun getAnimations(): List<String>
    {
        return internal
    }
}

object ScoreboardRenderer : CgsGameScoreboardRenderer
{
    private val format = SimpleDateFormat()

    override fun getTitle() = AnimatedBunkersTitle.current.toString()

    override fun render(lines: LinkedList<String>, player: Player, state: CgsGameState)
    {
        lines += ""

        if (state == CgsGameState.STARTING)
        {
            lines += "${CC.PRI}Game starts in:"
            lines += "${
                StartingStateRunnable.PRE_START_TIME
            } seconds"
            lines += ""
            lines += "${CC.WHITE}Selected team: ${
                player.team()?.data()?.colored()
            }"
            lines += "${CC.WHITE}Players: ${CC.PRI}${
                Bukkit.getOnlinePlayers().size
            }/${
                Bukkit.getMaxPlayers()
            }"
        } else if (state.isAfter(CgsGameState.STARTED))
        {
            lines += "Game Time: ${CC.PRI}${
                TimeUtil.formatIntoMMSS(
                    ((System.currentTimeMillis() - CgsGameEngine.INSTANCE.gameStart) / 1000).toInt()
                )
            }"

            if (!player.hasMetadata("spectator"))
            {
                lines += "Balance: ${CC.PRI}$${
                    Numbers.format(
                            BonkersModule.playerAccounts[player.uniqueId]?.balance ?: 0
                    )
                }"
                lines += "Kills: ${CC.PRI}$${
                    runCatching {
                        CgsGameEngine.INSTANCE
                                .getStatistics(CgsPlayerHandler.find(player)!!)
                                .gameKills.value
                    }.getOrNull() ?: 0
                }"
            }

            if (player.hasMetadata("spectator"))
            {
                lines += " "
                lines += "${CC.PRI}Team Info: "
                lines += " ${CgsGameTeamService.teams[1]?.data()!!.colored()} ${CC.GRAY}[${CgsGameTeamService.teams[1]?.participants?.size}/5]: ${CC.WHITE}${CgsGameTeamService.teams[1]?.data()!!.dtr} ◉"
                lines += " ${CgsGameTeamService.teams[2]?.data()!!.colored()} ${CC.GRAY}[${CgsGameTeamService.teams[2]?.participants?.size}/5]: ${CC.WHITE}${CgsGameTeamService.teams[2]?.data()!!.dtr} ◉"
                lines += " ${CgsGameTeamService.teams[3]?.data()!!.colored()} ${CC.GRAY}[${CgsGameTeamService.teams[3]?.participants?.size}/5]: ${CC.WHITE}${CgsGameTeamService.teams[3]?.data()!!.dtr} ◉"
                lines += " ${CgsGameTeamService.teams[4]?.data()!!.colored()} ${CC.GRAY}[${CgsGameTeamService.teams[4]?.participants?.size}/5]: ${CC.WHITE}${CgsGameTeamService.teams[4]?.data()!!.dtr} ◉"
            }

            val team = player.team()

            if (team != null)
            {
                val data = team.data()!!
                lines += ""
                lines += "${CC.PRI}Faction:"
                lines += "${CC.WHITE}Member: ${data.colored()}"

                if (data.teamFocus != null)
                {
                    val focusTeam = CgsGameTeamService.teams[data.teamFocus]

                    if (focusTeam != null)
                    {
                        lines += "Focus: ${focusTeam.data()!!.colored()}"
                    }
                }
            }

            if (TeamHomeTimer.isActive(player))
            {
                lines += "Home Timer: ${CC.BLUE}${
                    TimeUtil.formatIntoMMSS(
                        ((TeamHomeTimer.fetchRemaining(player)) / 1000).toInt()
                    )
                }"
            }

            if (CombatTimer.isActive(player))
            {
                lines += "Combat Tag: ${CC.RED}${
                    TimeUtil.formatIntoMMSS(
                        ((CombatTimer.fetchRemaining(player)) / 1000).toInt()
                    )
                }"
            }


            val centerKoth = BonkersModule.currentCentralKoth
            val conquest = BonkersModule.currentConquest

            if (centerKoth != null)
            {
                lines += ""
                lines += "${CC.GOLD}KingOfTheHill"
                lines += " Location: ${CC.BLUE}Classic"
                lines += " Time: ${CC.YELLOW}${
                    centerKoth.formatRemainingCaptureTime()
                }"
                if (centerKoth.currentHolder != null)
                {
                    val holdingPlayer = Bukkit.getPlayer(centerKoth.currentHolder)
                    val team = holdingPlayer.team() ?: return

                    lines += " ${CC.R}Capturer: ${
                        team.data()!!.colored()
                    }"
                } else
                {
                    lines += " ${CC.R}Capturer: ${CC.GRAY}None"
                }
            } else if (conquest != null)
            {
                lines += ""
                lines += "${CC.D_AQUA}Conquest:"
                lines += " ${conquest.name}"
                lines += " ${CC.R}Time: ${CC.AQUA}${
                    conquest.formatRemainingCaptureTime()
                }"
                if (conquest.currentHolder != null)
                {
                    val holdingPlayer = Bukkit.getPlayer(conquest.currentHolder)
                    val team = holdingPlayer.team() ?: return

                    lines += " ${CC.R}Capturer: ${
                        team.data()!!.colored()
                    }"
                } else
                {
                    lines += " ${CC.R}Capturer: ${CC.GRAY}None"
                }
            }

            // TODO: ADD Next Event Scoreboard Status

//            lines += ""
//            lines += "${CC.B_GOLD}Next Event"
//            lines += "${CC.I_GRAY}Starting in 5 seconds..."
        } else if (state == CgsGameState.WAITING)
        {
            lines += "${CC.PRI}Waiting:"
            lines += "${
                Bukkit.getMaxPlayers() - Bukkit.getOnlinePlayers().size
            } more players."
            lines += ""
            lines += "${CC.WHITE}Selected team: ${
                player.team()?.data()?.colored()
            }"
            lines += "${CC.WHITE}Players: ${CC.PRI}${
                Bukkit.getOnlinePlayers().size
            }/${
                Bukkit.getMaxPlayers()
            }"
        }

        lines += ""
        lines += CC.GRAY + LemonConstants.WEB_LINK + "          " + CC.GRAY + "      " + CC.GRAY + "  " + CC.GRAY
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
    val active: Boolean = false,
    val seconds: Int,
    val name: String,
    val startNewOneAfter: Boolean = false,
    val specialCentral: Boolean = false
)
{
    var currentHolder: UUID? = null
    var previousHolderTeamId: Int? = null

    var futureHolders = LinkedList<UUID>()

    val terminable = CompositeTerminable.create()
    var runner: DiminutionRunnable? = null

    var expectedEndTime = -1L

    fun formatRemainingCaptureTime() = if (runner == null)
    {
        TimeUtil.formatIntoMMSS(seconds)
    } else
    {
        TimeUtil.formatIntoMMSS(
            ((expectedEndTime - System.currentTimeMillis()) / 1000L).toInt()
        )
    }

    fun Location.stringify() = "($x, $y, $z)"

    fun createNewRunner(duels: Boolean = true) = createRunner(
        seconds = this.seconds,
        end = {
            if (specialCentral)
            {
                Bukkit.broadcastMessage(
                    "$PREFIX_KOTH${CC.YELLOW}${name}KoTH ${CC.GRAY}${region.lowerNE.stringify()} ${CC.YELLOW}was completed!"
                )
            } else
            {
                Bukkit.broadcastMessage(
                    "$PREFIX_CONQUEST${CC.YELLOW}${name}${CC.YELLOW}Conquest ${CC.GRAY}${region.lowerNE.stringify()} ${CC.YELLOW}was completed!"
                )
            }

            if (currentHolder == null)
            {
                Bukkit.broadcastMessage("$PREFIX_KOTH${CC.WHITE}No one captured.")
            } else
            {
                val holder = Bukkit.getPlayer(currentHolder)
                val holderTeam = holder?.team()?.data()

                if (specialCentral)
                {
                    Bukkit.broadcastMessage(
                        "$PREFIX_KOTH${CC.WHITE}${
                            Bukkit.getPlayer(currentHolder)?.colorfulName()
                        }${CC.GRAY} captured KoTH for ${
                            holderTeam?.color
                        }${
                            holderTeam?.teamName
                        }${CC.GRAY}!"
                    )
                } else
                {
                    Bukkit.broadcastMessage(
                        "$PREFIX_CONQUEST${CC.WHITE}${
                            Bukkit.getPlayer(currentHolder)?.colorfulName()
                        }${CC.GRAY} captured Conquest for ${
                            holderTeam?.color
                        }${
                            holderTeam?.teamName
                        }${CC.GRAY}!"
                    )
                }
            }

            BonkersModule.currentConquest = null

            if (!specialCentral)
            {
                Bukkit.broadcastMessage(
                    "$PREFIX_CONQUEST${CC.YELLOW}Loot is dropping at the center of ${name}${CC.YELLOW}Conquest ${CC.YELLOW}in 5 seconds!"
                )
            }

            Schedulers
                .sync()
                .runLater({
                    // TODO: drop loot
                }, 100L)

            if (startNewOneAfter)
            {
                Schedulers.sync()
                    .runLater({
                        if (!duels)
                        {
                            BonkersModule.conquests.entries.random()
                                .apply {
                                    BonkersModule.startNewConquestEvent(value, key)
                                }
                        }
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
            if (specialCentral)
            {
                Bukkit.broadcastMessage(
                    "$PREFIX_KOTH${CC.BLUE}${name}${CC.YELLOW}KoTH ${CC.GRAY}${region.lowerNE.stringify()} ${CC.YELLOW}is being controlled. ${CC.RED}${
                        DurationFormatUtils.formatDurationWords(it * 1000L, true, true) + " left${CC.YELLOW}."
                    }"
                )
            } else
            {
                Bukkit.broadcastMessage(
                    "$PREFIX_CONQUEST${name}${CC.YELLOW}Conquest ${CC.GRAY}${region.lowerNE.stringify()} ${CC.YELLOW}is being controlled. ${CC.RED}${
                        DurationFormatUtils.formatDurationWords(it * 1000L, true, true) + " left${CC.YELLOW}."
                    }"
                )
            }
        }
    ).apply {
        runner?.cancel()
        runner = this

        expectedEndTime = System.currentTimeMillis() + (seconds * 1000L)
    }

    fun configure()
    {
        if (specialCentral)
        {
            Bukkit.broadcastMessage("$PREFIX_KOTH${name}${CC.YELLOW}KoTH has started at ${CC.GRAY}${region.lowerNE.stringify()}${CC.YELLOW}!")
            BonkersModule.currentCentralKoth = this
        } else
        {
            BonkersModule.currentConquest = this
            Bukkit.broadcastMessage("$PREFIX_CONQUEST${CC.YELLOW}The ${name}${CC.YELLOW}Conquest ${CC.YELLOW}has started at ${CC.GRAY}${region.lowerNE.stringify()}${CC.YELLOW}!")
        }

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

                    if (specialCentral)
                    {
                        Bukkit.broadcastMessage(
                            "$PREFIX_KOTH${CC.YELLOW}Someone is now capping ${CC.BLUE}${name}${CC.YELLOW}KoTH!"
                        )
                    } else
                    {
                        Bukkit.broadcastMessage(
                            "$PREFIX_CONQUEST${CC.YELLOW}Someone is now capping ${name}${CC.YELLOW}Conquest${CC.YELLOW}!"
                        )
                    }

                    if (runner == null)
                    {
                        createNewRunner()
                    } else
                    {
                        runner?.cancel()
                        runner = null

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

        fun name() =
            if (specialCentral) "${name}${CC.GOLD}${CC.YELLOW}KoTH ${CC.GRAY}${region.lowerNE.stringify()}" else "${name}${CC.YELLOW}Conquest ${CC.GRAY}${region.lowerNE.stringify()}"

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
                        it.player.sendMessage("${CC.YELLOW}You entered: ${name().replace("_base", "")}")
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

                    it.player.sendMessage("${CC.YELLOW}You exited: ${name()}")
                    it.player.sendMessage(
                        "${CC.RED}You are no longer capping for your team!"
                    )

                    if (specialCentral)
                    {
                        Bukkit.broadcastMessage(
                            "$PREFIX_KOTH${CC.YELLOW}Control of ${CC.BLUE}${name}${CC.YELLOW}KoTH has been lost!"
                        )
                    } else
                    {
                        Bukkit.broadcastMessage(
                            "$PREFIX_CONQUEST${CC.YELLOW}Control of ${name}${CC.YELLOW}Conquest ${CC.YELLOW}has been lost!"
                        )
                    }
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
