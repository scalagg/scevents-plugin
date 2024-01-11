package gg.scala.events.plugin.game.module.bonkers.teams.timers

import gg.scala.cgs.common.CgsGameEngine
import gg.scala.cgs.common.states.CgsGameState
import gg.scala.events.plugin.game.ScalaEventsGamePlugin
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.lemon.cooldown.CooldownHandler
import gg.scala.lemon.cooldown.CooldownHandler.notifyAndContinueNoBypass
import gg.scala.lemon.cooldown.type.PlayerStaticCooldown
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import net.evilblock.cubed.util.CC
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.EnderPearl
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.PlayerInteractEvent
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.round

/**
 * @author GrowlyX
 * @since 11/21/2021
 */
@Service
object EnderPearlCooldown : PlayerStaticCooldown(
    "Enderpearl", 0L
)
{
    @Inject
    lateinit var plugin: ScalaEventsGamePlugin

    override fun durationFor(t: Player) = TimeUnit.SECONDS.toMillis(15.toLong())

    @Configure
    fun configure()
    {
        // We need this for the exp bar
        notifyOnExpiration()
        whenExpired {
            it.sendMessage("${CC.GRAY}Your enderpearl cooldown has expired.")
        }

        Schedulers
            .async()
            .runRepeating(Runnable {
                for (id in tasks.keys)
                {
                    val player = Bukkit.getPlayer(id)
                        ?: continue

                    val time = fetchRemaining(player)
                    if (time < 0)
                    {
                        continue
                    }

                    val seconds = round(time.toDouble() / 1000.0).toInt()

                    player.level = max(seconds, 0)
                    player.exp = (time.toFloat() / (15 * 1000.0f)).coerceIn(0.0f, 1.0f)
                }
            }, 0L, 1L)

        Events
            .subscribe(PlayerInteractEvent::class.java, EventPriority.HIGHEST)
            .filter {
                it.hasItem() &&
                    it.action.name.contains("RIGHT") &&
                    it.item.type == Material.ENDER_PEARL
            }
            .handler {
                val player = it.player

                val game = CgsGameEngine.INSTANCE

                if (game.gameState != CgsGameState.STARTED)
                {
                    it.isCancelled = true
                    player.updateInventory()

                    player.sendMessage("${CC.RED}You cannot throw pearls right now!")
                    return@handler
                }

                if (!notifyAndContinueNoBypass(this.javaClass, player, "throwing a pearl"))
                {
                    it.isCancelled = true
                    player.updateInventory()
                }
            }
            .bindWith(plugin)

        Events
            .subscribe(ProjectileLaunchEvent::class.java)
            .filter { it.entity is EnderPearl }
            .filter { it.entity.shooter is Player }
            .handler {
                val player = it.entity.shooter as Player

                if (!it.isCancelled)
                {
                    addOrOverride(player)
                }
            }
            .bindWith(plugin)

        CooldownHandler.register(this)
    }
}
