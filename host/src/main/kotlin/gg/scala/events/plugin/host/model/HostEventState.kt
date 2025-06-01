package gg.scala.events.plugin.host.model

import java.util.*

/**
 * Represents the overall state of a host-controlled event
 */
data class HostEventState(
    var eventName: String = "Custom Event",
    var host: UUID? = null,
    var pvpEnabled: Boolean = false,
    var buildEnabled: Boolean = false,
    var eventActive: Boolean = false,
    var startTime: Long? = null,
    val playerStates: MutableMap<UUID, PlayerState> = mutableMapOf()
) {
    /**
     * Get the number of alive players
     */
    fun getAliveCount(): Int {
        return playerStates.values.count { it == PlayerState.ALIVE }
    }
    
    /**
     * Get the number of dead players
     */
    fun getDeadCount(): Int {
        return playerStates.values.count { it == PlayerState.DEAD }
    }
    
    /**
     * Get total player count (excluding host)
     */
    fun getTotalPlayerCount(): Int {
        return playerStates.values.count { it != PlayerState.HOST }
    }
    
    /**
     * Check if the event has a host
     */
    fun hasHost(): Boolean {
        return host != null
    }
    
    /**
     * Start the event
     */
    fun startEvent() {
        eventActive = true
        startTime = System.currentTimeMillis()
    }
    
    /**
     * End the event
     */
    fun endEvent() {
        eventActive = false
        startTime = null
    }
    
    /**
     * Get event duration in milliseconds
     */
    fun getEventDuration(): Long? {
        return startTime?.let { System.currentTimeMillis() - it }
    }
} 