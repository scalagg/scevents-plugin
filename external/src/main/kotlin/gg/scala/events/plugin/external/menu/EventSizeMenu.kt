package gg.scala.events.plugin.external.menu

import gg.scala.aware.message.AwareMessage
import gg.scala.aware.thread.AwareThreadContext
import gg.scala.events.shared.ModuleSize
import gg.scala.events.shared.ScalaEventsChannel
import gg.scala.events.shared.type.EventType
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.bukkit.Tasks
import org.bukkit.Material
import org.bukkit.entity.Player

/**
 * @author TastyCake
 * @date 8/23/2024
 */
class EventSizeMenu(
    var type: EventType
) : Menu("Select team size for ${type.info.fancyNameRender}${CC.RESET} event") {
    init
    {
        placeholdBorders = true
    }

    override fun size(buttons: Map<Int, Button>) = 27

    override fun getButtons(player: Player): Map<Int, Button> {
        return mutableMapOf<Int, Button>().apply {
            var index = 10
            EventType.TYPES[type]?.forEach {
                this[index] = ItemBuilder
                    .copyOf(type.icon)
                    .name("${type.info.fancyNameRender} ${it.text}")
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
                                AwareThreadContext.SYNC
                            )
                    }
                index++
            }
        }
    }

    override fun onClose(player: Player, manualClose: Boolean)
    {
        if (manualClose)
        {
            Tasks.delayed(1L)
            {
                EventsViewMenu().openMenu(player)
            }
        }
    }
}