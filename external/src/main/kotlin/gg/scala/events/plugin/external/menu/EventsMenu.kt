package gg.scala.events.plugin.external.menu

import com.cryptomorin.xseries.XMaterial
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 10/3/2022
 */
class EventsMenu : Menu("Events")
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
                "${CC.GRAY}Host an event!",
                "",
                "${CC.L_PURPLE}Events:",
                "${CC.WHITE} - One in the Chamber",
                "${CC.WHITE}   * FFA",
                "",
                "${CC.GREEN}Click to join!"
            )
            .toButton { _, _ ->
                player.sendMessage(
                    "${CC.RED}You don't have permission to host events!"
                )
                player.sendMessage(
                    "${CC.RED}Purchase the ${CC.AQUA}Prime${CC.RED} rank on ${CC.YELLOW}store.minequest.gg${CC.RED} to host events!"
                )
            }


        return buttons
    }
}
