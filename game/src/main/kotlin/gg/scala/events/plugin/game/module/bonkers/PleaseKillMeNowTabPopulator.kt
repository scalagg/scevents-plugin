package gg.scala.events.plugin.game.module.bonkers

import gg.scala.commons.tablist.TablistPopulator
import io.github.nosequel.tab.shared.entry.TabElement
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 5/23/2023
 */
class PleaseKillMeNowTabPopulator : TablistPopulator
{
    override fun displayPreProcessor(player: Player) =
        CompletableFuture.completedFuture(true)

    override fun populate(player: Player, element: TabElement)
    {
        element.add(0, 1, "horseee")
    }
}
