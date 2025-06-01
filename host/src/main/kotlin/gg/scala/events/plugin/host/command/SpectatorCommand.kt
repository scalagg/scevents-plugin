package gg.scala.events.plugin.host.command

import gg.scala.commons.acf.annotation.*
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.events.plugin.host.model.PlayerState
import gg.scala.events.plugin.host.service.HostEventService
import net.evilblock.cubed.util.CC
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * Commands for spectators to teleport and navigate
 */
@AutoRegister
@CommandAlias("spec|spectate")
object SpectatorCommand : ScalaCommand() {
    
    private fun getHostService(): HostEventService? {
        return HostEventService.instance
    }
    
    private fun withHostService(player: ScalaPlayer, action: (HostEventService) -> Unit) {
        val hostService = getHostService() ?: run {
            player.sendMessage("${CC.RED}Event service not available!")
            return
        }
        action(hostService)
    }

    @Default
    @Subcommand("tp")
    @Description("Teleport to another player (spectators only)")
    @CommandCompletion("@players")
    fun onTeleport(player: ScalaPlayer, targetName: String) {
        withHostService(player) { hostService ->
            if (hostService.getPlayerState(player.bukkit()) != PlayerState.SPECTATOR) {
                player.sendMessage("${CC.RED}Only spectators can use this command!")
                return@withHostService
            }
            
            val targetPlayer = Bukkit.getPlayer(targetName)
            if (targetPlayer == null) {
                player.sendMessage("${CC.RED}Player not found!")
                return@withHostService
            }
            
            if (targetPlayer == player.bukkit()) {
                player.sendMessage("${CC.RED}You cannot teleport to yourself!")
                return@withHostService
            }
            
            player.bukkit().teleport(targetPlayer.location)
            player.sendMessage("${CC.GREEN}Teleported to ${CC.YELLOW}${targetPlayer.name}${CC.GREEN}!")
        }
    }

    @Subcommand("list")
    @Description("List all players you can teleport to")
    fun onList(player: ScalaPlayer) {
        withHostService(player) { hostService ->
            if (hostService.getPlayerState(player.bukkit()) != PlayerState.SPECTATOR) {
                player.sendMessage("${CC.RED}Only spectators can use this command!")
                return@withHostService
            }
            
            val alivePlayers = hostService.getAlivePlayers()
            val deadPlayers = hostService.getDeadPlayers()
            val spectatorPlayers = hostService.getSpectatorPlayers().filter { it != player.bukkit() }
            val hostPlayer = hostService.getCurrentHost()?.let { Bukkit.getPlayer(it) }
            
            player.sendMessage("")
            player.sendMessage("${CC.AQUA}${CC.BOLD}SPECTATOR TELEPORT")
            player.sendMessage("${CC.GRAY}Use ${CC.YELLOW}/spec tp <player>${CC.GRAY} to teleport")
            player.sendMessage("")
            
            if (hostPlayer != null) {
                player.sendMessage("${CC.GOLD}Host: ${CC.WHITE}${hostPlayer.name}")
            }
            
            if (alivePlayers.isNotEmpty()) {
                val aliveList = alivePlayers.joinToString("${CC.WHITE}, ${CC.GREEN}") { it.name }
                player.sendMessage("${CC.GREEN}Alive: ${CC.GREEN}$aliveList")
            }
            
            if (deadPlayers.isNotEmpty()) {
                val deadList = deadPlayers.joinToString("${CC.WHITE}, ${CC.RED}") { it.name }
                player.sendMessage("${CC.RED}Dead: ${CC.RED}$deadList")
            }
            
            if (spectatorPlayers.isNotEmpty()) {
                val spectatorList = spectatorPlayers.joinToString("${CC.WHITE}, ${CC.AQUA}") { it.name }
                player.sendMessage("${CC.AQUA}Other Spectators: ${CC.AQUA}$spectatorList")
            }
            
            player.sendMessage("")
        }
    }

    @Subcommand("help")
    @Description("Show spectator commands")
    fun onHelp(player: ScalaPlayer) {
        player.sendMessage("")
        player.sendMessage("${CC.AQUA}${CC.BOLD}SPECTATOR COMMANDS")
        player.sendMessage("${CC.GRAY}${CC.STRIKE_THROUGH}${"-".repeat(25)}")
        player.sendMessage("${CC.YELLOW}/spec tp <player>${CC.GRAY} - Teleport to a player")
        player.sendMessage("${CC.YELLOW}/spec list${CC.GRAY} - List all players")
        player.sendMessage("${CC.YELLOW}/spec help${CC.GRAY} - Show this help")
        player.sendMessage("")
        player.sendMessage("${CC.GRAY}You are invisible to players but can see other spectators!")
        player.sendMessage("")
    }
} 