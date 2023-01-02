package gg.scala.events.plugin.external

import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.commons.annotations.container.ContainerEnable
import gg.scala.commons.annotations.container.flavor.LazyStartup
import gg.scala.events.shared.ScalaEventsChannel
import me.lucko.helper.plugin.ap.Plugin
import me.lucko.helper.plugin.ap.PluginDependency

/**
 * @author GrowlyX
 * @since 8/5/2022
 */
@Plugin(
    name = "ScEvents",
    depends = [
        PluginDependency("Lemon"),
        PluginDependency("scala-commons"),
        PluginDependency("cloudsync", soft = true),
        PluginDependency("store-spigot")
    ]
)
@LazyStartup
class ScalaEventsExternalPlugin : ExtendedScalaPlugin()
{
    @ContainerEnable
    fun containerEnable()
    {
        ScalaEventsChannel.channel().connect()
            .thenRun {
                ScalaEventsChannel.kv()
            }
    }
}
