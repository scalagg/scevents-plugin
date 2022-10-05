package gg.scala.events.plugin.external.feature

import gg.scala.commons.persist.ProfileOrchestrator
import gg.scala.events.shared.ScalaEventsPlayerModel
import gg.scala.flavor.service.Service
import java.util.*

/**
 * @author GrowlyX
 * @since 10/4/2022
 */
@Service
object EventPlayerFeature : ProfileOrchestrator<ScalaEventsPlayerModel>()
{
    override fun new(uniqueId: UUID) = ScalaEventsPlayerModel(uniqueId)
    override fun type() = ScalaEventsPlayerModel::class
}
