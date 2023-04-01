package gg.scala.events.plugin.game.module.bonkers

import gg.scala.cgs.common.CgsGameEngine
import gg.scala.cgs.common.player.CgsGamePlayer
import gg.scala.cgs.common.player.nametag.CgsGameNametagAdapter
import gg.scala.cgs.common.player.scoreboard.CgsGameScoreboardRenderer
import gg.scala.cgs.common.player.visibility.CgsGameVisibilityAdapter
import gg.scala.cgs.common.snapshot.CgsGameSnapshot
import gg.scala.cgs.common.states.CgsGameState
import gg.scala.cgs.common.teams.CgsGameTeam
import gg.scala.cgs.common.teams.CgsGameTeamService
import gg.scala.events.plugin.game.module.EventBind
import gg.scala.events.plugin.game.module.EventModule
import gg.scala.events.shared.type.EventType
import gg.scala.flavor.service.Service
import gg.scala.flavor.service.ignore.IgnoreAutoScan
import gg.scala.lemon.LemonConstants
import gg.scala.lemon.util.task.DiminutionRunnable
import me.lucko.helper.Commands
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.terminable.composite.CompositeTerminable
import net.evilblock.cubed.nametag.NametagInfo
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.EventUtils
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.bukkit.Tasks.delayed
import net.evilblock.cubed.util.bukkit.cuboid.Cuboid
import net.evilblock.cubed.util.math.Numbers
import net.evilblock.cubed.visibility.VisibilityAction
import org.apache.commons.lang.time.DurationFormatUtils
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*

/**
 * @author GrowlyX
 * @since 3/31/2023
 */
@Service
@IgnoreAutoScan
@EventBind(EventType.Bonkers::class)
object BonkersModule : EventModule
{
    data class TeamData(
        val teamId: Int,
        val color: ChatColor,
        val spawnLocation: Location,
        val region: Cuboid,
        var points: Int = 0,
        var raidable: Boolean = false
    )

    private fun loc(x: Double, y: Double, z: Double) =
        Location(
            Bukkit.getWorld("world"), x, y, z
        )

    val mappings = mutableListOf(
        "" to TODO(),
        "Blue" to TeamData(
            teamId = 1,
            color = ChatColor.BLUE,
            spawnLocation = loc(500.500, 53.0, 678.500),
            region = Cuboid(
                loc(454.500, 0.0, 301.500),
                loc(546.500, 144.0, 351.500)
            )
        ),
        "Red" to TeamData(
            teamId = 2,
            color = ChatColor.RED,
            spawnLocation = loc(500.500, 53.0, 322.500),
            region = Cuboid(
                loc(546.500, 0.0, 649.500),
                loc(454.500, 144.0, 699.500)

            )
        ),
        "Green" to TeamData(
            teamId = 3,
            color = ChatColor.GREEN,
            spawnLocation = loc(322.500, 53.0, 500.500),
            region = Cuboid(
                loc(699.500, 0.0, 454.500),
                loc(649.500, 144.0, 546.500)

            )
        ),
        "Yellow" to TeamData(
            teamId = 4,
            color = ChatColor.YELLOW,
            spawnLocation = loc(678.500, 53.0, 500.500),
            region = Cuboid(
                loc(301.500, 0.0, 546.500),
                loc(351.500, 144.0, 454.500)
            )
        )
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

            lines += "Balance: ${
                Numbers.format(
                    playerAccounts[player.uniqueId]?.balance ?: 0
                )
            }"

            val teamData = player.team()?.data()
            lines += "Points: ${
                teamData?.second?.color ?: ChatColor.WHITE
            }${
                Numbers.format(
                    teamData?.second?.points ?: 0
                )
            }"

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

    override val gameSnapshot = GameSnapshot
    override val nametagProvider = NametagAdapter
    override val type = EventType.Bonkers
    override val scoreboardFormatter = ScoreboardRenderer
    override val visibilityAdapter = VisibilityAdapter

    val playerAccounts = mutableMapOf<UUID, Account>()
    val conquests = mutableListOf(
        Cuboid(
            loc(385.0, 46.0, 358.0),
            loc(372.0, 60.0, 372.0)
        ),
        Cuboid(
            loc(642.0, 46.0, 372.0),
            loc(628.0, 60.0, 358.0)
        ),
        Cuboid(
            loc(358.0, 46.0, 628.0),
            loc(372.0, 60.0, 642.0)
        ),
        Cuboid(
            loc(628.0, 46.0, 642.0),
            loc(642.0, 60.0, 628.0)
        )
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
    }

    fun startNewConquestEvent(region: Cuboid)
    {
        ConquestKothMix(region, 60 * 5).configure()
    }

    data class ConquestKothMix(
        val region: Cuboid,
        val seconds: Int,
        val startNewOneAfter: Boolean = false
    )
    {
        private var currentHolder: UUID? = null
        private var futureHolders = LinkedList<UUID>()

        private val terminable = CompositeTerminable.create()
        private var runner: DiminutionRunnable? = null

        fun createNewRunner() = createRunner(
            seconds = this.seconds,
            end = {
                Bukkit.broadcastMessage(
                    "${CC.B_GREEN}Koth ${region.lowerNE} was completed!"
                )

                if (currentHolder == null)
                {
                    Bukkit.broadcastMessage("${CC.GRAY} - No one capped.")
                } else
                {
                    Bukkit.broadcastMessage(
                        "${CC.GRAY} - ${
                            Bukkit.getPlayer(currentHolder)?.name
                        } captured koth!"
                    )
                }

                Bukkit.broadcastMessage(
                    "${CC.GOLD}Loot is dropping at the center of Koth ${region.lowerNE} in 5 seconds!"
                )

                Schedulers
                    .sync()
                    .runLater({
                        // TODO: drop loot
                    }, 100L)

                Schedulers.sync()
                    .runLater({
                        startNewConquestEvent(conquests.random())
                    }, 30 * 20)

                terminable.closeAndReportException()
            },
            update = {
                Bukkit.broadcastMessage(
                    "${CC.GREEN}Koth ${region.lowerNE} will capture in ${
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

                        currentHolder = futureHolders.pop()
                        bukkitPlayer.sendMessage(
                            "${CC.GREEN}You are now the new capper of Koth!"
                        )

                        createNewRunner()

                        Bukkit.broadcastMessage(
                            "${CC.GREEN}${bukkitPlayer.name} is now capping Koth!"
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

            Events
                .subscribe(PlayerMoveEvent::class.java)
                .filter(EventUtils::hasPlayerMoved)
                .handler {
                    if (region.contains(it.to))
                    {
                        if (currentHolder != it.player.uniqueId)
                        {
                            if (!futureHolders.contains(it.player.uniqueId))
                            {
                                futureHolders.push(it.player.uniqueId)
                                it.player.sendMessage("${CC.GRAY}You entered koth.")
                            }
                        }
                    } else
                    {
                        if (currentHolder == it.player.uniqueId)
                        {
                            currentHolder = null

                            runner?.cancel()
                            runner = null

                            it.player.sendMessage(
                                "${CC.RED}You are no longer capping!"
                            )

                            Bukkit.broadcastMessage(
                                "${CC.RED}${it.player.name} is no longer capping Koth!"
                            )
                        }

                        if (futureHolders.contains(it.player.uniqueId))
                        {
                            futureHolders.remove(it.player.uniqueId)
                            it.player.sendMessage("${CC.GRAY}You left koth.")
                        }
                    }
                }
                .bindWith(terminable)
        }
    }

    var gracePeriodCompleted = false

    fun Player.team() = CgsGameTeamService.getTeamOf(this)
    fun CgsGameTeam.data() = mappings[this.id]

    override fun configure()
    {
        val claims = mutableMapOf<String, Cuboid>()

        this.mappings.forEach {
            claims[it.first.lowercase() + "_base"] = it.second.region
            claims[it.first.lowercase() + "_mine"] = it.second.region
        }

        claims["central_mine"] = Cuboid(
            loc(559.500, 0.0, 559.500),
            loc(441.500, 144.0, 441.500)
        )

        fun Player.findClaim() = claims.values
            .firstOrNull {
                it.contains(this)
            }

        fun Player.findClaimContextual(context: String) = claims.entries
            .filter { it.key.contains(context) }
            .firstOrNull {
                it.value.contains(this)
            }

        fun Block.findClaimContextual(context: String) = claims.entries
            .filter { it.key.contains(context) }
            .firstOrNull {
                it.value.contains(this)
            }

        Events
            .subscribe(PlayerJoinEvent::class.java)
            .handler {
                playerAccounts[it.player.uniqueId] = Account()

                delayed(1L) {
                    SelectTeamMenu().openMenu(it.player)
                }
            }

        Events
            .subscribe(PlayerQuitEvent::class.java)
            .handler {
                playerAccounts.remove(it.player.uniqueId)
            }

        val mappings = mutableMapOf(
            Material.GOLD_ORE to Material.GOLD_INGOT,
            Material.DIAMOND_ORE to Material.DIAMOND,
            Material.COAL_ORE to Material.COAL,
            Material.IRON_ORE to Material.IRON_INGOT,
            Material.STONE to Material.COBBLESTONE
        )

        Events
            .subscribe(BlockBreakEvent::class.java)
            .filter {
                it.block.type in mappings.keys && it.player.location.y < 42
            }
            .handler {
                val claim = it.block.findClaimContextual("mine")

                if (claim == null)
                {
                    it.isCancelled = true
                    return@handler
                }

                val mapping = this.mappings
                    .firstOrNull { data ->
                        data.first.lowercase() == claim.key.split("_")[0]
                    }

                if (mapping != null)
                {
                    if (it.player.team()?.id != mapping.second.teamId)
                    {
                        it.isCancelled = true
                        return@handler
                    }
                }

                val original = it.block.type
                it.isCancelled = true
                it.block.type = Material.COBBLESTONE

                it.player.inventory.addItem(
                    ItemBuilder
                        .of(mappings[original]!!)
                        .build()
                )

                delayed(40L) {
                    it.block.type = original
                }
            }

        Events
            .subscribe(PlayerInteractEvent::class.java)
            .filter { it.clickedBlock != null }
            .handler {
                val claim = it.clickedBlock
                    .findClaimContextual("base")
                    ?: return@handler

                val mapping = this.mappings
                    .firstOrNull { data ->
                        data.first.lowercase() == claim.key.split("_")[0]
                    }
                    ?: return@handler

                if (it.player.team()?.id != mapping.second.teamId)
                {
                    it.isCancelled = true
                    return@handler
                }

                it.isCancelled = false
            }

        fun createGracePeriodRunner() = createRunner(
            seconds = 60 * 15,
            end = {
                gracePeriodCompleted = true
                Bukkit.broadcastMessage("${CC.GREEN}Grace period has ended! You can now capture koths!")

                startNewConquestEvent(conquests.random())
            },
            update = {
                Bukkit.broadcastMessage(
                    "${CC.RED}Grace period ends in ${
                        DurationFormatUtils.formatDurationWords(it * 1000L, true, true)
                    }..."
                )
            }
        )

        Events
            .subscribe(CgsGameEngine.CgsGameStartEvent::class.java)
            .handler {
                createGracePeriodRunner()

                this.mappings.forEach {
                    CgsGameTeamService.teams[it.second.teamId]
                        ?.apply {
                            participants.forEach { player ->
                                val bukkitPlayer = Bukkit.getPlayer(player)
                                    ?: return@apply

                                bukkitPlayer
                                    .teleport(it.second.spawnLocation)
                            }
                        }
                }

                Schedulers.async()
                    .runRepeating({ _ ->
                        playerAccounts
                            .forEach { (_, account) ->
                                account.balance += 3
                            }
                    }, 0L, 60L)
            }

        Commands
            .create()
            .assertPlayer()
            .handler {
                SelectTeamMenu().openMenu(it.sender())
            }
            .register("selectteam")
    }
}
