package gg.scala.events.plugin.game.module.bonkers.teams.timers

import gg.scala.cgs.common.CgsGameEngine
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.lemon.cooldown.type.PlayerStaticCooldown
import me.lucko.helper.Events
import net.evilblock.cubed.util.CC
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import java.util.concurrent.TimeUnit

@Service
object CombatTimer : PlayerStaticCooldown(
    "Combat", TimeUnit.SECONDS.toMillis(30L)
) {

    @Configure
    fun configure()
    {
        notifyOnExpiration()
        whenExpired {
            it.sendMessage("${CC.RED}Your Combat Tag has expired.")
        }

        Events.subscribe(EntityDamageByEntityEvent::class.java)
            .filter {
                it.entity is Player && it.damager is Player
            }
            .handler {
                val damaged = it.entity as Player
                val damager = it.damager as Player

                if (!isActive(damaged))
                {
                    damaged.sendMessage("${CC.RED}You have been put on Combat Tag for 30 seconds.")
                } else
                {
                    this.reset(damaged)
                }

                if (!isActive(damager))
                {
                    damager.sendMessage("${CC.RED}You have been put on Combat Tag for 30 seconds.")
                } else
                {
                    this.reset(damager)
                }

                addOrOverride(damaged)
                addOrOverride(damager)
            }
            .bindWith(CgsGameEngine.INSTANCE.plugin)
    }
}