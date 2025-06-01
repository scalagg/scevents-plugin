package gg.scala.events.plugin.host.menu

import com.cryptomorin.xseries.XMaterial
import gg.scala.events.plugin.host.service.HostEventService
import gg.scala.flavor.inject.Inject
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.bukkit.Tasks
import org.bukkit.entity.Player

/**
 * Menu for configuring event settings and preferences
 */
class SettingsMenu : Menu("${CC.LIGHT_PURPLE}${CC.BOLD}Settings") {
    
    @Inject
    lateinit var hostService: HostEventService

    init {
        placeholder = true
    }

    override fun size(buttons: Map<Int, Button>) = 36

    override fun onClose(player: Player, manualClose: Boolean) {
        if (manualClose) {
            Tasks.delayed(1L) {
                ManagementMenu().openMenu(player)
            }
        }
    }

    override fun getButtons(player: Player): Map<Int, Button> {
        val buttons = mutableMapOf<Int, Button>()
        
        val config = hostService.getSetupConfig()
        
        // Max Players Setting
        buttons[10] = ItemBuilder
            .of(XMaterial.PLAYER_HEAD)
            .name("${CC.YELLOW}${CC.BOLD}Max Players")
            .addToLore(
                "${CC.GRAY}Maximum players allowed",
                "${CC.GRAY}in the event.",
                "",
                "${CC.YELLOW}Current: ${CC.WHITE}${config["maxPlayers"]}",
                "",
                "${CC.YELLOW}Use ${CC.WHITE}/host setup maxplayers <num>",
                "${CC.YELLOW}to change this setting."
            )
            .toButton()

        // Min Players Setting
        buttons[11] = ItemBuilder
            .of(XMaterial.SKELETON_SKULL)
            .name("${CC.YELLOW}${CC.BOLD}Min Players")
            .addToLore(
                "${CC.GRAY}Minimum players required",
                "${CC.GRAY}to start the event.",
                "",
                "${CC.YELLOW}Current: ${CC.WHITE}${config["minPlayers"]}",
                "",
                "${CC.YELLOW}Use ${CC.WHITE}/host setup minplayers <num>",
                "${CC.YELLOW}to change this setting."
            )
            .toButton()

        // Auto-Start Toggle
        val autoStart = config["autoStart"] as Boolean
        buttons[12] = ItemBuilder
            .of(if (autoStart) XMaterial.EMERALD else XMaterial.REDSTONE)
            .name("${CC.AQUA}${CC.BOLD}Auto-Start")
            .addToLore(
                "${CC.GRAY}Automatically start when",
                "${CC.GRAY}minimum players join.",
                "",
                "${CC.YELLOW}Status: ${if (autoStart) "${CC.GREEN}ENABLED" else "${CC.RED}DISABLED"}",
                "",
                "${CC.YELLOW}Use ${CC.WHITE}/host setup autostart",
                "${CC.YELLOW}to toggle this setting."
            )
            .toButton()

        // Event Description
        buttons[14] = ItemBuilder
            .of(XMaterial.BOOK)
            .name("${CC.YELLOW}${CC.BOLD}Event Description")
            .addToLore(
                "${CC.GRAY}Description shown when",
                "${CC.GRAY}announcing the event.",
                "",
                "${CC.YELLOW}Current:",
                "${CC.WHITE}${config["description"]}",
                "",
                "${CC.YELLOW}Use ${CC.WHITE}/host setup description <text>",
                "${CC.YELLOW}to change this setting."
            )
            .toButton()

        // Current Configuration Summary
        buttons[22] = ItemBuilder
            .of(XMaterial.WRITABLE_BOOK)
            .name("${CC.GOLD}${CC.BOLD}Configuration Summary")
            .addToLore(
                "${CC.GRAY}Current event settings:",
                "",
                "${CC.YELLOW}Name: ${CC.WHITE}${config["eventName"]}",
                "${CC.YELLOW}Description: ${CC.WHITE}${config["description"]}",
                "${CC.YELLOW}Players: ${CC.WHITE}${config["minPlayers"]}-${config["maxPlayers"]}",
                "${CC.YELLOW}Auto-Start: ${if (config["autoStart"] as Boolean) "${CC.GREEN}ON" else "${CC.RED}OFF"}",
                "${CC.YELLOW}PVP: ${if (config["pvpEnabled"] as Boolean) "${CC.GREEN}ON" else "${CC.RED}OFF"}",
                "${CC.YELLOW}Building: ${if (config["buildEnabled"] as Boolean) "${CC.GREEN}ON" else "${CC.RED}OFF"}",
                "${CC.YELLOW}Setup Mode: ${if (config["setupMode"] as Boolean) "${CC.GREEN}ACTIVE" else "${CC.RED}INACTIVE"}"
            )
            .toButton()

        // Enter Setup Mode
        val setupMode = config["setupMode"] as Boolean
        buttons[24] = ItemBuilder
            .of(if (setupMode) XMaterial.REDSTONE_BLOCK else XMaterial.EMERALD_BLOCK)
            .name("${CC.AQUA}${CC.BOLD}Setup Mode")
            .addToLore(
                "${CC.GRAY}Enter configuration mode",
                "${CC.GRAY}to modify event settings.",
                "",
                "${CC.YELLOW}Status: ${if (setupMode) "${CC.GREEN}ACTIVE" else "${CC.RED}INACTIVE"}",
                "",
                "${CC.GREEN}Click to ${if (setupMode) "exit" else "enter"} setup mode!"
            )
            .toButton { _, _ ->
                hostService.toggleSetupMode()
                player.sendMessage("${CC.GREEN}Setup mode ${if (!setupMode) "enabled" else "disabled"}!")
                Tasks.delayed(1L) {
                    SettingsMenu().openMenu(player)
                }
            }

        // Back Button
        buttons[31] = ItemBuilder
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