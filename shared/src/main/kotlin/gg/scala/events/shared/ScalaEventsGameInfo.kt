package gg.scala.events.shared

import gg.scala.cgs.common.information.CgsGameAwardInfo
import gg.scala.cgs.common.information.CgsGameGeneralInfo

/**
 * @author GrowlyX
 * @since 10/3/2022
 */
object ScalaEventsGameInfo : CgsGameGeneralInfo(
    fancyNameRender = "Events",
    gameVersion = 1.0F,
    minimumPlayers = 2,
    startingCountdownSec = 21,
    awards = CgsGameAwardInfo(
        false, 0..100, 0..100
    ),
    preStartVoting = false,
    disqualifyOnLogout = true,
    spectateOnDeath = true,
    showTabHearts = false,
    showNameHearts = true,
    usesCustomArenaWorld = true,
    gameModes = listOf(
        ScalaEventsCustomGameMode
    )
)
{
    init
    {
        configureSpectatorChat = false
    }
}
