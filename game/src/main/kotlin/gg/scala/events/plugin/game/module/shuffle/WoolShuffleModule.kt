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
import gg.scala.events.plugin.game.module.ModuleSize
import gg.scala.events.shared.type.EventType
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Service
import gg.scala.flavor.service.ignore.IgnoreAutoScan
import gg.scala.lemon.LemonConstants
import gg.scala.lemon.util.task.DiminutionRunnable
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.utils.Players
import net.evilblock.cubed.nametag.NametagInfo
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.EventUtils
import net.evilblock.cubed.util.bukkit.Tasks
import net.evilblock.cubed.util.bukkit.cuboid.Cuboid
import net.evilblock.cubed.util.time.TimeUtil
import net.evilblock.cubed.visibility.VisibilityAction
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
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
    private var color: ChatColor? = null

    private fun findAliveParticipants() = Players.all()
        .filter {
            !it.hasMetadata("spectator")
        }

    object ScoreboardRenderer : CgsGameScoreboardRenderer
    {
        override fun getTitle() = "${CC.B_PRI}Wool Shuffle ${CC.GRAY}(BETA)"

        override fun render(
            lines: LinkedList<String>,
            player: Player, state: CgsGameState
        )
        {
            lines += " "

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
                    lines += "${CC.WHITE}Current color:"
                    lines += "${
                        if (color != null) "$color${color?.better()}" else "${CC.RED}Selecting..."
                    }"
                    lines += ""
                    lines += "${CC.WHITE}Players: ${CC.PRI}${Players.all().size}/30"
                }

                CgsGameState.ENDED ->
                {
                    lines += "Thanks for playing!"
                    lines += "${CC.PRI}${findAliveParticipants().firstOrNull()?.name ?: "N/A"} won!"
                }

                CgsGameState.WAITING ->
                {
                    lines += "${CC.WHITE}Waiting for players..."
                    lines += "${CC.PRI}${Players.all().size}/30"
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

    override val gameSnapshot = GameSnapshot
    override val nametagProvider = NametagAdapter
    override val type = EventType.WoolShuffle
    override val scoreboardFormatter = ScoreboardRenderer
    override val visibilityAdapter = VisibilityAdapter

    private val lobby = Location(
        Bukkit.getWorld("MapHike"),
        32.5, 54.5, 62.5, -90.0f, 0.0f
    )

    private val middleOfMap = Location(
        Bukkit.getWorld("MapHike"),
        61.5, 50.0, 59.0, 0f, 0f
    )

    override fun configure(moduleSize: ModuleSize)
    {
        val platform = Cuboid(
            Location(
                Bukkit.getWorld("MapHike"),
                82.5, 48.0, 34.5
            ),
            Location(
                Bukkit.getWorld("MapHike"),
                38.5, 48.0, 83.5
            )
        )

        fun resetPlatform()
        {
            platform.getChunks().forEach {
                for (tileEntity in it.tileEntities)
                {
                    tileEntity.type = Material.AIR
                    tileEntity.update()
                }
            }

            Tasks.sync {
                platform.forEach { block ->
                    block.data = 0
                    block.type = Material.WOOL
                }
            }
        }

        resetPlatform()

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
                if (!findAliveParticipants().contains(it.entity))
                {
                    it.isCancelled = true
                    return@handler
                }

                if (color == null)
                {
                    it.isCancelled = true
                }
            }

        var currentRound = 1

        fun randomizePlatform()
        {
            platform
                .forEach {
                    it.type = Material.WOOL
                    it.data = WoolShuffleUtilities.randomColor()
                }
        }

        /**
         * Shuffle wool colors for all blocks in the [cuboid].
         */
        fun shuffle(
            color: ChatColor =
                WoolShuffleUtilities.availableColors.random()
        )
        {
            this.color = color

            val wool = ItemStack(
                Material.WOOL, 1,
                WoolShuffleUtilities
                    .fromChatColor(color)
                    .toShort()
            ).apply {
                val itemMeta = this.itemMeta
                itemMeta.displayName = "$color${color.better()} Wool"
                itemMeta.lore = mutableListOf(
                    "${ChatColor.GRAY}Stand on this block!"
                )

                this.itemMeta = itemMeta
            }

            for (player in findAliveParticipants())
            {
                for (i in 0..8)
                {
                    player.inventory.setItem(i, wool)
                }
            }
        }

        /**
         * Eliminate players who are not on
         * a block with the color [color].
         */
        fun eliminate()
        {
            val woolData = WoolShuffleUtilities
                .fromChatColor(color!!)

            platform
                .filter { it.data != woolData }
                .forEach {
                    it.type = Material.AIR
                }

            Bukkit.broadcastMessage(
                "${CC.B_GOLD}✮ ${CC.YELLOW}Round #$currentRound: ${CC.RED}Players not on $color${color?.better()}${CC.RED} were eliminated!"
            )
        }

        fun startNewRound()
        {
            val active = findAliveParticipants()

            if (active.size <= 1)
            {
                color = null
                winner = active.firstOrNull()?.name
                return
            }

            Bukkit.broadcastMessage(
                "${CC.B_GOLD}✮ ${CC.YELLOW}Round #${currentRound++}: ${CC.GREEN}Selecting color..."
            )

            color = null

            randomizePlatform()

            findAliveParticipants()
                .forEach {
                    it.teleport(middleOfMap)
                    it.inventory.clear()
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
                    shuffle()

                    Bukkit.broadcastMessage(
                        "${CC.B_GOLD}✮ ${CC.YELLOW}Round #$currentRound: ${CC.WHITE}The color $color${color?.better()}${CC.WHITE} was selected!"
                    )

                    findAliveParticipants().forEach {
                        it.playSound(it.location, Sound.NOTE_PLING, 1.0F, 1.0f)
                    }

                    object : DiminutionRunnable((10 - currentRound / 2).coerceIn(2..10))
                    {
                        override fun getSeconds() = listOf(
                            18000, 14400, 10800, 7200, 3600, 2700, 1800,
                            900, 600, 300, 240, 180, 120, 60, 50, 40, 30,
                            15, 10, 5, 4, 3, 2, 1
                        )

                        override fun onEnd()
                        {
                            eliminate()

                            Schedulers.sync()
                                .runLater({
                                          startNewRound()
                                }, 60L)
                        }

                        override fun onRun()
                        {
                            Bukkit.broadcastMessage(
                                "${CC.B_GOLD}✮ ${CC.YELLOW}Round #$currentRound: ${CC.RED}Players will be eliminated in ${CC.BOLD}${seconds}s${CC.RED}!"
                            )
                        }
                    }.runTaskTimer(plugin, 10L, 20L)
                }

                override fun onRun()
                {
                    Bukkit.broadcastMessage(
                        "${CC.B_GOLD}✮ ${CC.YELLOW}Round #$currentRound: ${CC.GREEN}Color will be selected in ${CC.GOLD}${seconds}s${CC.GREEN}..."
                    )

                    findAliveParticipants().forEach {
                        it.playSound(it.location, Sound.NOTE_STICKS, 1.0F, 1.0f)
                    }
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

        listOf(
            BlockBreakEvent::class.java,
            BlockPlaceEvent::class.java,
            FoodLevelChangeEvent::class.java,
            PlayerDropItemEvent::class.java
        )
            .forEach {
                Events
                    .subscribe(it)
                    .handler { event ->
                        event.isCancelled = true
                    }
            }

        Events
            .subscribe(PlayerMoveEvent::class.java)
            .filter(EventUtils::hasPlayerMoved)
            .filter {
                findAliveParticipants().contains(it.player)
            }
            .filter {
                it.to.block.isLiquid
            }
            .handler {
                it.player.health = 0.0
                it.player.teleport(lobby)
                it.player.setMetadata(
                    "spectator", FixedMetadataValue(plugin, true)
                )

                playerLoses(it.player)
            }

        Events
            .subscribe(PlayerInteractEvent::class.java)
            .filter(PlayerInteractEvent::hasBlock)
            .handler { event ->
                event.isCancelled = true
            }

        Events
            .subscribe(CgsGameEngine.CgsGameStartEvent::class.java)
            .handler {
                for (player in Players.all())
                {
                    player.teleport(middleOfMap)
                }
                startNewRound()
            }
    }
}

fun ChatColor.better(): String = name
    .replace("_", " ")
    .lowercase().capitalize()
