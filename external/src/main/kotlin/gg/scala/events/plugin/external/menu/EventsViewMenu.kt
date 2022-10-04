package gg.scala.events.plugin.external.menu

import gg.scala.aware.message.AwareMessage
import gg.scala.aware.thread.AwareThreadContext
import gg.scala.events.plugin.external.feature.EventStatusFeature
import gg.scala.events.shared.ScalaEventsChannel
import gg.scala.events.shared.type.EventType
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.bukkit.Tasks
import net.evilblock.cubed.util.text.TextSplitter
import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 10/3/2022
 */
class EventsViewMenu : Menu(
    "Events ${Constants.DOUBLE_ARROW_RIGHT} Select an Event"
)
{
    init
    {
        placeholdBorders = true
    }

    override fun size(buttons: Map<Int, Button>) = 27

    override fun onClose(player: Player, manualClose: Boolean)
    {
        if (manualClose)
        {
            Tasks.delayed(1L)
            {
                EventsMenu().openMenu(player)
            }
        }
    }

    override fun getButtons(player: Player): Map<Int, Button>
    {
        val buttons = mutableMapOf<Int, Button>()
        val status = EventStatusFeature.status()

        for (type in EventType.TYPES)
        {
            buttons[10 + buttons.size] =
                ItemBuilder
                    .copyOf(type.icon)
                    .name("${CC.B_GREEN}${type.info.fancyNameRender}")
                    .setLore(
                        TextSplitter.split(
                            text = type.description,
                            linePrefix = CC.WHITE
                        )
                    )
                    .addToLore(
                        "",
                        "${CC.GREEN}Click to host!"
                    )
                    .toButton { _, _ ->
                        if (status != null)
                        {
                            player.sendMessage("${CC.RED}An event has already started!")
                            return@toButton
                        }

                        player.sendMessage("${CC.D_GREEN}Preparing an event for you...")
                        player.closeInventory()

                        AwareMessage
                            .of(
                                "start-event",
                                ScalaEventsChannel.channel(),
                                "uniqueId" to player.uniqueId,
                                "eventType" to type.javaClass.name
                            )
                            .publish(
                                AwareThreadContext.ASYNC
                            )
                    }
        }

        return buttons
    }
}
