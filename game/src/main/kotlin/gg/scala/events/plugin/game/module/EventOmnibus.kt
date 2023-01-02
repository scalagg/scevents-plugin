package gg.scala.events.plugin.game.module

import gg.scala.aware.message.AwareMessage
import gg.scala.cgs.common.CgsGameEngine
import gg.scala.cgs.common.information.arena.CgsGameArenaHandler
import gg.scala.cgs.common.runnable.state.EndedStateRunnable
import gg.scala.cgs.common.runnable.state.StartingStateRunnable
import gg.scala.cgs.common.states.CgsGameState
import gg.scala.commons.agnostic.sync.ServerSync
import gg.scala.events.plugin.game.ScalaEventsGameEngine
import gg.scala.events.shared.ScalaEventsChannel
import gg.scala.events.shared.type.EventType
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.inject.condition.Named
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.flavor.service.ignore.IgnoreAutoScan
import gg.scala.lemon.util.QuickAccess
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.terminable.composite.CompositeTerminable
import me.lucko.helper.utils.Players
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import net.evilblock.cubed.util.time.TimeUtil
import org.bukkit.Bukkit
import org.bukkit.event.player.PlayerJoinEvent
import java.util.UUID
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
        engine.gameInfo = module.type.info

        CgsGameArenaHandler.configure(module.type.mode)
        engine.gameArena = CgsGameArenaHandler.arena

        module.configure()

        configureJoinEventHandlers()

        Events
            .subscribe(CgsGameEngine.CgsGamePreStartCancelEvent::class.java)
            .handler {
                EventOrchestrator.terminateActive()
            }
    }

    private var terminable: CompositeTerminable? = null

    fun configureJoinEventHandlers()
    {
        terminable = if (terminable == null)
        {
            CompositeTerminable.create()
        } else
        {
            terminable!!.closeAndReportException()
            CompositeTerminable.create()
        }

        var hasFirstPlayerJoined = false

        Events
            .subscribe(PlayerJoinEvent::class.java)
            .expireIf { hasFirstPlayerJoined }
            .handler {
                val message = AwareMessage
                    .of(
                        "event-alert",
                        ScalaEventsChannel.channel(),
                        "player" to QuickAccess.coloredName(it.player.uniqueId),
                        "event" to module.type.info.fancyNameRender,
                        "server" to ServerSync.local.id
                    )

                Schedulers
                    .sync()
                    .runRepeating({ task ->
                        if (
                            engine.gameState != CgsGameState.WAITING &&
                            engine.gameState != CgsGameState.STARTING
                        )
                        {
                            task.closeAndReportException()
                            return@runRepeating
                        }

                        message.publish()
                    }, 0L, 20L * 20L)

                hasFirstPlayerJoined = true
            }
    }
}
