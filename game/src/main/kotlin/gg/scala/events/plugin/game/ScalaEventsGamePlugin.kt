package gg.scala.events.plugin.game

import gg.scala.cgs.common.CgsGameEngine
import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.commons.annotations.container.ContainerEnable
import gg.scala.commons.annotations.container.ContainerPreEnable
import gg.scala.commons.annotations.custom.CustomAnnotationProcessors
import gg.scala.events.plugin.game.module.EventModule
import gg.scala.events.plugin.game.module.EventModuleBindType
import gg.scala.events.plugin.game.module.EventOmnibus
import gg.scala.flavor.binder.FlavorBinderContainer
import me.lucko.helper.plugin.ap.Plugin
import me.lucko.helper.plugin.ap.PluginDependency

/**
 * @author GrowlyX
 * @since 8/4/2022
 */
@Plugin(
    name = "ScEvents",
    depends = [
        PluginDependency("scala-commons"),
        PluginDependency("store-spigot"),
        PluginDependency("CGS-Engine"),
        PluginDependency("SlimeWorldManager"),
        PluginDependency("cloudsync", soft = true)
    ]
)
class ScalaEventsGamePlugin : ExtendedScalaPlugin()
{
    @ContainerPreEnable
    fun containerPreEnable()
    {
        CustomAnnotationProcessors
            .process<EventModuleBindType> {
                val annotation = it.javaClass
                    .getAnnotation(
                        EventModuleBindType::class.java
                    )

                EventOmnibus.bind(
                    annotation.value.objectInstance!!,
                    it as EventModule
                )
            }
    }

    @ContainerEnable
    fun containerEnable()
    {
        val engine = ScalaEventsGameEngine(this)
        engine.initialLoad()

        val module =
            object : FlavorBinderContainer()
            {
                override fun populate()
                {
                    bind(engine)
                        .to<ScalaEventsGameEngine>()
                        .to<CgsGameEngine<*>>()
                }
            }

        flavor().inherit(module)
    }
}
