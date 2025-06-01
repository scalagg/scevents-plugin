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
 * Menu for controlling event settings like PVP and building
 */
class EventControlsMenu : Menu("${CC.GREEN}${CC.BOLD}Event Controls") {

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
        
        // PVP Toggle
        val pvpEnabled = hostService.isPvpEnabled()
        buttons[10] = ItemBuilder
            .of(if (pvpEnabled) XMaterial.DIAMOND_SWORD else XMaterial.WOODEN_SWORD)
            .name("${CC.YELLOW}${CC.BOLD}PVP Control")
            .addToLore(
                "${CC.GRAY}Control player vs player combat.",
                "",
                "${CC.YELLOW}Status: ${if (pvpEnabled) "${CC.GREEN}ENABLED" else "${CC.RED}DISABLED"}",
                "",
                "${CC.GREEN}Click to ${if (pvpEnabled) "disable" else "enable"}!"
            )
            .toButton { _, _ ->
                hostService.togglePvp()
                player.sendMessage("${CC.GREEN}PVP ${if (!pvpEnabled) "enabled" else "disabled"}!")
                Tasks.delayed(1L) {
                    EventControlsMenu().openMenu(player)
                }
            }

        // Building Toggle
        val buildEnabled = hostService.isBuildEnabled()
        buttons[12] = ItemBuilder
            .of(if (buildEnabled) XMaterial.DIAMOND_PICKAXE else XMaterial.WOODEN_PICKAXE)
            .name("${CC.YELLOW}${CC.BOLD}Building Control")
            .addToLore(
                "${CC.GRAY}Control block breaking/placing.",
                "",
                "${CC.YELLOW}Status: ${if (buildEnabled) "${CC.GREEN}ENABLED" else "${CC.RED}DISABLED"}",
                "",
                "${CC.GREEN}Click to ${if (buildEnabled) "disable" else "enable"}!"
            )
            .toButton { _, _ ->
                hostService.toggleBuild()
                player.sendMessage("${CC.GREEN}Building ${if (!buildEnabled) "enabled" else "disabled"}!")
                Tasks.delayed(1L) {
                    EventControlsMenu().openMenu(player)
                }
            }

        // Setup Mode Toggle
        val setupMode = hostService.isInSetupMode()
        buttons[14] = ItemBuilder
            .of(if (setupMode) XMaterial.REDSTONE_BLOCK else XMaterial.REDSTONE)
            .name("${CC.AQUA}${CC.BOLD}Setup Mode")
            .addToLore(
                "${CC.GRAY}Configure event parameters.",
                "",
                "${CC.YELLOW}Status: ${if (setupMode) "${CC.GREEN}ACTIVE" else "${CC.RED}INACTIVE"}",
                "",
                "${CC.GREEN}Click to ${if (setupMode) "exit" else "enter"} setup!"
            )
            .toButton { _, _ ->
                hostService.toggleSetupMode()
                player.sendMessage("${CC.GREEN}Setup mode ${if (!setupMode) "enabled" else "disabled"}!")
                Tasks.delayed(1L) {
                    EventControlsMenu().openMenu(player)
                }
            }

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