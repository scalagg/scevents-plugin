package gg.scala.events.plugin.game

import gg.scala.cgs.common.CgsGameEngine
import gg.scala.cgs.common.snapshot.CgsGameSnapshot
import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.events.shared.ScalaEventsCustomGameMode
import gg.scala.events.shared.ScalaEventsGameInfo
import gg.scala.events.shared.ScalaEventsPlayerModel

/**
 * @author GrowlyX
 * @since 12/3/2021
 */
class ScalaEventsGameEngine(
    plugin: ExtendedScalaPlugin,
) : CgsGameEngine<ScalaEventsPlayerModel>(
    plugin, ScalaEventsGameInfo, ScalaEventsCustomGameMode,
    ScalaEventsPlayerModel::class
)
{
    override fun getScoreboardRenderer() = SurvivalGamesScoreboard
    override fun getVisibilityAdapter() = SurvivalGamesVisibilityAdapter

    override fun getGameSnapshot() = object : CgsGameSnapshot
    {
        override fun getExtraInformation() = listOf<String>()
    }

    override fun getNametagAdapter() = SurvivalGamesNametagAdapter
}
