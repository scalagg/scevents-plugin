package gg.scala.events.plugin.host.menu

import com.cryptomorin.xseries.XMaterial
import gg.scala.events.plugin.host.service.HostEventService
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.entity.Player

/**
 * Main management menu for host-controlled events
 */
class ManagementMenu : Menu("${CC.GOLD}${CC.BOLD}Event Management") {

    init {
        placeholder = true
    }

    override fun size(buttons: Map<Int, Button>) = 54

    override fun getButtons(player: Player): Map<Int, Button> {
        val buttons = mutableMapOf<Int, Button>()
        
        val hostService = HostEventService.instance
        
        // Event Controls Menu
        buttons[10] = ItemBuilder
            .of(XMaterial.REDSTONE)
            .name("${CC.GREEN}${CC.BOLD}Event Controls")
            .addToLore(
                "${CC.GRAY}Control PVP, Building, and",
                "${CC.GRAY}other event settings.",
                "",
                if (hostService != null) "${CC.GREEN}Click to open!" else "${CC.RED}Service unavailable!"
            )
            .toButton { _, _ ->
                if (hostService != null) {
                    EventControlsMenu().openMenu(player)
                } else {
                    player.sendMessage("${CC.RED}Host Event Service is not available!")
                }
            }

        // Player Management Menu
        buttons[12] = ItemBuilder
            .of(XMaterial.PLAYER_HEAD)
            .name("${CC.YELLOW}${CC.BOLD}Player Management")
            .addToLore(
                "${CC.GRAY}Manage players in the event.",
                "${CC.GRAY}Revive, kill, teleport players.",
                "",
                if (hostService != null) "${CC.GREEN}Click to open!" else "${CC.RED}Service unavailable!"
            )
            .toButton { _, _ ->
                if (hostService != null) {
                    PlayerManagementMenu().openMenu(player)
                } else {
                    player.sendMessage("${CC.RED}Host Event Service is not available!")
                }
            }

        // Host Event Menu
        buttons[14] = ItemBuilder
            .of(XMaterial.DIAMOND)
            .name("${CC.AQUA}${CC.BOLD}Host Event")
            .addToLore(
                "${CC.GRAY}Start, end, and configure",
                "${CC.GRAY}your custom event.",
                "",
                if (hostService != null) "${CC.GREEN}Click to open!" else "${CC.RED}Service unavailable!"
            )
            .toButton { _, _ ->
                if (hostService != null) {
                    HostEventMenu().openMenu(player)
                } else {
                    player.sendMessage("${CC.RED}Host Event Service is not available!")
                }
            }

        // Settings Menu
        buttons[16] = ItemBuilder
            .of(XMaterial.COMPARATOR)
            .name("${CC.LIGHT_PURPLE}${CC.BOLD}Settings")
            .addToLore(
                "${CC.GRAY}Configure event settings",
                "${CC.GRAY}and preferences.",
                "",
                if (hostService != null) "${CC.GREEN}Click to open!" else "${CC.RED}Service unavailable!"
            )
            .toButton { _, _ ->
                if (hostService != null) {
                    SettingsMenu().openMenu(player)
                } else {
                    player.sendMessage("${CC.RED}Host Event Service is not available!")
                }
            }

        // Current Event Status
        if (hostService != null) {
            val alivePlayers = hostService.getAlivePlayers()
            val deadPlayers = hostService.getDeadPlayers()
            
            buttons[31] = ItemBuilder
                .of(XMaterial.BOOK)
                .name("${CC.GOLD}${CC.BOLD}Event Status")
                .addToLore(
                    "${CC.YELLOW}Event: ${CC.WHITE}${hostService.getEventName()}",
                    "${CC.YELLOW}Host: ${CC.WHITE}${player.name}",
                    "",
                    "${CC.GREEN}Alive: ${CC.WHITE}${alivePlayers.size}",
                    "${CC.RED}Dead: ${CC.WHITE}${deadPlayers.size}",
                    "${CC.GRAY}Total: ${CC.WHITE}${alivePlayers.size + deadPlayers.size}",
                    "",
                    "${CC.YELLOW}PVP: ${if (hostService.isPvpEnabled()) "${CC.GREEN}ON" else "${CC.RED}OFF"}",
                    "${CC.YELLOW}Building: ${if (hostService.isBuildEnabled()) "${CC.GREEN}ON" else "${CC.RED}OFF"}"
                )
                .toButton()
        } else {
            buttons[31] = ItemBuilder
                .of(XMaterial.BARRIER)
                .name("${CC.RED}${CC.BOLD}Service Unavailable")
                .addToLore(
                    "${CC.RED}Host Event Service is not available.",
                    "${CC.GRAY}Please try again later."
                )
                .toButton()
        }

        return buttons
    }
} 