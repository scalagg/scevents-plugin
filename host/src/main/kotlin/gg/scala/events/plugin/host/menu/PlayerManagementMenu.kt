package gg.scala.events.plugin.host.menu

import com.cryptomorin.xseries.XMaterial
import gg.scala.events.plugin.host.service.HostEventService
import gg.scala.events.plugin.host.model.PlayerState
import gg.scala.flavor.inject.Inject
import me.lucko.helper.utils.Players
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.bukkit.Tasks
import org.bukkit.entity.Player

/**
 * Menu for managing players in the event
 */
class PlayerManagementMenu : Menu("${CC.YELLOW}${CC.BOLD}Player Management") {
    
    @Inject
    lateinit var hostService: HostEventService

    init {
        placeholder = true
    }

    override fun size(buttons: Map<Int, Button>) = 54

    override fun onClose(player: Player, manualClose: Boolean) {
        if (manualClose) {
            Tasks.delayed(1L) {
                ManagementMenu().openMenu(player)
            }
        }
    }

    override fun getButtons(player: Player): Map<Int, Button> {
        val buttons = mutableMapOf<Int, Button>()
        
        // Mass Actions
        buttons[10] = ItemBuilder
            .of(XMaterial.EMERALD)
            .name("${CC.GREEN}${CC.BOLD}Revive All Players")
            .addToLore(
                "${CC.GRAY}Revive all dead players.",
                "",
                "${CC.GREEN}Click to revive all!"
            )
            .toButton { _, _ ->
                val deadPlayers = hostService.getDeadPlayers()
                deadPlayers.forEach { deadPlayer ->
                    hostService.setPlayerState(deadPlayer, PlayerState.ALIVE)
                }
                player.sendMessage("${CC.GREEN}Revived ${deadPlayers.size} players!")
                Tasks.delayed(1L) {
                    PlayerManagementMenu().openMenu(player)
                }
            }

        buttons[12] = ItemBuilder
            .of(XMaterial.REDSTONE)
            .name("${CC.RED}${CC.BOLD}Kill All Players")
            .addToLore(
                "${CC.GRAY}Kill all living players.",
                "",
                "${CC.RED}Click to kill all!"
            )
            .toButton { _, _ ->
                val alivePlayers = hostService.getAlivePlayers()
                alivePlayers.forEach { alivePlayer ->
                    hostService.setPlayerState(alivePlayer, PlayerState.DEAD)
                }
                player.sendMessage("${CC.GREEN}Killed ${alivePlayers.size} players!")
                Tasks.delayed(1L) {
                    PlayerManagementMenu().openMenu(player)
                }
            }

        buttons[14] = ItemBuilder
            .of(XMaterial.ENDER_PEARL)
            .name("${CC.AQUA}${CC.BOLD}Teleport Alive to Me")
            .addToLore(
                "${CC.GRAY}Teleport all alive players",
                "${CC.GRAY}to your location.",
                "",
                "${CC.GREEN}Click to teleport!"
            )
            .toButton { _, _ ->
                val alivePlayers = hostService.getAlivePlayers()
                alivePlayers.forEach { alivePlayer ->
                    alivePlayer.teleport(player.location)
                    alivePlayer.sendMessage("${CC.GREEN}You have been teleported to the host!")
                }
                player.sendMessage("${CC.GREEN}Teleported ${alivePlayers.size} alive players to you!")
            }

        buttons[16] = ItemBuilder
            .of(XMaterial.ENDER_EYE)
            .name("${CC.LIGHT_PURPLE}${CC.BOLD}Teleport Dead to Me")
            .addToLore(
                "${CC.GRAY}Teleport all dead players",
                "${CC.GRAY}to your location.",
                "",
                "${CC.GREEN}Click to teleport!"
            )
            .toButton { _, _ ->
                val deadPlayers = hostService.getDeadPlayers()
                deadPlayers.forEach { deadPlayer ->
                    deadPlayer.teleport(player.location)
                    deadPlayer.sendMessage("${CC.GREEN}You have been teleported to the host!")
                }
                player.sendMessage("${CC.GREEN}Teleported ${deadPlayers.size} dead players to you!")
            }

        // Individual Player Management
        var slot = 27
        Players.all().forEach { targetPlayer ->
            if (targetPlayer == player) return@forEach // Skip the host
            
            val state = hostService.getPlayerState(targetPlayer)
            val material = when (state) {
                PlayerState.ALIVE -> XMaterial.LIME_DYE
                PlayerState.DEAD -> XMaterial.RED_DYE
                PlayerState.HOST -> XMaterial.GOLD_INGOT
            }
            
            val statusColor = when (state) {
                PlayerState.ALIVE -> CC.GREEN
                PlayerState.DEAD -> CC.RED
                PlayerState.HOST -> CC.GOLD
            }
            
            buttons[slot] = ItemBuilder
                .of(material)
                .name("$statusColor${targetPlayer.name}")
                .addToLore(
                    "${CC.GRAY}Status: $statusColor${state.name}",
                    "",
                    "${CC.YELLOW}Left-click: ${if (state == PlayerState.ALIVE) "Kill" else "Revive"}",
                    "${CC.YELLOW}Right-click: Teleport to you"
                )
                .toButton { clickPlayer, clickType ->
                    if (clickPlayer == null) return@toButton
                    
                    when (clickType) {
                        org.bukkit.event.inventory.ClickType.LEFT -> {
                            if (state == PlayerState.ALIVE) {
                                hostService.setPlayerState(targetPlayer, PlayerState.DEAD)
                                clickPlayer.sendMessage("${CC.GREEN}Killed ${targetPlayer.name}!")
                            } else {
                                hostService.setPlayerState(targetPlayer, PlayerState.ALIVE)
                                clickPlayer.sendMessage("${CC.GREEN}Revived ${targetPlayer.name}!")
                            }
                            Tasks.delayed(1L) {
                                PlayerManagementMenu().openMenu(clickPlayer)
                            }
                        }
                        org.bukkit.event.inventory.ClickType.RIGHT -> {
                            targetPlayer.teleport(clickPlayer.location)
                            clickPlayer.sendMessage("${CC.GREEN}Teleported ${targetPlayer.name} to you!")
                            targetPlayer.sendMessage("${CC.GREEN}You have been teleported to the host!")
                        }
                        else -> {}
                    }
                }
            
            slot++
            if (slot >= 53) return@forEach // Don't overflow the inventory
        }

        // Back Button
        buttons[53] = ItemBuilder
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