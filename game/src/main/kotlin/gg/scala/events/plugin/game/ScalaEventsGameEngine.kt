package gg.scala.events.plugin.game

import gg.scala.cgs.common.CgsGameEngine
import gg.scala.cgs.common.snapshot.CgsGameSnapshot
import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.events.plugin.game.module.EventOmnibus
import gg.scala.events.plugin.game.module.defaults.DefaultEventNametags
import gg.scala.events.plugin.game.module.defaults.DefaultEventScoreboard
import gg.scala.events.plugin.game.module.defaults.DefaultEventSnapshot
import gg.scala.events.plugin.game.module.defaults.DefaultEventVisibility
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
    plugin,
    ScalaEventsGameInfo,
    ScalaEventsCustomGameMode,
    ScalaEventsPlayerModel::class
)
{
    override fun getScoreboardRenderer() =
        EventOmnibus.moduleOr({
            scoreboardFormatter
        }) {
            DefaultEventScoreboard
        }

    override fun getVisibilityAdapter() =
        EventOmnibus.moduleOr({
            visibilityAdapter
        }) {
            DefaultEventVisibility
        }

    override fun getGameSnapshot() =
        EventOmnibus.moduleOr({
            gameSnapshot
        }) {
            DefaultEventSnapshot
        }

    override fun getNametagAdapter() =
        EventOmnibus.moduleOr({
            nametagProvider
        }) {
            DefaultEventNametags
        }
}
