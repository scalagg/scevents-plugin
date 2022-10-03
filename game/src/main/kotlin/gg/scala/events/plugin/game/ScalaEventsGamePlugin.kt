package gg.scala.events.plugin.game

import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.commons.config.annotations.ContainerConfig
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
@ContainerConfig("config", ScalaEventsGameConfig::class)
class ScalaEventsGamePlugin : ExtendedScalaPlugin()
