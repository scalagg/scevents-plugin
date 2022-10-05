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
import me.lucko.helper.Events
import me.lucko.helper.utils.Players
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.EventUtils
import net.evilblock.cubed.util.time.TimeUtil
import net.evilblock.cubed.visibility.VisibilityAction
import org.bukkit.Material
import org.bukkit.entity.Player
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
                lines += ""
                lines += "${CC.WHITE}Ends in: ${CC.GREEN}15:00"
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
        override fun getExtraInformation() = listOf<String>()
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
