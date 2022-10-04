package gg.scala.events.plugin.game.module.oitc

import gg.scala.cgs.common.player.nametag.CgsGameNametagAdapter
import gg.scala.cgs.common.player.scoreboard.CgsGameScoreboardRenderer
import gg.scala.cgs.common.player.visibility.CgsGameVisibilityAdapter
import gg.scala.cgs.common.snapshot.CgsGameSnapshot
import gg.scala.events.plugin.game.module.EventModule
import gg.scala.events.plugin.game.module.EventModuleBindType
import gg.scala.events.shared.type.EventType

/**
 * @author GrowlyX
 * @since 10/3/2022
 */
@EventModuleBindType(EventType.OneInTheChamber::class)
object OneOnTheChamberModule : EventModule
{
    override val scoreboardFormatter: CgsGameScoreboardRenderer
        get() = TODO("Not yet implemented")
    override val visibilityAdapter: CgsGameVisibilityAdapter
        get() = TODO("Not yet implemented")
    override val nametagProvider: CgsGameNametagAdapter
        get() = TODO("Not yet implemented")
    override val gameSnapshot: CgsGameSnapshot
        get() = TODO("Not yet implemented")

    override fun configure()
    {
        TODO("Not yet implemented")
    }
}
