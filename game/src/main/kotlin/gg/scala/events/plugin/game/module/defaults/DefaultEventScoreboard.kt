package gg.scala.events.plugin.game.module.defaults

import gg.scala.cgs.common.player.scoreboard.CgsGameScoreboardRenderer
import gg.scala.cgs.common.states.CgsGameState
import org.bukkit.entity.Player
import java.util.*

/**
 * @author GrowlyX
 * @since 10/3/2022
 */
object DefaultEventScoreboard : CgsGameScoreboardRenderer
{
    override fun getTitle() = "server"

    override fun render(
        lines: LinkedList<String>, player: Player, state: CgsGameState
    )
    {

    }
}
