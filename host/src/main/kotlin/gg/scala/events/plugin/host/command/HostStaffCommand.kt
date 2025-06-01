package gg.scala.events.plugin.host.command

import gg.scala.commons.acf.annotation.*
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.events.plugin.host.service.HostEventService
import gg.scala.events.plugin.host.model.PlayerState
import gg.scala.flavor.inject.Inject

import me.lucko.helper.utils.Players
import net.evilblock.cubed.util.CC
import org.bukkit.Bukkit
import org.bukkit.GameMode
import gg.scala.commons.acf.CommandHelp

@AutoRegister
@CommandAlias("hoststaff|staffhost|hs")
@CommandPermission("scevents.staff")
object HostStaffCommand : ScalaCommand() {
    
    @Inject
    lateinit var hostService: HostEventService

    @Default
    @HelpCommand
    fun onHelp(player: ScalaPlayer, help: CommandHelp) {
        help.showHelp()
    }

    @Subcommand("forcehost")
    @Description("Force a player to become the event host")
    @CommandCompletion("@players")
    fun onForceHost(player: ScalaPlayer, @Optional target: String?) {
        if (target == null) {
            // Make the staff member the host
            hostService.forceSetHost(player.bukkit())
            player.sendMessage("${CC.GREEN}You are now forcefully hosting the event!")
            Players.all().forEach { p ->
                if (p != player.bukkit()) {
                    p.sendMessage("${CC.RED}${CC.BOLD}STAFF: ${CC.YELLOW}${player.bukkit().name}${CC.GRAY} has taken control of the event!")
                }
            }
        } else {
            val targetPlayer = Bukkit.getPlayer(target)
            if (targetPlayer == null) {
                player.sendMessage("${CC.RED}Player not found!")
                return
            }
            
            hostService.forceSetHost(targetPlayer)
            player.sendMessage("${CC.GREEN}${targetPlayer.name} is now forcefully hosting the event!")
            Players.all().forEach { p ->
                p.sendMessage("${CC.RED}${CC.BOLD}STAFF: ${CC.YELLOW}${targetPlayer.name}${CC.GRAY} has been made the event host by staff!")
            }
        }
    }

    @Subcommand("removehost")
    @Description("Remove the current event host")
    fun onRemoveHost(player: ScalaPlayer) {
        val currentHost = hostService.getCurrentHost()
        if (currentHost == null) {
            player.sendMessage("${CC.RED}There is no current event host!")
            return
        }
        
        val hostName = Bukkit.getPlayer(currentHost)?.name ?: "Unknown"
        hostService.removeHost()
        
        player.sendMessage("${CC.GREEN}Removed ${CC.YELLOW}$hostName${CC.GREEN} as event host!")
        Players.all().forEach { p ->
            p.sendMessage("${CC.RED}${CC.BOLD}STAFF: ${CC.GRAY}Event host has been removed by staff!")
        }
    }

    @Subcommand("status")
    @Description("View current event status (staff override)")
    fun onStatus(player: ScalaPlayer) {
        val alivePlayers = hostService.getAlivePlayers()
        val deadPlayers = hostService.getDeadPlayers()
        val currentHost = hostService.getCurrentHost()
        val hostName = currentHost?.let { Bukkit.getPlayer(it)?.name } ?: "None"
        
        player.sendMessage("")
        player.sendMessage("${CC.RED}${CC.BOLD}STAFF EVENT STATUS")
        player.sendMessage("${CC.GRAY}${CC.STRIKE_THROUGH}${"-".repeat(30)}")
        player.sendMessage("${CC.YELLOW}Event: ${CC.WHITE}${hostService.getEventName()}")
        player.sendMessage("${CC.YELLOW}Host: ${CC.WHITE}$hostName")
        player.sendMessage("${CC.YELLOW}Total Players: ${CC.AQUA}${Players.all().size}")
        player.sendMessage("${CC.YELLOW}Alive Players: ${CC.GREEN}${alivePlayers.size}")
        player.sendMessage("${CC.YELLOW}Dead Players: ${CC.RED}${deadPlayers.size}")
        player.sendMessage("${CC.YELLOW}PVP: ${if (hostService.isPvpEnabled()) "${CC.GREEN}ON" else "${CC.RED}OFF"}")
        player.sendMessage("${CC.YELLOW}Building: ${if (hostService.isBuildEnabled()) "${CC.GREEN}ON" else "${CC.RED}OFF"}")
        player.sendMessage("")
        
        if (alivePlayers.isNotEmpty()) {
            player.sendMessage("${CC.GREEN}Alive: ${CC.WHITE}${alivePlayers.joinToString(", ") { it.name }}")
        }
        if (deadPlayers.isNotEmpty()) {
            player.sendMessage("${CC.RED}Dead: ${CC.WHITE}${deadPlayers.joinToString(", ") { it.name }}")
        }
        player.sendMessage("")
    }

    @Subcommand("reset")
    @Description("Reset the entire event")
    fun onReset(player: ScalaPlayer) {
        hostService.resetEvent()
        
        player.sendMessage("${CC.GREEN}Event has been completely reset!")
        Players.all().forEach { p ->
            if (p != player.bukkit()) {
                p.sendMessage("${CC.RED}${CC.BOLD}STAFF: ${CC.GRAY}The event has been reset by staff!")
            }
        }
    }

    @Subcommand("revive")
    @Description("Force revive a player")
    @CommandCompletion("@players")
    fun onRevive(player: ScalaPlayer, target: String) {
        val targetPlayer = Bukkit.getPlayer(target)
        if (targetPlayer == null) {
            player.sendMessage("${CC.RED}Player not found!")
            return
        }
        
        hostService.setPlayerState(targetPlayer, PlayerState.ALIVE)
        player.sendMessage("${CC.GREEN}Force revived ${CC.YELLOW}${targetPlayer.name}${CC.GREEN}!")
        targetPlayer.sendMessage("${CC.GREEN}You have been revived by staff!")
    }

    @Subcommand("kill")
    @Description("Force kill a player")
    @CommandCompletion("@players")
    fun onKill(player: ScalaPlayer, target: String) {
        val targetPlayer = Bukkit.getPlayer(target)
        if (targetPlayer == null) {
            player.sendMessage("${CC.RED}Player not found!")
            return
        }
        
        if (hostService.isHost(targetPlayer)) {
            player.sendMessage("${CC.YELLOW}Warning: Killing the current host!")
        }
        
        hostService.setPlayerState(targetPlayer, PlayerState.DEAD)
        player.sendMessage("${CC.GREEN}Force killed ${CC.YELLOW}${targetPlayer.name}${CC.GREEN}!")
        targetPlayer.sendMessage("${CC.RED}You have been killed by staff!")
    }

    @Subcommand("reviveall")
    @Description("Force revive all players")
    fun onReviveAll(player: ScalaPlayer) {
        val deadPlayers = hostService.getDeadPlayers()
        
        deadPlayers.forEach { deadPlayer ->
            hostService.setPlayerState(deadPlayer, PlayerState.ALIVE)
        }
        
        player.sendMessage("${CC.GREEN}Force revived ${CC.YELLOW}${deadPlayers.size}${CC.GREEN} players!")
        Players.all().forEach { p ->
            if (p != player.bukkit()) {
                p.sendMessage("${CC.GREEN}All players have been revived by staff!")
            }
        }
    }

    @Subcommand("killall")
    @Description("Force kill all players")
    fun onKillAll(player: ScalaPlayer) {
        val alivePlayers = hostService.getAlivePlayers()
        
        alivePlayers.forEach { alivePlayer ->
            hostService.setPlayerState(alivePlayer, PlayerState.DEAD)
        }
        
        player.sendMessage("${CC.GREEN}Force killed ${CC.YELLOW}${alivePlayers.size}${CC.GREEN} players!")
        Players.all().forEach { p ->
            if (p != player.bukkit()) {
                p.sendMessage("${CC.RED}All players have been killed by staff!")
            }
        }
    }

    @Subcommand("setname")
    @Description("Force set the event name")
    @CommandCompletion("StaffEvent")
    fun onSetName(player: ScalaPlayer, eventName: String) {
        hostService.setEventName(eventName)
        player.sendMessage("${CC.GREEN}Force set event name to: ${CC.YELLOW}$eventName")
        Players.all().forEach { p ->
            if (p != player.bukkit()) {
                p.sendMessage("${CC.GOLD}Event name changed to ${CC.YELLOW}$eventName${CC.GOLD} by staff!")
            }
        }
    }

    @Subcommand("forcepvp")
    @Description("Force toggle PVP")
    @CommandCompletion("on|off")
    fun onForcePvp(player: ScalaPlayer, @Optional setting: String?) {
        val enabled = when (setting?.lowercase()) {
            "on", "true", "enable" -> {
                if (!hostService.isPvpEnabled()) hostService.togglePvp()
                true
            }
            "off", "false", "disable" -> {
                if (hostService.isPvpEnabled()) hostService.togglePvp()
                false
            }
            else -> hostService.togglePvp()
        }
        
        val status = if (enabled) "${CC.GREEN}ENABLED" else "${CC.RED}DISABLED"
        player.sendMessage("${CC.GREEN}Force set PVP to $status${CC.GREEN}!")
        Players.all().forEach { p ->
            if (p != player.bukkit()) {
                p.sendMessage("${CC.RED}${CC.BOLD}STAFF: ${CC.YELLOW}PVP has been $status${CC.YELLOW} by staff!")
            }
        }
    }

    @Subcommand("forcebuild")
    @Description("Force toggle building")
    @CommandCompletion("on|off")
    fun onForceBuild(player: ScalaPlayer, @Optional setting: String?) {
        val enabled = when (setting?.lowercase()) {
            "on", "true", "enable" -> {
                if (!hostService.isBuildEnabled()) hostService.toggleBuild()
                true
            }
            "off", "false", "disable" -> {
                if (hostService.isBuildEnabled()) hostService.toggleBuild()
                false
            }
            else -> hostService.toggleBuild()
        }
        
        val status = if (enabled) "${CC.GREEN}ENABLED" else "${CC.RED}DISABLED"
        player.sendMessage("${CC.GREEN}Force set building to $status${CC.GREEN}!")
        Players.all().forEach { p ->
            if (p != player.bukkit()) {
                p.sendMessage("${CC.RED}${CC.BOLD}STAFF: ${CC.YELLOW}Building has been $status${CC.YELLOW} by staff!")
            }
        }
    }

    @Subcommand("announce")
    @Description("Force announce the event")
    @CommandCompletion("Staff is hosting an amazing event!")
    fun onAnnounce(player: ScalaPlayer, message: String) {
        hostService.staffAnnounceEvent(player.bukkit().name, message)
        player.sendMessage("${CC.GREEN}Force announced event to the network!")
        player.sendMessage("${CC.GRAY}Message: ${CC.WHITE}$message")
    }

    @Subcommand("spectate")
    @Description("Enter spectator mode for the event")
    fun onSpectate(player: ScalaPlayer) {
        val bukkitPlayer = player.bukkit()
        
        bukkitPlayer.gameMode = GameMode.SPECTATOR
        bukkitPlayer.sendMessage("${CC.AQUA}You are now in spectator mode!")
        bukkitPlayer.sendMessage("${CC.GRAY}Use ${CC.YELLOW}/hs status${CC.GRAY} to monitor the event.")
        
        // Don't add them to player states - they're in staff spectator mode
    }

    @Subcommand("tp")
    @Description("Teleport to a player in the event")
    @CommandCompletion("@players")
    fun onTeleport(player: ScalaPlayer, target: String) {
        val targetPlayer = Bukkit.getPlayer(target)
        if (targetPlayer == null) {
            player.sendMessage("${CC.RED}Player not found!")
            return
        }
        
        player.bukkit().teleport(targetPlayer)
        player.sendMessage("${CC.GREEN}Teleported to ${CC.YELLOW}${targetPlayer.name}${CC.GREEN}!")
    }

    @Subcommand("emergency")
    @Description("Emergency stop - kick everyone and reset")
    fun onEmergency(player: ScalaPlayer) {
        player.sendMessage("${CC.RED}${CC.BOLD}EMERGENCY STOP ACTIVATED!")
        
        // Kick all non-staff players
        Players.all().forEach { p ->
            if (!p.hasPermission("scevents.staff")) {
                p.kickPlayer("${CC.RED}Emergency event shutdown by staff.")
            }
        }
        
        // Reset everything
        hostService.removeHost()
        hostService.setEventName("Custom Event")
        
        player.sendMessage("${CC.GREEN}Emergency shutdown complete!")
    }

    @Subcommand("test scoreboard")
    @Description("Test the scoreboard system")
    fun onTestScoreboard(player: ScalaPlayer) {
        val bukkitPlayer = player.bukkit()
        
        bukkitPlayer.sendMessage("${CC.GREEN}Testing scoreboard system...")
        bukkitPlayer.sendMessage("${CC.GRAY}Service instance: ${HostEventService.instance != null}")
        
        if (HostEventService.instance != null) {
            val service = HostEventService.instance!!
            bukkitPlayer.sendMessage("${CC.GREEN}Service is available!")
            bukkitPlayer.sendMessage("${CC.GRAY}Event: ${service.getEventName()}")
            bukkitPlayer.sendMessage("${CC.GRAY}Player state: ${service.getPlayerState(bukkitPlayer)}")
            bukkitPlayer.sendMessage("${CC.GRAY}Total players: ${service.getAlivePlayers().size + service.getDeadPlayers().size}")
        } else {
            bukkitPlayer.sendMessage("${CC.RED}Service is not available!")
        }
    }

    @Subcommand("scoreboard")
    @Description("Toggle scoreboard for a player")
    @CommandCompletion("@players")
    fun onScoreboard(player: ScalaPlayer, @Optional target: String?) {
        val bukkitPlayer = if (target != null) {
            Bukkit.getPlayer(target) ?: run {
                player.sendMessage("${CC.RED}Player not found!")
                return
            }
        } else {
            player.bukkit()
        }
        
        try {
            // Try to create and show a manual scoreboard
            val scoreboard = Bukkit.getScoreboardManager().newScoreboard
            val objective = scoreboard.registerNewObjective("host", "dummy")
            objective.displaySlot = org.bukkit.scoreboard.DisplaySlot.SIDEBAR
            objective.displayName = "${CC.B_PRI}Scala Events ${CC.GRAY}(Manual)"
            
            val hostService = HostEventService.instance
            if (hostService != null) {
                val score15 = objective.getScore("${CC.WHITE}Event: ${CC.PRI}${hostService.getEventName()}")
                score15.score = 15
                
                val score14 = objective.getScore("${CC.WHITE}Host: ${CC.GREEN}Test")
                score14.score = 14
                
                val score13 = objective.getScore("")
                score13.score = 13
                
                val score12 = objective.getScore("${CC.WHITE}Status: ${CC.GREEN}Testing")
                score12.score = 12
            } else {
                val score15 = objective.getScore("${CC.RED}Service not available")
                score15.score = 15
            }
            
            bukkitPlayer.scoreboard = scoreboard
            player.sendMessage("${CC.GREEN}Manual scoreboard shown to ${bukkitPlayer.name}!")
            
        } catch (e: Exception) {
            player.sendMessage("${CC.RED}Error creating scoreboard: ${e.message}")
        }
    }
} 