package gg.scala.events.plugin.game.module.oitc

import gg.scala.cgs.common.player.CgsGamePlayer
import gg.scala.cgs.common.player.nametag.CgsGameNametagAdapter
import gg.scala.cgs.common.player.scoreboard.CgsGameScoreboardRenderer
import gg.scala.cgs.common.player.visibility.CgsGameVisibilityAdapter
import gg.scala.cgs.common.snapshot.CgsGameSnapshot
import gg.scala.cgs.common.states.CgsGameState
import gg.scala.events.plugin.game.ScalaEventsGameEngine
import gg.scala.events.plugin.game.module.EventModule
import gg.scala.events.plugin.game.module.EventBind
import gg.scala.events.shared.type.EventType
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Service
import gg.scala.flavor.service.ignore.IgnoreAutoScan
import me.lucko.helper.Events
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.visibility.VisibilityAction
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
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

    object ScoreboardRenderer : CgsGameScoreboardRenderer
    {
        override fun getTitle() = "${CC.B_YELLOW}OITC Event"

        override fun render(
            lines: LinkedList<String>,
            player: Player, state: CgsGameState
        )
        {
            lines += ""
            lines += "${CC.WHITE}Starts in: ${CC.GREEN}2:00"
            lines += ""
            lines += "${CC.YELLOW}www.liftgate.io"
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
        Events
            .subscribe(PlayerJoinEvent::class.java)
            .handler {
                it.player.sendMessage("Welcome to OITC")
            }
    }
}
