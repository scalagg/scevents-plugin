package gg.scala.events.plugin.game.module.bonkers.teams.timers

import gg.scala.cgs.common.CgsGameEngine
import gg.scala.events.plugin.game.module.bonkers.data
import gg.scala.events.plugin.game.module.bonkers.team
import gg.scala.flavor.service.Configure
import gg.scala.lemon.cooldown.type.PlayerStaticCooldown
import me.lucko.helper.Events
import net.evilblock.cubed.util.CC
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import java.util.concurrent.TimeUnit

object TeamHomeTimer : PlayerStaticCooldown(
    "Team-Home", TimeUnit.SECONDS.toMillis(10L)
) {
    @Configure
    fun configure() {
        notifyOnExpiration()
        whenExpired {
            val teamData = it.team()!!.data()
            val location = teamData?.spawnLocation

            if (location == null)
            {
                it.sendMessage("${CC.RED}Your team does not have a spawn location set.")
                return@whenExpired
            }

            it.teleport(location)
            it.sendMessage("${CC.GREEN}You have been teleported to your team's spawn location.")
        }

        Events.subscribe(EntityDamageByEntityEvent::class.java)
            .filter {
                it.entity is Player
            }
            .handler { event ->
                val player = event.entity as Player

                if (this.isActive(player))
                {
                    this.reset(player)
                    player.sendMessage("${CC.RED}You took damage! Your team home timer has been reset.")
                }
            }
            .bindWith(CgsGameEngine.INSTANCE.plugin)

        Events.subscribe(EntityDamageEvent::class.java)
            .filter {
                it.entity is Player
            }
            .handler { event ->
                val player = event.entity as Player

                if (this.isActive(player))
                {
                    this.reset(player)
                    player.sendMessage("${CC.RED}You took damage! Your team home timer has been reset.")
                }
            }
            .bindWith(CgsGameEngine.INSTANCE.plugin)
    }
}