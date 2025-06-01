package gg.scala.events.plugin.host.model

/**
 * Represents the different states a player can be in during a host-controlled event
 */
enum class PlayerState {
    /**
     * Player is alive and can participate in the event
     */
    ALIVE,
    
    /**
     * Player is dead and cannot participate until revived
     */
    DEAD,
    
    /**
     * Player is the event host with special privileges
     */
    HOST
} 