package gg.scala.events.plugin.host.menu

import com.cryptomorin.xseries.XMaterial
import gg.scala.events.plugin.host.service.HostEventService
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.bukkit.Tasks
import org.bukkit.entity.Player

/**
 * Menu for hosting event actions (start, end, configure)
 */
class HostEventMenu : Menu("${CC.AQUA}${CC.BOLD}Host Event") {

    init {
        placeholder = true
    }

    override fun size(buttons: Map<Int, Button>) = 27

    override fun onClose(player: Player, manualClose: Boolean) {
        if (manualClose) {
            Tasks.delayed(1L) {
                ManagementMenu().openMenu(player)
            }
        }
    }

    override fun getButtons(player: Player): Map<Int, Button> {
        val buttons = mutableMapOf<Int, Button>()
        
        val hostService = HostEventService.instance
        if (hostService == null) {
            // Service unavailable
            buttons[13] = ItemBuilder
                .of(XMaterial.BARRIER)
                .name("${CC.RED}${CC.BOLD}Service Unavailable")
                .addToLore("${CC.RED}Host Event Service is not available.")
                .toButton()
            return buttons
        }
        
        // Start Event
        buttons[10] = ItemBuilder
            .of(XMaterial.EMERALD_BLOCK)
            .name("${CC.GREEN}${CC.BOLD}Start Event")
            .addToLore(
                "${CC.GRAY}Begin the event and set all",
                "${CC.GRAY}players to alive status.",
                "",
                "${CC.GREEN}Click to start!"
            )
            .toButton { _, _ ->
                hostService.startEvent()
                player.sendMessage("${CC.GREEN}Event started!")
                player.closeInventory()
            }

        // End Event
        buttons[12] = ItemBuilder
            .of(XMaterial.REDSTONE_BLOCK)
            .name("${CC.RED}${CC.BOLD}End Event")
            .addToLore(
                "${CC.GRAY}End the current event and",
                "${CC.GRAY}reset all players.",
                "",
                "${CC.RED}Click to end!"
            )
            .toButton { _, _ ->
                hostService.endEvent()
                player.sendMessage("${CC.GREEN}Event ended!")
                player.closeInventory()
            }

        // Event Name
        val eventName = hostService.getEventName() ?: "Unnamed Event"
        buttons[14] = ItemBuilder
            .of(XMaterial.NAME_TAG)
            .name("${CC.YELLOW}${CC.BOLD}Event Name")
            .addToLore(
                "${CC.GRAY}Current name:",
                "${CC.WHITE}$eventName",
                "",
                "${CC.YELLOW}Use ${CC.WHITE}/host name <name>",
                "${CC.YELLOW}to change the event name."
            )
            .toButton()

        // Network Announcement
        buttons[16] = ItemBuilder
            .of(XMaterial.BELL)
            .name("${CC.GOLD}${CC.BOLD}Announce Event")
            .addToLore(
                "${CC.GRAY}Announce your event to",
                "${CC.GRAY}the entire network.",
                "",
                "${CC.YELLOW}Use ${CC.WHITE}/host announce <message>",
                "${CC.YELLOW}to make announcements."
            )
            .toButton()

        // Back Button
        buttons[22] = ItemBuilder
            .of(XMaterial.ARROW)
            .name("${CC.RED}${CC.BOLD}Back")
            .addToLore(
                "${CC.GRAY}Return to main menu."
            )
            .toButton { _, _ ->
                ManagementMenu().openMenu(player)
            }

        return buttons
    }
} 