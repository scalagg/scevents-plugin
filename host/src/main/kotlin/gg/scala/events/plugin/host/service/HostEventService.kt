package gg.scala.events.plugin.host.service

import gg.scala.aware.message.AwareMessage
import gg.scala.aware.thread.AwareThreadContext
import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.events.plugin.host.model.HostEventState
import gg.scala.events.plugin.host.model.PlayerState
import gg.scala.events.shared.ScalaEventsChannel
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service

import me.lucko.helper.Events
import me.lucko.helper.utils.Players
import net.evilblock.cubed.nametag.NametagHandler
import net.evilblock.cubed.nametag.NametagInfo
import net.evilblock.cubed.nametag.NametagProvider
import net.evilblock.cubed.serializers.Serializers
import net.evilblock.cubed.util.CC
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*

/**
 * Core service for managing host-controlled events
 */
@Service
class HostEventService {
    
    companion object {
        var instance: HostEventService? = null
    }
    
    @Inject
    lateinit var plugin: ExtendedScalaPlugin
    private var currentHost: UUID? = null
    private var eventName: String = "Custom Event"
    private var pvpEnabled: Boolean = false
    private var buildEnabled: Boolean = false
    private var setupMode: Boolean = false
    private val playerStates = mutableMapOf<UUID, PlayerState>()
    
    private val eventState = HostEventState()
    
    // Setup mode configuration
    private var maxPlayers: Int = 30
    private var minPlayers: Int = 2
    private var eventDescription: String = "A custom hosted event"
    private var autoStartEnabled: Boolean = false

    /**
     * Custom nametag provider for host events
     */
    private inner class HostEventNametagProvider : NametagProvider("Host Event Provider", 100) {
        override fun fetchNametag(toRefresh: Player, refreshFor: Player): NametagInfo? {
            val state = getPlayerState(toRefresh)
            return when (state) {
                PlayerState.ALIVE -> NametagProvider.createNametag("${CC.GREEN}", "", "1")
                PlayerState.DEAD -> NametagProvider.createNametag("${CC.RED}", "", "2")  
                PlayerState.HOST -> NametagProvider.createNametag("${CC.GOLD}[HOST] ", "", "0")
            }
        }
    }

    @Configure
    fun configure() {
        // Set instance reference for static access
        instance = this
        plugin.logger.info("[HostEventService] Service configured and static instance set!")
        
        setupEventListeners()
        
        // Register our custom nametag provider
        try {
            NametagHandler.registerProvider(HostEventNametagProvider())
        } catch (e: Exception) {
            plugin.logger.warning("Failed to register nametag provider: ${e.message}")
        }
        
        // Initialize all online players as dead
        Players.all().forEach { player ->
            setPlayerState(player, PlayerState.DEAD)
        }
    }

    private fun setupEventListeners() {
        // Player join - set as dead by default
        Events.subscribe(PlayerJoinEvent::class.java)
            .handler { event ->
                val player = event.player
                setPlayerState(player, PlayerState.DEAD)
                
                player.gameMode = GameMode.ADVENTURE
                player.sendMessage("")
                player.sendMessage("${CC.GOLD}Welcome to ${CC.YELLOW}$eventName${CC.GOLD}!")
                player.sendMessage("${CC.GRAY}You are currently ${CC.RED}DEAD${CC.GRAY}.")
                
                currentHost?.let { hostUuid ->
                    val hostPlayer = Bukkit.getPlayer(hostUuid)
                    val hostName = hostPlayer?.name ?: "Unknown"
                    player.sendMessage("${CC.GRAY}Host: ${CC.GREEN}$hostName")
                    
                    hostPlayer?.sendMessage("${CC.YELLOW}${player.name} ${CC.GRAY}joined and needs to be revived! Use ${CC.GREEN}/host revive ${player.name}")
                }
                player.sendMessage("")
                
                updateNametagsForAll()
            }

        // Player quit - cleanup
        Events.subscribe(PlayerQuitEvent::class.java)
            .handler { event ->
                playerStates.remove(event.player.uniqueId)
            }

        // PVP Control
        Events.subscribe(EntityDamageByEntityEvent::class.java)
            .handler { event ->
                if (!pvpEnabled) {
                    event.isCancelled = true
                    return@handler
                }
                
                // Only allow PVP between alive players
                val damager = event.damager as? Player ?: return@handler
                val damaged = event.entity as? Player ?: return@handler
                
                if (getPlayerState(damager) != PlayerState.ALIVE || 
                    getPlayerState(damaged) != PlayerState.ALIVE) {
                    event.isCancelled = true
                }
            }

        // Build Control
        Events.subscribe(BlockBreakEvent::class.java)
            .handler { event ->
                if (!buildEnabled && !isHost(event.player)) {
                    event.isCancelled = true
                }
            }

        Events.subscribe(BlockPlaceEvent::class.java)
            .handler { event ->
                if (!buildEnabled && !isHost(event.player)) {
                    event.isCancelled = true
                }
            }
    }

    /**
     * Set a player as the event host
     */
    fun setHost(player: Player): Boolean {
        if (currentHost != null) {
            return false // Already has a host
        }
        
        currentHost = player.uniqueId
        setPlayerState(player, PlayerState.HOST)
        
        player.sendMessage("${CC.GREEN}You are now hosting ${CC.YELLOW}$eventName${CC.GREEN}!")
        player.sendMessage("${CC.GRAY}Use ${CC.YELLOW}/host help${CC.GRAY} for commands.")
        
        // Announce to all players
        Players.all().forEach { p ->
            if (p != player) {
                p.sendMessage("${CC.YELLOW}${player.name}${CC.GRAY} is now hosting the event!")
            }
        }
        
        updateEventStatus()
        return true
    }

    /**
     * Remove the current host
     */
    fun removeHost() {
        currentHost?.let { hostUuid ->
            val hostPlayer = Bukkit.getPlayer(hostUuid)
            if (hostPlayer != null) {
                setPlayerState(hostPlayer, PlayerState.DEAD)
                hostPlayer.sendMessage("${CC.RED}You are no longer the host.")
            }
        }
        currentHost = null
        updateEventStatus()
    }

    /**
     * Set player state (alive/dead/host)
     */
    fun setPlayerState(player: Player, state: PlayerState) {
        playerStates[player.uniqueId] = state
        
        when (state) {
            PlayerState.ALIVE -> {
                player.gameMode = GameMode.SURVIVAL
                player.sendMessage("${CC.GREEN}You have been revived! You are now ${CC.GREEN}ALIVE${CC.GREEN}!")
            }
            PlayerState.DEAD -> {
                player.gameMode = GameMode.ADVENTURE
                player.sendMessage("${CC.RED}You are now ${CC.D_RED}DEAD${CC.RED}!")
            }
            PlayerState.HOST -> {
                player.gameMode = GameMode.CREATIVE
            }
        }
        
        updateNametagsForAll()
        
        // Check for auto-start if player was revived
        if (state == PlayerState.ALIVE) {
            checkAutoStart()
        }
    }

    /**
     * Get player state
     */
    fun getPlayerState(player: Player): PlayerState {
        return playerStates[player.uniqueId] ?: PlayerState.DEAD
    }

    /**
     * Check if player is the host
     */
    fun isHost(player: Player): Boolean {
        return currentHost == player.uniqueId
    }

    /**
     * Toggle PVP on/off
     */
    fun togglePvp(): Boolean {
        pvpEnabled = !pvpEnabled
        
        val status = if (pvpEnabled) "${CC.GREEN}ENABLED" else "${CC.RED}DISABLED"
        Players.all().forEach { player ->
            player.sendMessage("${CC.YELLOW}PVP has been $status${CC.YELLOW}!")
        }
        
        return pvpEnabled
    }

    /**
     * Toggle building on/off
     */
    fun toggleBuild(): Boolean {
        buildEnabled = !buildEnabled
        
        val status = if (buildEnabled) "${CC.GREEN}ENABLED" else "${CC.RED}DISABLED"
        Players.all().forEach { player ->
            player.sendMessage("${CC.YELLOW}Building has been $status${CC.YELLOW}!")
        }
        
        return buildEnabled
    }

    /**
     * Set custom event name
     */
    fun setEventName(name: String) {
        eventName = name
        
        Players.all().forEach { player ->
            player.sendMessage("${CC.GOLD}Event name changed to: ${CC.YELLOW}$name")
        }
        
        updateEventStatus()
    }

    /**
     * Announce the event across the network
     */
    fun announceEvent(message: String) {
        currentHost?.let { hostUuid ->
            val hostName = Bukkit.getPlayer(hostUuid)?.name ?: "Unknown"
            
            try {
                val awareMessage = AwareMessage.of(
                    "event-alert",
                    ScalaEventsChannel.channel(),
                    "player" to hostName,
                    "event" to eventName,
                    "message" to message,
                    "server" to "event-host"
                )
                awareMessage.publish(AwareThreadContext.ASYNC)
            } catch (e: Exception) {
                plugin.logger.warning("Failed to publish event announcement: ${e.message}")
            }
        }
    }

    /**
     * Get alive players
     */
    fun getAlivePlayers(): List<Player> {
        return Players.all().filter { getPlayerState(it) == PlayerState.ALIVE }
    }

    /**
     * Get dead players  
     */
    fun getDeadPlayers(): List<Player> {
        return Players.all().filter { getPlayerState(it) == PlayerState.DEAD }
    }

    /**
     * Update nametags for all players based on their state
     */
    private fun updateNametagsForAll() {
        try {
            Players.all().forEach { player ->
                NametagHandler.reloadPlayer(player)
            }
        } catch (e: Exception) {
            // Nametag system might not be available, continue without it
            plugin.logger.warning("Nametag system unavailable: ${e.message}")
        }
    }

    /**
     * Update event status in Redis
     */
    private fun updateEventStatus() {
        val status = mapOf(
            "eventName" to eventName,
            "host" to (currentHost?.toString() ?: "none"),
            "alivePlayers" to getAlivePlayers().size,
            "totalPlayers" to Players.all().size,
            "pvpEnabled" to pvpEnabled,
            "buildEnabled" to buildEnabled
        )
        
        ScalaEventsChannel.kv().sync().set(
            "events:host:status", 
            Serializers.gson.toJson(status)
        )
    }

    /**
     * Force set a player as host (staff override)
     */
    fun forceSetHost(player: Player): Boolean {
        currentHost?.let { removeHost() } // Remove existing host
        currentHost = player.uniqueId
        setPlayerState(player, PlayerState.HOST)
        
        player.sendMessage("${CC.GREEN}You have been made the event host by staff!")
        updateEventStatus()
        return true
    }

    /**
     * Check if a host is being abusive (killing/reviving repeatedly)
     *
     * i wanna figure something out
     */
//    fun isHostAbusive(): Boolean {
//    }

    /**
     * Get event statistics for staff monitoring
     */
    fun getEventStats(): Map<String, Any> {
        return mapOf(
            "eventName" to eventName,
            "hostUuid" to (currentHost?.toString() ?: "none"),
            "hostName" to (currentHost?.let { Bukkit.getPlayer(it)?.name } ?: "None"),
            "totalPlayers" to Players.all().size,
            "alivePlayers" to getAlivePlayers().size,
            "deadPlayers" to getDeadPlayers().size,
            "pvpEnabled" to pvpEnabled,
            "buildEnabled" to buildEnabled,
            "timestamp" to System.currentTimeMillis()
        )
    }

    /**
     * Staff override for event announcements
     */
    fun staffAnnounceEvent(staffName: String, message: String) {
        try {
            val awareMessage = AwareMessage.of(
                "event-alert",
                ScalaEventsChannel.channel(),
                "player" to staffName,
                "event" to eventName,
                "message" to "${CC.RED}[STAFF] ${CC.RESET}$message",
                "server" to "event-host"
            )
            awareMessage.publish(AwareThreadContext.ASYNC)
        } catch (e: Exception) {
            plugin.logger.warning("Failed to publish staff announcement: ${e.message}")
        }
    }

    /**
     * Reset event to default state (staff function)
     */
    fun resetEvent() {
        currentHost = null
        eventName = "Custom Event"
        pvpEnabled = false
        buildEnabled = false
        setupMode = false
        maxPlayers = 30
        minPlayers = 2
        eventDescription = "A custom hosted event"
        autoStartEnabled = false
        playerStates.clear()
        
        // Set all online players to dead
        Players.all().forEach { player ->
            setPlayerState(player, PlayerState.DEAD)
        }
        
        updateEventStatus()
    }

    /**
     * Toggle setup mode for configuring event parameters
     */
    fun toggleSetupMode(): Boolean {
        setupMode = !setupMode
        
        if (setupMode) {
            Players.all().forEach { player ->
                if (isHost(player)) {
                    player.sendMessage("")
                    player.sendMessage("${CC.AQUA}${CC.BOLD}SETUP MODE ENABLED")
                    player.sendMessage("${CC.GRAY}Configure your event parameters:")
                    player.sendMessage("${CC.YELLOW}/host setup help${CC.GRAY} - View setup commands")
                    player.sendMessage("${CC.GRAY}Use ${CC.YELLOW}/host setup finish${CC.GRAY} when ready!")
                    player.sendMessage("")
                } else {
                    player.sendMessage("${CC.AQUA}The host is setting up the event...")
                }
            }
        } else {
            Players.all().forEach { player ->
                if (isHost(player)) {
                    player.sendMessage("${CC.GREEN}Setup mode disabled! Event is ready to start.")
                } else {
                    player.sendMessage("${CC.GREEN}Event setup complete! Ready to begin.")
                }
            }
        }
        
        return setupMode
    }

    /**
     * Set maximum players for the event
     */
    fun setMaxPlayers(max: Int): Boolean {
        if (max < 1 || max > 100) return false
        if (max < minPlayers) return false
        
        maxPlayers = max
        updateEventStatus()
        return true
    }

    /**
     * Set minimum players for the event
     */
    fun setMinPlayers(min: Int): Boolean {
        if (min < 1) return false
        if (min > maxPlayers) return false
        
        minPlayers = min
        updateEventStatus()
        return true
    }

    /**
     * Set event description
     */
    fun setEventDescription(description: String) {
        eventDescription = description
        updateEventStatus()
    }

    /**
     * Toggle auto-start when minimum players are reached
     */
    fun toggleAutoStart(): Boolean {
        autoStartEnabled = !autoStartEnabled
        updateEventStatus()
        return autoStartEnabled
    }

    /**
     * Get setup mode configuration summary
     */
    fun getSetupConfig(): Map<String, Any> {
        return mapOf(
            "eventName" to eventName,
            "description" to eventDescription,
            "maxPlayers" to maxPlayers,
            "minPlayers" to minPlayers,
            "autoStart" to autoStartEnabled,
            "pvpEnabled" to pvpEnabled,
            "buildEnabled" to buildEnabled,
            "setupMode" to setupMode
        )
    }

    /**
     * Check if enough players to start (if auto-start is enabled)
     */
    fun checkAutoStart() {
        if (!autoStartEnabled || !setupMode) return
        
        val alivePlayers = getAlivePlayers().size
        if (alivePlayers >= minPlayers) {
            setupMode = false
            Players.all().forEach { player ->
                player.sendMessage("${CC.GREEN}${CC.BOLD}AUTO-START!")
                player.sendMessage("${CC.GRAY}Minimum players reached (${alivePlayers}/${minPlayers})")
            }
        }
    }

    /**
     * Start the event - set all players to alive and announce
     */
    fun startEvent() {
        Players.all().forEach { player ->
            if (!isHost(player)) {
                setPlayerState(player, PlayerState.ALIVE)
                player.health = 20.0
                player.foodLevel = 20
            }
        }
        
        currentHost?.let { hostUuid ->
            val hostName = Bukkit.getPlayer(hostUuid)?.name ?: "Unknown"
            
            Players.all().forEach { player ->
                player.sendMessage("")
                player.sendMessage("${CC.GOLD}${CC.BOLD}EVENT STARTED!")
                player.sendMessage("${CC.GRAY}Event: ${CC.YELLOW}$eventName")
                player.sendMessage("${CC.GRAY}Host: ${CC.GREEN}$hostName")
                player.sendMessage("")
            }
            
            // Network announcement
            announceEvent("The event has started!")
        }
        
        setupMode = false
        updateEventStatus()
    }

    /**
     * End the event - reset all states
     */
    fun endEvent() {
        currentHost?.let { hostUuid ->
            val hostName = Bukkit.getPlayer(hostUuid)?.name ?: "Unknown"
            
            Players.all().forEach { player ->
                player.sendMessage("")
                player.sendMessage("${CC.RED}${CC.BOLD}EVENT ENDED!")
                player.sendMessage("${CC.GRAY}Thanks for playing ${CC.YELLOW}$eventName${CC.GRAY}!")
                player.sendMessage("${CC.GRAY}Hosted by: ${CC.GREEN}$hostName")
                player.sendMessage("")
                
                // Reset everyone to dead/lobby state
                if (!isHost(player)) {
                    setPlayerState(player, PlayerState.DEAD)
                }
            }
        }
        
        // Reset event state
        pvpEnabled = false
        buildEnabled = false
        setupMode = false
        updateEventStatus()
    }

    // Getters
    fun getCurrentHost() = currentHost
    fun getEventName() = eventName
    fun isPvpEnabled() = pvpEnabled
    fun isBuildEnabled() = buildEnabled
    fun isInSetupMode() = setupMode
    fun getMaxPlayers() = maxPlayers
    fun getMinPlayers() = minPlayers
    fun getEventDescription() = eventDescription
    fun isAutoStartEnabled() = autoStartEnabled
} 