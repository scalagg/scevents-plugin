package gg.scala.events.plugin.external.menu

import com.cryptomorin.xseries.XMaterial
import gg.scala.events.plugin.external.feature.EventStatusFeature
import gg.scala.events.shared.ScalaEventsChannel
import gg.scala.events.shared.type.EventType
import gg.scala.lemon.redirection.impl.VelocityRedirectSystem
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.serializers.Serializers
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.time.TimeUtil
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*

/**
 * @author GrowlyX
 * @since 10/3/2022
 */
class EventsMenu : Menu("Events ${Constants.DOUBLE_ARROW_RIGHT} Main")
{
    init
    {
        autoUpdate = true
        placeholder = true
    }

    override fun size(buttons: Map<Int, Button>) = 27

    override fun getButtons(player: Player): Map<Int, Button>
    {
        val buttons = mutableMapOf<Int, Button>()
        val status = EventStatusFeature.status()

        val additionalDescription = mutableListOf<String>()
        additionalDescription += ""
        additionalDescription += "${CC.GRAY}Active event:"

        val material: ItemStack

        if (status == null)
        {
            additionalDescription += "${CC.D_GRAY}No event is running!"

            material = ItemBuilder
                .of(XMaterial.GRAY_DYE)
                .build()
        } else
        {
            val type = Class.forName(status.type)
                .kotlin.objectInstance as EventType

            additionalDescription += "${CC.GRAY}Event: ${CC.WHITE}${
                type.info.fancyNameRender
            }"
            additionalDescription += "${CC.GRAY}Started: ${CC.WHITE}${
                TimeUtil.formatIntoMMSS(
                    ((System.currentTimeMillis() - status.startTime) / 1000).toInt()
                )
            }"

            material = type.icon
        }

        additionalDescription += ""
        additionalDescription += "${CC.GREEN}${
            if (status == null) "${CC.RED}No event is running." else "Click to join!"
        }"

        buttons[11] = ItemBuilder
            .copyOf(material)
            .name("${CC.B_GREEN}Join event")
            .addToLore(
                "${CC.WHITE}Join an active event!"
            )
            .addToLore(
                *additionalDescription.toTypedArray()
            )
            .toButton { _, _ ->
                if (status != null)
                {
                    VelocityRedirectSystem.redirect(player, status.serverId)
                    return@toButton
                }

                player.sendMessage("${CC.RED}There is no event running!")
            }

        buttons[13] = ItemBuilder
            .of(XMaterial.COMPASS)
            .name("${CC.B_GREEN}Spectate event")
            .addToLore(
                "${CC.WHITE}Spectate an active event!"
            )
            .addToLore(
                *additionalDescription.toTypedArray()
            )
            .toButton { _, _ ->
                if (status != null)
                {
                    VelocityRedirectSystem.redirect(player, status.serverId)
                    return@toButton
                }

                player.sendMessage("${CC.RED}There is no event running!")
            }

        buttons[15] = ItemBuilder
            .of(XMaterial.PAPER)
            .name("${CC.B_GREEN}Host event")
            .addToLore(
                "${CC.WHITE}Select an event to host.",
                "",
                "${CC.AQUA}Prime${CC.WHITE} rank is",
                "${CC.WHITE}required to host events.",
                "",
                "${CC.GREEN}Click to view events!"
            )
            .toButton { _, _ ->
                if (status != null)
                {
                    player.sendMessage("${CC.RED}An event has already started!")
                    return@toButton
                }

                if (!player.hasPermission("scevents.host-events"))
                {
                    player.sendMessage(
                        "${CC.RED}You don't have permission to host events!"
                    )
                    player.sendMessage(
                        "${CC.RED}Purchase the ${CC.AQUA}Prime${CC.RED} rank on ${CC.YELLOW}store.liftgate.io${CC.RED} to host events!"
                    )
                    return@toButton
                }

                EventsViewMenu().openMenu(player)
            }


        return buttons
    }
}
