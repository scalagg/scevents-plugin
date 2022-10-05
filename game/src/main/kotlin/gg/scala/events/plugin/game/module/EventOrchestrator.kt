package gg.scala.events.plugin.game.module

import gg.scala.aware.message.AwareMessage
import gg.scala.commons.agnostic.sync.ServerSync
import gg.scala.events.plugin.game.ScalaEventsGameEngine
import gg.scala.events.plugin.game.ScalaEventsGamePlugin
import gg.scala.events.shared.ScalaEventsChannel
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.inject.condition.Named
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import me.lucko.helper.Events
import net.evilblock.cubed.serializers.Serializers
import net.evilblock.cubed.util.CC
import org.bukkit.event.EventPriority
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import java.util.*

/**
 * @author GrowlyX
 * @since 10/4/2022
 */
@Service
object EventOrchestrator
{
    @Inject
    lateinit var plugin: ScalaEventsGamePlugin

    @Inject
    lateinit var engine: ScalaEventsGameEngine

    private var eventStarted = false

    @Configure
    fun configure()
    {
        Events
            .subscribe(
                AsyncPlayerPreLoginEvent::class.java,
                EventPriority.MONITOR
            )
            .handler {
                if (!eventStarted)
                {
                    it.disallow(
                        AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        "${CC.RED}An event has not been prepared for you yet!"
                    )
                }
            }

        ScalaEventsChannel.channel()
            .listen("start-event") {
                if (eventStarted)
                {
                    plugin.logger.info("Tried starting event while event already started")
                    return@listen
                }

                val type = Class.forName(
                    retrieve("eventType")
                )

                val mapping = EventOmnibus
                    .bindings[type.kotlin.objectInstance]
                    ?: return@listen run {
                        plugin.logger.info("Tried starting event with event type ${type.name}")
                    }

                ScalaEventsChannel.kv().sync()
                    .set(
                        "events:status", Serializers.gson
                            .toJson(mapOf(
                                "type" to type.name,
                                "initiator" to retrieve<UUID>("uniqueId").toString(),
                                "startTime" to System.currentTimeMillis().toString(),
                                "serverId" to ServerSync.getLocalGameServer().id
                            ))
                    )

                plugin.flavor {
                    inject(mapping)
                    bind<EventModule>() to mapping

                    inject(EventOmnibus)
                }

                eventStarted = true
            }

        ScalaEventsChannel
            .channel()
            .connect()
            .toCompletableFuture()
            .join()

        // init lazy val
        ScalaEventsChannel.kv()
    }
}
