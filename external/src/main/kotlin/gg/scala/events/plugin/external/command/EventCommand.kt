package gg.scala.events.plugin.external.command

import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.events.plugin.external.menu.EventsMenu

/**
 * @author GrowlyX
 * @since 10/3/2022
 */
@AutoRegister
object EventCommand : ScalaCommand()
{
    @CommandAlias("event|events|e")
    fun onEvents(player: ScalaPlayer)
    {
        EventsMenu().openMenu(player.bukkit())
    }
}
