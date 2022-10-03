package gg.scala.events.shared

import gg.scala.cgs.common.player.statistic.GameSpecificStatistics
import gg.scala.store.controller.DataStoreObjectControllerCache
import gg.scala.store.storage.type.DataStoreStorageType
import java.util.*

/**
 * @author GrowlyX
 * @since 10/3/2022
 */
class ScalaEventsPlayerModel(
    uniqueId: UUID
) : GameSpecificStatistics(uniqueId)
{
    override fun getAbstractType() =
        ScalaEventsPlayerModel::class.java

    override fun save() = DataStoreObjectControllerCache
        .findNotNull<ScalaEventsPlayerModel>()
        .save(this, DataStoreStorageType.MONGO)
}
