package gg.scala.events.plugin.game.module.bonkers

import gg.scala.cgs.common.player.CgsGamePlayer
import gg.scala.cgs.common.player.nametag.CgsGameNametagAdapter
import gg.scala.cgs.common.player.scoreboard.CgsGameScoreboardRenderer
import gg.scala.cgs.common.player.visibility.CgsGameVisibilityAdapter
import gg.scala.cgs.common.snapshot.CgsGameSnapshot
import gg.scala.cgs.common.states.CgsGameState
import gg.scala.events.plugin.game.module.EventBind
import gg.scala.events.plugin.game.module.EventModule
import gg.scala.events.shared.type.EventType
import gg.scala.flavor.service.Service
import gg.scala.flavor.service.ignore.IgnoreAutoScan
import gg.scala.lemon.LemonConstants
import me.lucko.helper.Commands
import me.lucko.helper.Events
import net.evilblock.cubed.nametag.NametagInfo
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Tasks.delayed
import net.evilblock.cubed.visibility.VisibilityAction
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import java.time.Instant
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
    val mappings = mutableListOf(
        "" to ChatColor.BLACK,
        "Blue" to ChatColor.BLUE,
        "Red" to ChatColor.RED,
        "Green" to ChatColor.GREEN,
        "Yellow" to ChatColor.YELLOW
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
                CgsGameState.WAITING -> {
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

    override val gameSnapshot = GameSnapshot
    override val nametagProvider = NametagAdapter
    override val type = EventType.Bonkers
    override val scoreboardFormatter = ScoreboardRenderer
    override val visibilityAdapter = VisibilityAdapter

    val playerAccounts = mutableMapOf<UUID, Account>()
    val teamPoints = mutableMapOf<Int, Int>()

    lateinit var gameStartTime: Instant

    override fun configure()
    {
        Events
            .subscribe(PlayerJoinEvent::class.java)
            .handler {
                delayed(1L) {
                    SelectTeamMenu().openMenu(it.player)
                }
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
