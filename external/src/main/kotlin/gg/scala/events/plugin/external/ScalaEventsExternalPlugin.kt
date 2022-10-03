package gg.scala.events.plugin.external

import gg.scala.commons.ExtendedScalaPlugin
import me.lucko.helper.plugin.ap.Plugin
import me.lucko.helper.plugin.ap.PluginDependency

/**
 * @author GrowlyX
 * @since 8/5/2022
 */
@Plugin(
    name = "ScEvents",
    depends = [
        PluginDependency("scala-commons"),
        PluginDependency("store-spigot")
    ]
)
class ScalaEventsExternalPlugin : ExtendedScalaPlugin()
