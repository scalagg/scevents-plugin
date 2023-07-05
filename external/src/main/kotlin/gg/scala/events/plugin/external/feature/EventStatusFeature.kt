package gg.scala.events.plugin.external.feature

import gg.scala.events.shared.ScalaEventsChannel
import gg.scala.flavor.service.Service
import net.evilblock.cubed.serializers.Serializers
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
}
