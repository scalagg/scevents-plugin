package gg.scala.events.plugin.external.menu

import gg.scala.aware.message.AwareMessage
import gg.scala.aware.thread.AwareThreadContext
import gg.scala.events.shared.ScalaEventsChannel
import gg.scala.events.shared.type.EventType
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.bukkit.Tasks
import org.bukkit.entity.Player

/**
 * @author TastyCake
 * @date 8/23/2024
 */
class EventSizeMenu(private val type: EventType) : Menu("Selecting team size...") {
    init
    {
        placeholdBorders = true
    }

    override fun size(buttons: Map<Int, Button>) = 27

    override fun getButtons(player: Player) = mutableMapOf<Int, Button>().apply {
        type.effectiveSizes.forEach {
            this[10 + size] = ItemBuilder
                .copyOf(type.icon)
                .name("${CC.GREEN}${type.info.fancyNameRender} ${it.text}")
                .toButton { player, _ ->
                    player!!.sendMessage("${CC.D_GREEN}Preparing an event for you...")
                    player.closeInventory()

                    AwareMessage
                        .of(
                            "start-event",
                            ScalaEventsChannel.channel(),
                            "uniqueId" to player.uniqueId,
                            "eventType" to type.javaClass.name,
                            "eventSize" to it.name
                        )
                        .publish(
                            AwareThreadContext.ASYNC
                        )
                }
        }
    }

    override fun onClose(player: Player, manualClose: Boolean)
    {
        if (manualClose)
        {
            Tasks.sync {
                EventsViewMenu().openMenu(player)
            }
        }
    }
}
