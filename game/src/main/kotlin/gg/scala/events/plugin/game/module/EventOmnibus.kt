package gg.scala.events.plugin.game.module

import gg.scala.cgs.common.CgsGameEngine
import gg.scala.cgs.common.information.arena.CgsGameArenaHandler
import gg.scala.cgs.common.runnable.state.EndedStateRunnable
import gg.scala.cgs.common.states.CgsGameState
import gg.scala.events.plugin.game.ScalaEventsGameEngine
import gg.scala.events.shared.type.EventType
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.flavor.service.ignore.IgnoreAutoScan
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.utils.Players
import net.evilblock.cubed.util.CC
import org.bukkit.Bukkit
import java.util.concurrent.TimeUnit

/**
 * @author GrowlyX
 * @since 10/3/2022
 */
@Service
@IgnoreAutoScan
object EventOmnibus
{
    @Inject
    lateinit var module: EventModule

    @Inject
    lateinit var engine: ScalaEventsGameEngine

    val bindings =
        mutableMapOf<EventType, EventModule>()

    fun bind(type: EventType, module: EventModule)
    {
        this.bindings[type] = module
    }

    fun module() = kotlin
        .runCatching { module }
        .getOrNull()

    fun <T> moduleOr(
        `this`: EventModule.() -> T,
        that: () -> T
    ): T
    {
        return module()?.`this`() ?: that()
    }

    @Configure
    fun configure()
    {
        CgsGameArenaHandler.configure(module.type.mode)
        engine.gameArena = CgsGameArenaHandler.arena

        module.configure()
        engine.gameState = CgsGameState.STARTING

        Events
            .subscribe(CgsGameEngine.CgsGamePreStartCancelEvent::class.java)
            .handler {
                Bukkit.getOnlinePlayers()
                    .forEach { player ->
                        player.kickPlayer("${CC.RED}We were unable to start the event due to a lack of players.")
                    }

                EndedStateRunnable.ALLOWED_TO_JOIN = false

                Schedulers.sync()
                    .runLater({
                        Bukkit.shutdown()
                    }, 100L)
            }
    }
}
