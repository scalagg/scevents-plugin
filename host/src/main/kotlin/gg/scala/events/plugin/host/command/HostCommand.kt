package gg.scala.events.plugin.host.command

import gg.scala.commons.acf.annotation.*
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.events.plugin.host.model.PlayerState
import gg.scala.events.plugin.host.service.HostEventService
import gg.scala.events.plugin.host.menu.ManagementMenu
import gg.scala.events.plugin.host.command.HostAnnounceCommand
import net.evilblock.cubed.util.CC
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import gg.scala.commons.acf.CommandHelp

/**
 * Main host command system for event control
 */
@AutoRegister
@CommandAlias("host")
@CommandPermission("scevents.host")
object HostCommand : ScalaCommand() {
    
    private fun getHostService(): HostEventService? {
        return HostEventService.instance
    }
    
    private fun withHostService(player: ScalaPlayer, action: (HostEventService) -> Unit) {
        val hostService = getHostService() ?: run {
            player.sendMessage("${CC.RED}Event service not available!")
            player.sendMessage("${CC.GRAY}Please wait a moment for the system to initialize...")
            player.sendMessage("${CC.GRAY}Try using ${CC.YELLOW}/hs status${CC.GRAY} (staff) to check system status")
            return
        }
        action(hostService)
    }

    @Default
    @HelpCommand
    @Subcommand("help")
    fun onHelp(player: ScalaPlayer, help: CommandHelp) {
        withHostService(player) { hostService ->
            if (!hostService.isHost(player.bukkit())) {
                player.sendMessage("${CC.RED}You are not the event host!")
                return@withHostService
            }
            
            player.sendMessage("")
            player.sendMessage("${CC.GOLD}${CC.BOLD}HOST COMMANDS")
            player.sendMessage("${CC.GRAY}${CC.STRIKE_THROUGH}${"-".repeat(30)}")
            player.sendMessage("${CC.YELLOW}/host sethost${CC.GRAY} - Become the event host")
            player.sendMessage("${CC.YELLOW}/host panel${CC.GRAY} - Open management GUI")
            player.sendMessage("${CC.YELLOW}/host start${CC.GRAY} - Start the event")
            player.sendMessage("${CC.YELLOW}/host end${CC.GRAY} - End the event")
            player.sendMessage("${CC.YELLOW}/host name <name>${CC.GRAY} - Set event name")
            player.sendMessage("${CC.YELLOW}/host revive <player>${CC.GRAY} - Revive a dead player")
            player.sendMessage("${CC.YELLOW}/host kill <player>${CC.GRAY} - Kill a living player")
            player.sendMessage("${CC.YELLOW}/host spectate <player>${CC.GRAY} - Make player spectator")
            player.sendMessage("${CC.YELLOW}/host unspectate <player>${CC.GRAY} - Remove from spectator mode")
            player.sendMessage("${CC.YELLOW}/host reviveall${CC.GRAY} - Revive all dead players")
            player.sendMessage("${CC.YELLOW}/host killall${CC.GRAY} - Kill all living players")
            player.sendMessage("${CC.YELLOW}/host tpalive${CC.GRAY} - Teleport all alive players to you")
            player.sendMessage("${CC.YELLOW}/host tpdead${CC.GRAY} - Teleport all dead players to you")
            player.sendMessage("${CC.YELLOW}/host players${CC.GRAY} - List all players with status")
            player.sendMessage("${CC.YELLOW}/host announce [message]${CC.GRAY} - Announce to network")
            player.sendMessage("${CC.YELLOW}/host quit${CC.GRAY} - Stop hosting")
            player.sendMessage("")
        }
    }

    @Subcommand("debug")
    @Description("Debug the host event system")
    @CommandPermission("scevents.staff")
    fun onDebug(player: ScalaPlayer) {
        player.sendMessage("")
        player.sendMessage("${CC.GOLD}${CC.BOLD}HOST EVENT SYSTEM DEBUG")
        player.sendMessage("${CC.GRAY}${CC.STRIKE_THROUGH}${"-".repeat(30)}")
        player.sendMessage("${CC.YELLOW}Service Instance: ${if (HostEventService.instance != null) "${CC.GREEN}✅ Available" else "${CC.RED}❌ Not Available"}")
        
        val service = HostEventService.instance
        if (service != null) {
            player.sendMessage("${CC.YELLOW}Current Host: ${CC.WHITE}${service.getCurrentHost()?.let { Bukkit.getPlayer(it)?.name } ?: "None"}")
            player.sendMessage("${CC.YELLOW}Event Name: ${CC.WHITE}${service.getEventName()}")
            player.sendMessage("${CC.YELLOW}PVP Enabled: ${if (service.isPvpEnabled()) "${CC.GREEN}Yes" else "${CC.RED}No"}")
            player.sendMessage("${CC.YELLOW}Building Enabled: ${if (service.isBuildEnabled()) "${CC.GREEN}Yes" else "${CC.RED}No"}")
        }
        player.sendMessage("")
    }

    @Subcommand("sethost")
    @Description("Become the event host")
    fun onSetHost(player: ScalaPlayer) {
        withHostService(player) { hostService ->
            val success = hostService.setHost(player.bukkit())
            if (!success) {
                player.sendMessage("${CC.RED}There is already an event host!")
            }
        }
    }

    @Subcommand("quit")
    @Description("Stop hosting the event")
    fun onQuit(player: ScalaPlayer) {
        withHostService(player) { hostService ->
            if (!hostService.isHost(player.bukkit())) {
                player.sendMessage("${CC.RED}You are not the event host!")
                return@withHostService
            }
            
            hostService.removeHost()
            player.sendMessage("${CC.YELLOW}You are no longer hosting the event.")
        }
    }

    @Subcommand("name")
    @Description("Set the event name")
    @CommandCompletion("EventName")
    fun onSetName(player: ScalaPlayer, @Single eventName: String) {
        withHostService(player) { hostService ->
            if (!hostService.isHost(player.bukkit())) {
                player.sendMessage("${CC.RED}You are not the event host!")
                return@withHostService
            }
            
            hostService.setEventName(eventName)
            player.sendMessage("${CC.GREEN}Event name set to: ${CC.YELLOW}$eventName")
        }
    }

    @Subcommand("revive")
    @Description("Revive a dead player")
    @CommandCompletion("@players")
    fun onRevive(player: ScalaPlayer, target: String) {
        withHostService(player) { hostService ->
            if (!hostService.isHost(player.bukkit())) {
                player.sendMessage("${CC.RED}You are not the event host!")
                return@withHostService
            }
            
            val targetPlayer = Bukkit.getPlayer(target)
            if (targetPlayer == null) {
                player.sendMessage("${CC.RED}Player not found!")
                return@withHostService
            }
            
            if (hostService.getPlayerState(targetPlayer) == PlayerState.ALIVE) {
                player.sendMessage("${CC.RED}${targetPlayer.name} is already alive!")
                return@withHostService
            }
            
            hostService.setPlayerState(targetPlayer, PlayerState.ALIVE)
            player.sendMessage("${CC.GREEN}Revived ${CC.YELLOW}${targetPlayer.name}${CC.GREEN}!")
        }
    }

    @Subcommand("kill")
    @Description("Kill a living player")
    @CommandCompletion("@players")
    fun onKill(player: ScalaPlayer, target: String) {
        withHostService(player) { hostService ->
            if (!hostService.isHost(player.bukkit())) {
                player.sendMessage("${CC.RED}You are not the event host!")
                return@withHostService
            }
            
            val targetPlayer = Bukkit.getPlayer(target)
            if (targetPlayer == null) {
                player.sendMessage("${CC.RED}Player not found!")
                return@withHostService
            }
            
            if (hostService.isHost(targetPlayer)) {
                player.sendMessage("${CC.RED}You cannot kill the host!")
                return@withHostService
            }
            
            if (hostService.getPlayerState(targetPlayer) == PlayerState.DEAD) {
                player.sendMessage("${CC.RED}${targetPlayer.name} is already dead!")
                return@withHostService
            }
            
            hostService.setPlayerState(targetPlayer, PlayerState.DEAD)
            player.sendMessage("${CC.GREEN}Killed ${CC.YELLOW}${targetPlayer.name}${CC.GREEN}!")
        }
    }

    @Subcommand("reviveall")
    @Description("Revive all dead players")
    fun onReviveAll(player: ScalaPlayer) {
        withHostService(player) { hostService ->
            if (!hostService.isHost(player.bukkit())) {
                player.sendMessage("${CC.RED}You are not the event host!")
                return@withHostService
            }
            
            val deadPlayers = hostService.getDeadPlayers()
            if (deadPlayers.isEmpty()) {
                player.sendMessage("${CC.RED}No dead players to revive!")
                return@withHostService
            }
            
            deadPlayers.forEach { deadPlayer ->
                hostService.setPlayerState(deadPlayer, PlayerState.ALIVE)
            }
            
            player.sendMessage("${CC.GREEN}Revived ${CC.YELLOW}${deadPlayers.size}${CC.GREEN} players!")
        }
    }

    @Subcommand("killall")
    @Description("Kill all living players")
    fun onKillAll(player: ScalaPlayer) {
        withHostService(player) { hostService ->
            if (!hostService.isHost(player.bukkit())) {
                player.sendMessage("${CC.RED}You are not the event host!")
                return@withHostService
            }
            
            val alivePlayers = hostService.getAlivePlayers()
            if (alivePlayers.isEmpty()) {
                player.sendMessage("${CC.RED}No alive players to kill!")
                return@withHostService
            }
            
            alivePlayers.forEach { alivePlayer ->
                hostService.setPlayerState(alivePlayer, PlayerState.DEAD)
            }
            
            player.sendMessage("${CC.GREEN}Killed ${CC.YELLOW}${alivePlayers.size}${CC.GREEN} players!")
        }
    }

    @Subcommand("spectate")
    @Description("Set a player as spectator (can fly, invisible)")
    @CommandCompletion("@players")
    fun onSpectate(player: ScalaPlayer, targetPlayer: Player) {
        withHostService(player) { hostService ->
            if (!hostService.isHost(player.bukkit())) {
                player.sendMessage("${CC.RED}You are not the event host!")
                return@withHostService
            }
            
            if (hostService.isHost(targetPlayer)) {
                player.sendMessage("${CC.RED}You cannot make the host a spectator!")
                return@withHostService
            }
            
            hostService.setPlayerState(targetPlayer, PlayerState.SPECTATOR)
            player.sendMessage("${CC.GREEN}${targetPlayer.name} is now spectating!")
        }
    }

    @Subcommand("unspectate")
    @Description("Remove a player from spectator mode")
    @CommandCompletion("@players")
    fun onUnspectate(player: ScalaPlayer, targetPlayer: Player) {
        withHostService(player) { hostService ->
            if (!hostService.isHost(player.bukkit())) {
                player.sendMessage("${CC.RED}You are not the event host!")
                return@withHostService
            }
            
            if (hostService.getPlayerState(targetPlayer) != PlayerState.SPECTATOR) {
                player.sendMessage("${CC.RED}${targetPlayer.name} is not spectating!")
                return@withHostService
            }
            
            hostService.setPlayerState(targetPlayer, PlayerState.DEAD)
            player.sendMessage("${CC.GREEN}${targetPlayer.name} is no longer spectating!")
        }
    }

    @Subcommand("pvp")
    @Description("Toggle PVP on/off")
    fun onTogglePvp(player: ScalaPlayer) {
        withHostService(player) { hostService ->
            if (!hostService.isHost(player.bukkit())) {
                player.sendMessage("${CC.RED}You are not the event host!")
                return@withHostService
            }
            
            val enabled = hostService.togglePvp()
            val status = if (enabled) "${CC.GREEN}ENABLED" else "${CC.RED}DISABLED"
            player.sendMessage("${CC.YELLOW}PVP is now $status${CC.YELLOW}!")
        }
    }

    @Subcommand("build")
    @Description("Toggle building on/off")
    fun onToggleBuild(player: ScalaPlayer) {
        withHostService(player) { hostService ->
            if (!hostService.isHost(player.bukkit())) {
                player.sendMessage("${CC.RED}You are not the event host!")
                return@withHostService
            }
            
            val enabled = hostService.toggleBuild()
            val status = if (enabled) "${CC.GREEN}ENABLED" else "${CC.RED}DISABLED"
            player.sendMessage("${CC.YELLOW}Building is now $status${CC.YELLOW}!")
        }
    }

    @Subcommand("status")
    @Description("View current event status")
    fun onStatus(player: ScalaPlayer) {
        withHostService(player) { hostService ->
            if (!hostService.isHost(player.bukkit())) {
                player.sendMessage("${CC.RED}You are not the event host!")
                return@withHostService
            }
            
            val alivePlayers = hostService.getAlivePlayers()
            val deadPlayers = hostService.getDeadPlayers()
            val spectatorPlayers = hostService.getSpectatorPlayers()
            
            player.sendMessage("")
            player.sendMessage("${CC.GOLD}${CC.BOLD}EVENT STATUS")
            player.sendMessage("${CC.GRAY}${CC.STRIKE_THROUGH}${"-".repeat(25)}")
            player.sendMessage("${CC.YELLOW}Event: ${CC.WHITE}${hostService.getEventName()}")
            player.sendMessage("${CC.YELLOW}Host: ${CC.WHITE}${player.bukkit().name}")
            player.sendMessage("${CC.YELLOW}Alive Players: ${CC.GREEN}${alivePlayers.size}")
            player.sendMessage("${CC.YELLOW}Dead Players: ${CC.RED}${deadPlayers.size}")
            player.sendMessage("${CC.YELLOW}Spectators: ${CC.AQUA}${spectatorPlayers.size}")
            player.sendMessage("${CC.YELLOW}PVP: ${if (hostService.isPvpEnabled()) "${CC.GREEN}ON" else "${CC.RED}OFF"}")
            player.sendMessage("${CC.YELLOW}Building: ${if (hostService.isBuildEnabled()) "${CC.GREEN}ON" else "${CC.RED}OFF"}")
            player.sendMessage("")
            
            if (alivePlayers.isNotEmpty()) {
                player.sendMessage("${CC.GREEN}Alive: ${CC.WHITE}${alivePlayers.joinToString(", ") { it.name }}")
            }
            if (deadPlayers.isNotEmpty()) {
                player.sendMessage("${CC.RED}Dead: ${CC.WHITE}${deadPlayers.joinToString(", ") { it.name }}")
            }
            if (spectatorPlayers.isNotEmpty()) {
                player.sendMessage("${CC.AQUA}Spectating: ${CC.WHITE}${spectatorPlayers.joinToString(", ") { it.name }}")
            }
            player.sendMessage("")
        }
    }

    @Subcommand("setup")
    @Description("Enter setup mode to configure event")
    fun onSetup(player: ScalaPlayer) {
        withHostService(player) { hostService ->
            if (!hostService.isHost(player.bukkit())) {
                player.sendMessage("${CC.RED}You are not the event host!")
                return@withHostService
            }
            
            val enabled = hostService.toggleSetupMode()
            if (enabled) {
                player.sendMessage("${CC.GREEN}Setup mode enabled!")
            }
        }
    }

    @Subcommand("setup help")
    @Description("View setup commands")
    fun onSetupHelp(player: ScalaPlayer) {
        withHostService(player) { hostService ->
            if (!hostService.isHost(player.bukkit())) {
                player.sendMessage("${CC.RED}You are not the event host!")
                return@withHostService
            }
            
            player.sendMessage("")
            player.sendMessage("${CC.AQUA}${CC.BOLD}SETUP COMMANDS")
            player.sendMessage("${CC.GRAY}${CC.STRIKE_THROUGH}${"-".repeat(25)}")
            player.sendMessage("${CC.YELLOW}/host setup config${CC.GRAY} - View current config")
            player.sendMessage("${CC.YELLOW}/host setup maxplayers <num>${CC.GRAY} - Set max players")
            player.sendMessage("${CC.YELLOW}/host setup minplayers <num>${CC.GRAY} - Set min players")
            player.sendMessage("${CC.YELLOW}/host setup description <text>${CC.GRAY} - Set description")
            player.sendMessage("${CC.YELLOW}/host setup autostart${CC.GRAY} - Toggle auto-start")
            player.sendMessage("${CC.YELLOW}/host setup finish${CC.GRAY} - Exit setup mode")
            player.sendMessage("")
        }
    }

    @Subcommand("setup config")
    @Description("View current setup configuration")
    fun onSetupConfig(player: ScalaPlayer) {
        withHostService(player) { hostService ->
            if (!hostService.isHost(player.bukkit())) {
                player.sendMessage("${CC.RED}You are not the event host!")
                return@withHostService
            }
            
            val config = hostService.getSetupConfig()
            
            player.sendMessage("")
            player.sendMessage("${CC.AQUA}${CC.BOLD}EVENT CONFIGURATION")
            player.sendMessage("${CC.GRAY}${CC.STRIKE_THROUGH}${"-".repeat(25)}")
            player.sendMessage("${CC.YELLOW}Name: ${CC.WHITE}${config["eventName"]}")
            player.sendMessage("${CC.YELLOW}Description: ${CC.WHITE}${config["description"]}")
            player.sendMessage("${CC.YELLOW}Max Players: ${CC.WHITE}${config["maxPlayers"]}")
            player.sendMessage("${CC.YELLOW}Min Players: ${CC.WHITE}${config["minPlayers"]}")
            player.sendMessage("${CC.YELLOW}Auto-Start: ${if (config["autoStart"] as Boolean) "${CC.GREEN}ON" else "${CC.RED}OFF"}")
            player.sendMessage("${CC.YELLOW}PVP: ${if (config["pvpEnabled"] as Boolean) "${CC.GREEN}ON" else "${CC.RED}OFF"}")
            player.sendMessage("${CC.YELLOW}Building: ${if (config["buildEnabled"] as Boolean) "${CC.GREEN}ON" else "${CC.RED}OFF"}")
            player.sendMessage("")
        }
    }

    @Subcommand("setup maxplayers")
    @Description("Set maximum players for the event")
    @CommandCompletion("30")
    fun onSetupMaxPlayers(player: ScalaPlayer, maxPlayers: Int) {
        withHostService(player) { hostService ->
            if (!hostService.isHost(player.bukkit())) {
                player.sendMessage("${CC.RED}You are not the event host!")
                return@withHostService
            }
            
            if (!hostService.isInSetupMode()) {
                player.sendMessage("${CC.RED}You must be in setup mode! Use ${CC.YELLOW}/host setup")
                return@withHostService
            }
            
            val success = hostService.setMaxPlayers(maxPlayers)
            if (success) {
                player.sendMessage("${CC.GREEN}Max players set to: ${CC.YELLOW}$maxPlayers")
            } else {
                player.sendMessage("${CC.RED}Invalid max players! Must be 1-100 and >= min players.")
            }
        }
    }

    @Subcommand("setup minplayers")
    @Description("Set minimum players for the event")
    @CommandCompletion("2")
    fun onSetupMinPlayers(player: ScalaPlayer, minPlayers: Int) {
        withHostService(player) { hostService ->
            if (!hostService.isHost(player.bukkit())) {
                player.sendMessage("${CC.RED}You are not the event host!")
                return@withHostService
            }
            
            if (!hostService.isInSetupMode()) {
                player.sendMessage("${CC.RED}You must be in setup mode! Use ${CC.YELLOW}/host setup")
                return@withHostService
            }
            
            val success = hostService.setMinPlayers(minPlayers)
            if (success) {
                player.sendMessage("${CC.GREEN}Min players set to: ${CC.YELLOW}$minPlayers")
            } else {
                player.sendMessage("${CC.RED}Invalid min players! Must be >= 1 and <= max players.")
            }
        }
    }

    @Subcommand("setup description")
    @Description("Set event description")
    @CommandCompletion("An amazing custom event!")
    fun onSetupDescription(player: ScalaPlayer, description: String) {
        withHostService(player) { hostService ->
            if (!hostService.isHost(player.bukkit())) {
                player.sendMessage("${CC.RED}You are not the event host!")
                return@withHostService
            }
            
            if (!hostService.isInSetupMode()) {
                player.sendMessage("${CC.RED}You must be in setup mode! Use ${CC.YELLOW}/host setup")
                return@withHostService
            }
            
            hostService.setEventDescription(description)
            player.sendMessage("${CC.GREEN}Event description set to:")
            player.sendMessage("${CC.WHITE}$description")
        }
    }

    @Subcommand("setup autostart")
    @Description("Toggle auto-start when minimum players join")
    fun onSetupAutoStart(player: ScalaPlayer) {
        withHostService(player) { hostService ->
            if (!hostService.isHost(player.bukkit())) {
                player.sendMessage("${CC.RED}You are not the event host!")
                return@withHostService
            }
            
            if (!hostService.isInSetupMode()) {
                player.sendMessage("${CC.RED}You must be in setup mode! Use ${CC.YELLOW}/host setup")
                return@withHostService
            }
            
            val enabled = hostService.toggleAutoStart()
            val status = if (enabled) "${CC.GREEN}ENABLED" else "${CC.RED}DISABLED"
            player.sendMessage("${CC.YELLOW}Auto-start is now $status${CC.YELLOW}!")
            
            if (enabled) {
                player.sendMessage("${CC.GRAY}Event will start when ${CC.YELLOW}${hostService.getMinPlayers()}${CC.GRAY} players join.")
            }
        }
    }

    @Subcommand("setup finish")
    @Description("Exit setup mode and prepare event")
    fun onSetupFinish(player: ScalaPlayer) {
        withHostService(player) { hostService ->
            if (!hostService.isHost(player.bukkit())) {
                player.sendMessage("${CC.RED}You are not the event host!")
                return@withHostService
            }
            
            if (!hostService.isInSetupMode()) {
                player.sendMessage("${CC.RED}You are not in setup mode!")
                return@withHostService
            }
            
            hostService.toggleSetupMode()
            player.sendMessage("${CC.GREEN}Setup complete! Your event is ready to begin.")
            
            // Announce the configured event
            val config = hostService.getSetupConfig()
            hostService.announceEvent("${config["description"]} | ${config["minPlayers"]}-${config["maxPlayers"]} players")
        }
    }

    @Subcommand("panel")
    @Description("Open the management GUI")
    fun onPanel(player: ScalaPlayer) {
        withHostService(player) { hostService ->
            if (!hostService.isHost(player.bukkit())) {
                player.sendMessage("${CC.RED}You are not the event host!")
                return@withHostService
            }
            
            ManagementMenu().openMenu(player.bukkit())
        }
    }

    @Subcommand("start")
    @Description("Start the event")
    fun onStart(player: ScalaPlayer) {
        withHostService(player) { hostService ->
            if (!hostService.isHost(player.bukkit())) {
                player.sendMessage("${CC.RED}You are not the event host!")
                return@withHostService
            }
            
            hostService.startEvent()
            player.sendMessage("${CC.GREEN}Event started!")
        }
    }

    @Subcommand("end")
    @Description("End the event")
    fun onEnd(player: ScalaPlayer) {
        withHostService(player) { hostService ->
            if (!hostService.isHost(player.bukkit())) {
                player.sendMessage("${CC.RED}You are not the event host!")
                return@withHostService
            }
            
            hostService.endEvent()
            player.sendMessage("${CC.GREEN}Event ended!")
        }
    }

    @Subcommand("tpalive")
    @Description("Teleport all alive players to your location")
    fun onTpAlive(player: ScalaPlayer) {
        withHostService(player) { hostService ->
            if (!hostService.isHost(player.bukkit())) {
                player.sendMessage("${CC.RED}You are not the event host!")
                return@withHostService
            }
            
            val alivePlayers = hostService.getAlivePlayers()
            if (alivePlayers.isEmpty()) {
                player.sendMessage("${CC.RED}No alive players to teleport!")
                return@withHostService
            }
            
            alivePlayers.forEach { alivePlayer ->
                alivePlayer.teleport(player.bukkit().location)
                alivePlayer.sendMessage("${CC.GREEN}You have been teleported to the host's location!")
            }
            
            player.sendMessage("${CC.GREEN}Teleported ${CC.YELLOW}${alivePlayers.size}${CC.GREEN} alive players to you!")
        }
    }

    @Subcommand("tpdead")
    @Description("Teleport all dead players to your location")
    fun onTpDead(player: ScalaPlayer) {
        withHostService(player) { hostService ->
            if (!hostService.isHost(player.bukkit())) {
                player.sendMessage("${CC.RED}You are not the event host!")
                return@withHostService
            }
            
            val deadPlayers = hostService.getDeadPlayers()
            if (deadPlayers.isEmpty()) {
                player.sendMessage("${CC.RED}No dead players to teleport!")
                return@withHostService
            }
            
            deadPlayers.forEach { deadPlayer ->
                deadPlayer.teleport(player.bukkit().location)
                deadPlayer.sendMessage("${CC.GREEN}You have been teleported to the host's location!")
            }
            
            player.sendMessage("${CC.GREEN}Teleported ${CC.YELLOW}${deadPlayers.size}${CC.GREEN} dead players to you!")
        }
    }

    @Subcommand("players")
    @Description("List all players with their status")
    fun onPlayers(player: ScalaPlayer) {
        withHostService(player) { hostService ->
            if (!hostService.isHost(player.bukkit())) {
                player.sendMessage("${CC.RED}You are not the event host!")
                return@withHostService
            }
            
            val alivePlayers = hostService.getAlivePlayers()
            val deadPlayers = hostService.getDeadPlayers()
            val totalPlayers = alivePlayers.size + deadPlayers.size
            
            player.sendMessage("")
            player.sendMessage("${CC.GOLD}${CC.BOLD}PLAYERS (${totalPlayers})")
            player.sendMessage("${CC.GRAY}${CC.STRIKE_THROUGH}${"-".repeat(25)}")
            
            if (alivePlayers.isNotEmpty()) {
                val aliveList = alivePlayers.joinToString("${CC.WHITE}, ${CC.GREEN}") { it.name }
                player.sendMessage("${CC.GREEN}Alive (${alivePlayers.size}): ${CC.GREEN}$aliveList")
            }
            
            if (deadPlayers.isNotEmpty()) {
                val deadList = deadPlayers.joinToString("${CC.WHITE}, ${CC.RED}") { it.name }
                player.sendMessage("${CC.RED}Dead (${deadPlayers.size}): ${CC.RED}$deadList")
            }
            
            player.sendMessage("")
        }
    }

    @Subcommand("announce")
    @Description("Announce your event across the network")
    @CommandCompletion("Come join my epic event!")
    fun onAnnounce(player: ScalaPlayer, @Optional message: String?) {
        withHostService(player) { hostService ->
            if (!hostService.isHost(player.bukkit())) {
                player.sendMessage("${CC.RED}You are not the event host!")
                return@withHostService
            }
            
            try {
                // Call the standalone announce command
                HostAnnounceCommand.onHostAnnounce(player.bukkit(), message)
            } catch (e: Exception) {
                player.sendMessage("${CC.RED}Error: ${e.message}")
            }
        }
    }
} 