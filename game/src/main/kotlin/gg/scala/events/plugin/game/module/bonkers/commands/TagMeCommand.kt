package gg.scala.events.plugin.game.module.bonkers.commands

import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandPermission
import gg.scala.commons.acf.annotation.PreCommand
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.events.plugin.game.ScalaEventsGamePlugin
import gg.scala.events.plugin.game.module.bonkers.teams.timers.CombatTimer
import gg.scala.flavor.inject.Inject
import net.evilblock.cubed.util.CC

object TagMeCommand : ScalaCommand()
{
    @Inject
    lateinit var plugin: ScalaEventsGamePlugin

    @CommandAlias("tagme")
    @CommandPermission("bunkers.tagme")
    fun onTagMe(player: ScalaPlayer)
    {
        if (CombatTimer.isActive(player.bukkit()))
        {
            CombatTimer.reset(player.bukkit())
        }

        CombatTimer.addOrOverride(player.bukkit())
        player.sendMessage("${CC.RED}You have put yourself onto Combat Tag manually.")
    }
}
