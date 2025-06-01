package gg.scala.events.plugin.host.visibility

import gg.scala.events.plugin.host.service.HostEventService
import gg.scala.events.plugin.host.model.PlayerState
import net.evilblock.cubed.visibility.VisibilityAction
import net.evilblock.cubed.visibility.VisibilityAdapter
import org.bukkit.entity.Player

/**
 * Host Event visibility adapter - follows CGS pattern exactly
 * Spectators can see each other but are hidden from normal players
 */
object HostEventVisibilityAdapter : VisibilityAdapter {
    
    override fun getAction(toRefresh: Player, refreshFor: Player): VisibilityAction {
        // If the player being refreshed is a spectator
        if (toRefresh.hasMetadata("spectator")) {
            // Hide spectators from non-spectators
            if (!refreshFor.hasMetadata("spectator")) {
                return VisibilityAction.HIDE
            }
        }

        // Both are spectators or both are normal players - show normally
        if (!toRefresh.hasMetadata("spectator") && !refreshFor.hasMetadata("spectator")) {
            return VisibilityAction.NEUTRAL
        }

        // Default case - let them see each other
        return VisibilityAction.NEUTRAL
    }
} 