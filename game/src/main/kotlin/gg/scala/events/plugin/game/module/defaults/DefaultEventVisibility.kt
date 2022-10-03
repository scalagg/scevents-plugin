package gg.scala.events.plugin.game.module.defaults

import gg.scala.cgs.common.player.CgsGamePlayer
import gg.scala.cgs.common.player.visibility.CgsGameVisibilityAdapter
import net.evilblock.cubed.visibility.VisibilityAction

/**
 * @author GrowlyX
 * @since 10/3/2022
 */
object DefaultEventVisibility : CgsGameVisibilityAdapter
{
    override fun computeVisibility(
        viewer: CgsGamePlayer, target: CgsGamePlayer
    ): VisibilityAction
    {
        return VisibilityAction.NEUTRAL
    }
}
