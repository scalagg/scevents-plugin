package gg.scala.events.plugin.host.menu

import com.cryptomorin.xseries.XMaterial
import gg.scala.events.plugin.host.service.HostEventService
import gg.scala.flavor.inject.Inject
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.entity.Player

/**
 * Main management menu for host-controlled events
 */
class ManagementMenu : Menu("${CC.GOLD}${CC.BOLD}Event Management") {
    
    @Inject
    lateinit var hostService: HostEventService

    init {
        placeholder = true
    }

    override fun size(buttons: Map<Int, Button>) = 54

    override fun getButtons(player: Player): Map<Int, Button> {
        val buttons = mutableMapOf<Int, Button>()
        
        // Event Controls Menu
        buttons[10] = ItemBuilder
            .of(XMaterial.REDSTONE)
            .name("${CC.GREEN}${CC.BOLD}Event Controls")
            .addToLore(
                "${CC.GRAY}Control PVP, Building, and",
                "${CC.GRAY}other event settings.",
                "",
                "${CC.GREEN}Click to open!"
            )
            .toButton { _, _ ->
                EventControlsMenu().openMenu(player)
            }

        // Player Management Menu
        buttons[12] = ItemBuilder
            .of(XMaterial.PLAYER_HEAD)
            .name("${CC.YELLOW}${CC.BOLD}Player Management")
            .addToLore(
                "${CC.GRAY}Manage players in the event.",
                "${CC.GRAY}Revive, kill, teleport players.",
                "",
                "${CC.GREEN}Click to open!"
            )
            .toButton { _, _ ->
                PlayerManagementMenu().openMenu(player)
            }

        // Host Event Menu
        buttons[14] = ItemBuilder
            .of(XMaterial.DIAMOND)
            .name("${CC.AQUA}${CC.BOLD}Host Event")
            .addToLore(
                "${CC.GRAY}Start, end, and configure",
                "${CC.GRAY}your custom event.",
                "",
                "${CC.GREEN}Click to open!"
            )
            .toButton { _, _ ->
                HostEventMenu().openMenu(player)
            }

        // Settings Menu
        buttons[16] = ItemBuilder
            .of(XMaterial.COMPARATOR)
            .name("${CC.LIGHT_PURPLE}${CC.BOLD}Settings")
            .addToLore(
                "${CC.GRAY}Configure event settings",
                "${CC.GRAY}and preferences.",
                "",
                "${CC.GREEN}Click to open!"
            )
            .toButton { _, _ ->
                SettingsMenu().openMenu(player)
            }

        // Current Event Status
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

        return buttons
    }
} 