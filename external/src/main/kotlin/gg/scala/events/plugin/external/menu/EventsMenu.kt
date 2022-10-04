package gg.scala.events.plugin.external.menu

import com.cryptomorin.xseries.XMaterial
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 10/3/2022
 */
class EventsMenu : Menu("Events ${Constants.DOUBLE_ARROW_RIGHT} Main")
{
    override fun size(buttons: Map<Int, Button>) = 27

    override fun getButtons(player: Player): Map<Int, Button>
    {
        val buttons = mutableMapOf<Int, Button>()

        buttons[11] = ItemBuilder
            .of(XMaterial.LIME_DYE)
            .name("${CC.B_GREEN}Join event")
            .addToLore(
                "${CC.WHITE}Join an active event!",
                "",
                "${CC.GRAY}Active event:",
                "${CC.RED}None",
                "",
                "${CC.GREEN}Click to join!"
            )
            .toButton { _, _ ->
                player.sendMessage("${CC.RED}There is no event running!")
            }

        buttons[13] = ItemBuilder
            .of(XMaterial.COMPASS)
            .name("${CC.B_GREEN}Spectate event")
            .addToLore(
                "${CC.WHITE}Spectate an active event!",
                "",
                "${CC.GRAY}Active event:",
                "${CC.RED}None",
                "",
                "${CC.GREEN}Click to join!"
            )
            .toButton { _, _ ->
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
                "${CC.RED}Event hosting is temporarily disabled!",
                "",
                "${CC.GREEN}Click to view events!"
            )
            .toButton { _, _ ->
                if (!player.hasPermission("scevents.host-events"))
                {
                    player.sendMessage(
                        "${CC.RED}You don't have permission to host events!"
                    )
                    player.sendMessage(
                        "${CC.RED}Purchase the ${CC.AQUA}Prime${CC.RED} rank on ${CC.YELLOW}store.minequest.gg${CC.RED} to host events!"
                    )
                    return@toButton
                }

                EventsViewMenu().openMenu(player)
            }


        return buttons
    }
}
