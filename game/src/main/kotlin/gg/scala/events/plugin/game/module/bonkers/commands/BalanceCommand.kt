package gg.scala.events.plugin.game.module.bonkers.commands

import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandPermission
import gg.scala.commons.acf.annotation.PreCommand
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.events.plugin.game.ScalaEventsGamePlugin
import gg.scala.events.plugin.game.module.bonkers.account
import gg.scala.events.plugin.game.module.bonkers.teams.timers.CombatTimer
import gg.scala.flavor.inject.Inject
import net.evilblock.cubed.util.CC

@AutoRegister
object BalanceCommand : ScalaCommand()
{
    @Inject
    lateinit var plugin: ScalaEventsGamePlugin

    @PreCommand
    fun onPreCommand(player: ScalaPlayer)
    {
        if (player.bukkit().hasMetadata("spectator"))
        {
            throw ConditionFailedException(
                "You may not use team commands as a spectator!"
            )
        }
    }

    @CommandAlias("balance|bal|bank|money")
    fun onBalanceRequest(player: ScalaPlayer)
    {
        val account = player.bukkit().account()

        player.sendMessage("${CC.WHITE}Your balance: ${CC.GOLD}$${account.balance}")
    }
}
