package gg.scala.events.shared

import gg.scala.aware.AwareBuilder
import gg.scala.aware.codec.codecs.interpretation.AwareMessageCodec
import gg.scala.aware.message.AwareMessage
import me.lucko.helper.utils.Players
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import net.evilblock.cubed.util.bukkit.FancyMessage
import net.md_5.bungee.api.chat.ClickEvent
import java.util.logging.Logger

/**
 * @author GrowlyX
 * @since 10/3/2022
 */
object ScalaEventsChannel
{
    private val aware by lazy {
        val aware = AwareBuilder
            .of<AwareMessage>("events:plugin")
            .codec(AwareMessageCodec)
            .logger(Logger.getAnonymousLogger())
            .build()

        aware
            .listen("event-alert") {
                val alerter = retrieve<String>("player")
                val event = retrieve<String>("event")
                val server = retrieve<String>("server")

                val header = "${CC.B_PRI}Event ${CC.B_GRAY}${Constants.DOUBLE_ARROW_RIGHT} "

                fun FancyMessage.style(): FancyMessage
                {
                    return apply {
                        andHoverOf(
                            "${CC.B_GREEN}Click to join the event!",
                            "",
                            "${CC.GREEN}By joining this event, you",
                            "${CC.GREEN}will be switching to the",
                            "${CC.GREEN}server ${CC.B_GREEN}$server${CC.GREEN}!",
                        )
                        andCommandOf(
                            ClickEvent.Action.RUN_COMMAND,
                            "/join $server"
                        )
                    }
                }

                val fancy = FancyMessage()
                    .withMessage(
                        "$header${
                            "${CC.PRI}$alerter${CC.SEC} is hosting a ${CC.WHITE}$event${CC.SEC} event!"
                        }"
                    )
                    .style()

                val later = FancyMessage()
                    .withMessage(
                        "$header${CC.GREEN}(Click to join)"
                    )
                    .style()

                Players.all().forEach {
                    fancy.sendToPlayer(it)
                    later.sendToPlayer(it)
                }
            }

        aware
    }

    private val connection by lazy {
        aware.internal().connect()
    }

    fun kv() = connection
    fun channel() = aware
}
