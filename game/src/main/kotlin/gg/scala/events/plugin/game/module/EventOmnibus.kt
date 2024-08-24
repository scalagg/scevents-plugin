package gg.scala.events.plugin.game.module

import gg.scala.cgs.common.CgsGameEngine
import gg.scala.cgs.common.information.arena.CgsGameArenaHandler
import gg.scala.cgs.common.teams.CgsGameTeamService
import gg.scala.events.plugin.game.ScalaEventsGameEngine
import gg.scala.events.shared.ModuleSize
import gg.scala.events.shared.type.EventType
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.flavor.service.ignore.IgnoreAutoScan
import me.lucko.helper.Events
import me.lucko.helper.terminable.composite.CompositeTerminable
import org.bukkit.event.player.PlayerJoinEvent

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

    var size = ModuleSize.ONE_VS_ONE

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

        // reconfigure teams
        CgsGameTeamService.teams.clear()
        CgsGameTeamService.configure()

        CgsGameArenaHandler.configure(module.type.mode)
        engine.gameArena = CgsGameArenaHandler.arena

        module.configure(size)

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
/*                val message = AwareMessage
                    .of(
                        "event-alert",
                        ScalaEventsChannel.channel(),
                        "player" to QuickAccess.coloredName(it.player.uniqueId),
                        "event" to module.type.info.fancyNameRender,
                        "server" to ServerSync.local.id
                    )

                message.publish()*/
                hasFirstPlayerJoined = true
            }
    }
}
