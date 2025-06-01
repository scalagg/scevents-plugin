package gg.scala.events.plugin.host

import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.commons.annotations.container.ContainerEnable
import gg.scala.commons.annotations.container.ContainerDisable
import gg.scala.events.shared.ScalaEventsChannel
import me.lucko.helper.plugin.ap.Plugin
import me.lucko.helper.plugin.ap.PluginDependency

/**
 * Independent host-controlled event system for custom events
 *
 */
@Plugin(
    name = "ScEvents-Host",
    depends = [
        PluginDependency("scala-commons"),
        PluginDependency("store-spigot"),
        PluginDependency("Lemon"),
        PluginDependency("Friends"),
        PluginDependency("SlimeWorldManager"),
        PluginDependency("cloudsync", soft = true)
    ]
)
class ScalaEventsHostPlugin : ExtendedScalaPlugin()
{
    @ContainerEnable
    fun containerEnable()
    {
        logger.info("Independent host-controlled event system enabled!")
        logger.info("System ready for custom event hosting")
    }

    @ContainerDisable
    fun containerDisable()
    {
        ScalaEventsChannel
            .kv().sync()
            .del("events:host:status")
            
        logger.info("Independent host-controlled event system disabled!")
    }
} 