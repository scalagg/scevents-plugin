package gg.scala.events.plugin.game.module

import gg.scala.events.shared.type.EventType
import kotlin.reflect.KClass

/**
 * @author GrowlyX
 * @since 10/3/2022
 */
@Target(
    AnnotationTarget.CLASS
)
annotation class EventModuleBindType(
    val value: KClass<out EventType>
)
