package gg.scala.events.plugin.game.module

import gg.scala.cgs.common.information.arena.CgsGameArenaHandler
import gg.scala.events.plugin.game.ScalaEventsGameEngine
import gg.scala.events.shared.type.EventType
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.flavor.service.ignore.IgnoreAutoScan

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


    }
}
