package gg.scala.events.plugin.host.service

import gg.scala.events.plugin.host.model.PlayerState
import gg.scala.flavor.service.Service
import me.lucko.helper.utils.Players
import net.evilblock.cubed.scoreboard.ScoreboardAdapter
import net.evilblock.cubed.scoreboard.ScoreboardAdapterRegister
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

/**
 * Scoreboard adapter for host-controlled events
 */
@ScoreboardAdapterRegister
object HostEventScoreboardAdapter : ScoreboardAdapter() {
    
    init {
        println("[HostEventScoreboard] Scoreboard adapter initialized!")
    }
    
    private fun getHostService(): HostEventService? {
        val service = HostEventService.instance
        if (service != null) {
            // Only print once when service becomes available
            if (!serviceAvailable) {
                println("[HostEventScoreboard] Service became available!")
                serviceAvailable = true
            }
        }
        return service
    }
    
    private var serviceAvailable = false

    override fun getLines(board: LinkedList<String>, player: Player) {
        val hostService = getHostService()
        
        if (hostService != null) {
            // Event name
            board += "${CC.WHITE}Event: ${CC.PRI}${hostService.getEventName()}"
            
            // Host information
            val currentHost = hostService.getCurrentHost()
            val hostName = currentHost?.let { Bukkit.getPlayer(it)?.name } ?: "None"
            val hostStatus = if (currentHost != null) "${CC.PRI}$hostName" else "${CC.RED}No Host"
            board += "${CC.WHITE}Host: $hostStatus"
            board += ""
            
            // Player state information
            val playerState = hostService.getPlayerState(player)
            val stateDisplay = when (playerState) {
                PlayerState.ALIVE -> "${CC.GREEN}ALIVE"
                PlayerState.DEAD -> "${CC.RED}DEAD"
                PlayerState.HOST -> "${CC.GOLD}HOST"
                PlayerState.SPECTATOR -> "${CC.AQUA}SPECTATING"
            }
            board += "${CC.WHITE}Status: $stateDisplay"
            board += ""
            
            // Player counts (condensed)
            val alivePlayers = hostService.getAlivePlayers()
            val deadPlayers = hostService.getDeadPlayers()
            val spectatorPlayers = hostService.getSpectatorPlayers()
            
            board += "${CC.GREEN}${alivePlayers.size} ${CC.WHITE}alive ${CC.RED}${deadPlayers.size} ${CC.WHITE}dead"
            if (spectatorPlayers.isNotEmpty()) {
                board += "${CC.AQUA}${spectatorPlayers.size} ${CC.WHITE}spectating"
            }
            board += ""
            
            // Event settings (condensed)
            val pvpStatus = if (hostService.isPvpEnabled()) "${CC.GREEN}${Constants.CHECK_SYMBOL}" else "${CC.RED}${Constants.X_SYMBOL}"
            val buildStatus = if (hostService.isBuildEnabled()) "${CC.GREEN}${Constants.CHECK_SYMBOL}" else "${CC.RED}${Constants.X_SYMBOL}"
            board += "${CC.WHITE}PVP: $pvpStatus ${CC.WHITE}Build: $buildStatus"
            
            // Setup mode indicator (only if active)
            if (hostService.isInSetupMode()) {
                board += "${CC.AQUA}${CC.BOLD}SETUP MODE"
            }
            
            board += ""
            
            // Help text (shorter)
            if (hostService.isHost(player)) {
                board += "${CC.GRAY}/host help"
            } else if (player.hasPermission("scevents.staff")) {
                board += "${CC.GRAY}/hs help"
            }
        } else {
            // Show default content when service isn't ready (simplified)
            board += "${CC.WHITE}Event: ${CC.GRAY}Loading..."
            board += "${CC.WHITE}Host: ${CC.RED}None"
            board += ""
            board += "${CC.WHITE}Status: ${CC.GRAY}Waiting..."
            board += ""
            board += "${CC.WHITE}Players: ${CC.PRI}${Players.all().size}"
            board += ""
            board += "${CC.GRAY}Starting up..."
        }
        
        board += ""
    }

    override fun getTitle(player: Player) = "${CC.B_PRI}Scala Events"
} 