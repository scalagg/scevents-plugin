package gg.scala.events.plugin.game.module

import gg.scala.cgs.common.player.nametag.CgsGameNametagAdapter
import gg.scala.cgs.common.player.scoreboard.CgsGameScoreboardRenderer
import gg.scala.cgs.common.player.visibility.CgsGameVisibilityAdapter
import gg.scala.cgs.common.snapshot.CgsGameSnapshot
import gg.scala.events.shared.type.EventType

/**
 * @author GrowlyX
 * @since 10/3/2022
 */
interface EventModule
{
    val type: EventType

    val scoreboardFormatter: CgsGameScoreboardRenderer
    val visibilityAdapter: CgsGameVisibilityAdapter
    val nametagProvider: CgsGameNametagAdapter

    val bitchRequiredSwmLoadedWorld: String?
        get() = null

    val gameSnapshot: CgsGameSnapshot

    fun configure()
}
