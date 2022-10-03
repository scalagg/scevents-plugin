package gg.scala.events.plugin.game.module.defaults

import gg.scala.cgs.common.player.CgsGamePlayer
import gg.scala.cgs.common.player.nametag.CgsGameNametagAdapter
import gg.scala.cgs.common.player.visibility.CgsGameVisibilityAdapter
import net.evilblock.cubed.nametag.NametagInfo
import net.evilblock.cubed.visibility.VisibilityAction

/**
 * @author GrowlyX
 * @since 10/3/2022
 */
object DefaultEventNametags : CgsGameNametagAdapter
{
    override fun computeNametag(
        viewer: CgsGamePlayer, target: CgsGamePlayer
    ): NametagInfo?
    {
        return null
    }
}
