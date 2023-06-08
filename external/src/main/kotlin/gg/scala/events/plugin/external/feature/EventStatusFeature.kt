package gg.scala.events.plugin.external.feature

import gg.scala.commons.agnostic.sync.ServerSync
import gg.scala.events.shared.ScalaEventsChannel
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.lemon.redirection.impl.VelocityRedirectSystem
import net.evilblock.cubed.serializers.Serializers
import org.bukkit.Bukkit
import java.util.*

/**
 * @author GrowlyX
 * @since 10/4/2022
 */
@Service
object EventStatusFeature
{
    data class EventStatus(
        val type: String,
        val initiator: UUID,
        val startTime: Long,
        val serverId: String
    )

    fun status() = ScalaEventsChannel
        .kv().sync()
        .get("events:status")
        ?.let {
            Serializers.gson.fromJson(it, EventStatus::class.java)
        }

    @Configure
    fun configure()
    {
        ScalaEventsChannel.channel()
            .listen("send-user") {
                val player = Bukkit
                    .getPlayer(
                        retrieve<UUID>("user")
                    )
                    ?: return@listen

                VelocityRedirectSystem.redirect(
                    player, retrieve("server")
                )
            }

        ScalaEventsChannel
            .channel()
            .connect()
            .toCompletableFuture()
            .join()
    }
}
