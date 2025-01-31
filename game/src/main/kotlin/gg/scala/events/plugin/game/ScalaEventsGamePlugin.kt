package gg.scala.events.plugin.game

import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.commons.annotations.container.ContainerDisable
import gg.scala.commons.annotations.container.ContainerEnable
import gg.scala.commons.annotations.container.ContainerPreEnable
import gg.scala.commons.annotations.custom.CustomAnnotationProcessors
import gg.scala.events.plugin.game.module.EventBind
import gg.scala.events.plugin.game.module.EventModule
import gg.scala.events.plugin.game.module.EventOmnibus
import gg.scala.events.shared.ScalaEventsChannel
import gg.scala.game.extensions.cosmetics.CosmeticLocalConfig
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
        PluginDependency("Lemon"),
        PluginDependency("Friends"),
        PluginDependency("ScGameFramework"),
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
            .process<EventBind> {
                val annotation = it.javaClass
                    .getAnnotation(
                        EventBind::class.java
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

        CosmeticLocalConfig.enableCosmeticResources = false

        flavor().bind<ScalaEventsGameEngine>() to engine
    }

    @ContainerDisable
    fun containerDisable()
    {
        ScalaEventsChannel
            .kv().sync()
            .del("events:status")
    }
}
