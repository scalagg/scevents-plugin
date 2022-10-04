package gg.scala.events.plugin.external.menu

import com.cryptomorin.xseries.XMaterial
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 10/3/2022
 */
class EventsMenu : Menu()
{
    override fun size(buttons: Map<Int, Button>) = 27

    override fun getButtons(player: Player): Map<Int, Button>
    {
        val buttons = mutableMapOf<Int, Button>()

        buttons[11] = ItemBuilder
            .of(XMaterial.LIME_DYE)
            .toButton()

        return buttons
    }
}
