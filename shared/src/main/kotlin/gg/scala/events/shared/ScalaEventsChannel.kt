package gg.scala.events.shared

import gg.scala.aware.AwareBuilder
import gg.scala.aware.codec.codecs.interpretation.AwareMessageCodec
import gg.scala.aware.message.AwareMessage
import java.util.logging.Logger

/**
 * @author GrowlyX
 * @since 10/3/2022
 */
object ScalaEventsChannel
{
    private val aware by lazy {
        AwareBuilder
            .of<AwareMessage>("events:plugin")
            .codec(AwareMessageCodec)
            .logger(Logger.getAnonymousLogger())
            .build()
    }

    fun channel() = aware
}
