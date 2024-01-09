package gg.scala.events.plugin.game.module.bonkers.commands

import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandPermission
import gg.scala.commons.acf.annotation.PreCommand
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.events.plugin.game.ScalaEventsGamePlugin
import gg.scala.events.plugin.game.module.bonkers.account
import gg.scala.events.plugin.game.module.bonkers.teams.timers.CombatTimer
import gg.scala.flavor.inject.Inject
import gg.scala.lemon.player.LemonPlayer
import net.evilblock.cubed.util.CC
import org.bukkit.Bukkit

object PayCommand : ScalaCommand()
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

    @CommandAlias("pay|sendmoney")
    fun onBalanceRequest(player: ScalaPlayer, target: LemonPlayer, amount: Int)
    {
        val found = Bukkit.getPlayer(target.uniqueId)

        if (found == null || !found.isOnline)
        {
            throw ConditionFailedException(
                "The player ${CC.YELLOW}${target.name} ${CC.RED}is not currently online"
            )
        }

        if (amount == 0)
        {
            throw ConditionFailedException(
                "You cannot pay another user $0."
            )
        }

        if (player.bukkit().uniqueId == found.uniqueId)
        {
            throw ConditionFailedException(
                "You cannot send money to yourself!"
            )
        }

        val targetAccount = found.account()
        val senderAccount = player.bukkit().account()

        if (senderAccount.balance < amount)
        {
            throw ConditionFailedException(
                "You do not have enough money! You need ${CC.YELLOW}$${amount - senderAccount.balance} ${CC.RED}more."
            )
        }

        targetAccount.balance += amount
        senderAccount.balance -= amount
        player.sendMessage("${CC.GREEN}You have sent $$amount to ${target.name}")
        found.sendMessage("${CC.GREEN}You have received $$amount from ${player.bukkit().name}")
    }
}
