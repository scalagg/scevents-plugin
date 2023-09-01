package gg.scala.events.plugin.game.module.bonkers.teams.commands

import gg.scala.cgs.common.teams.CgsGameTeamService
import gg.scala.commons.acf.CommandHelp
import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.Default
import gg.scala.commons.acf.annotation.Description
import gg.scala.commons.acf.annotation.HelpCommand
import gg.scala.commons.acf.annotation.Optional
import gg.scala.commons.acf.annotation.Subcommand
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.events.plugin.game.module.bonkers.account
import gg.scala.events.plugin.game.module.bonkers.data
import gg.scala.lemon.player.LemonPlayer
import gg.scala.lemon.util.QuickAccess.username
import gg.scala.events.plugin.game.module.bonkers.teams.joinTeam
import gg.scala.events.plugin.game.module.bonkers.teams.sendTeamInviteTo
import gg.scala.events.plugin.game.module.bonkers.teams.team
import net.evilblock.cubed.util.CC

/**
 * @author GrowlyX
 * @since 4/26/2023
 */
@CommandAlias("faction|fac|team|team")
object TeamCommands : ScalaCommand()
{
    @Default
    @HelpCommand
    fun onHelp(help: CommandHelp)
    {
        help.showHelp()
    }

    @Subcommand("invite")
    @Description("Invite another user to join your team.")
    fun onInvite(player: ScalaPlayer, target: LemonPlayer)
    {
        val team = player.bukkit().team
            ?: throw ConditionFailedException(
                "You are not in a team right now!"
            )

        team.sendTeamInviteTo(target.uniqueId)
    }

    @Subcommand("deposit|d")
    @Description("Deposit balance into your team's bank")
    fun onDeposit(player: ScalaPlayer, amount: String)
    {
        val account = player.bukkit().account()
        val team = player.bukkit().team
            ?: throw ConditionFailedException(
                "You are not in a team right now!"
            )
        val teamData = team.data()!!
        val max = account.balance

        if (amount.equals("all", ignoreCase = true))
        {
            if (max == 0)
            {
                throw ConditionFailedException(
                    "You do not have any balance to deposit."
                )
            }

            teamData.balance += max
            account.balance = 0

            player.sendMessage("${CC.GREEN}You have deposited ${CC.WHITE}$${max} ${CC.GREEN}into your team's balance.")
        } else
        {
            val deposit = amount.toIntOrNull()
                ?: throw ConditionFailedException(
                    "${CC.YELLOW}$amount ${CC.RED}is not a valid integer."
                )

            if (deposit == 0)
            {
                throw ConditionFailedException(
                    "${CC.RED}You cannot deposit $0."
                )
            }

            if (max < deposit)
            {
                throw ConditionFailedException(
                    "${CC.RED}You do not have enough balance to deposit. You need ${CC.YELLOW}$${deposit - max} ${CC.RED}more."
                )
            }

            teamData.balance += deposit
            account.balance -= deposit
            player.sendMessage("${CC.GREEN}You have deposited ${CC.WHITE}$$deposit ${CC.GREEN}into your team's balance.")
        }
    }

    @Subcommand("join")
    @Description("Join a team via an invite.")
    fun onJoin(player: ScalaPlayer, team: Int)
    {
        player.bukkit().joinTeam(team)
    }

    @Subcommand("view")
    @Description("View details for a particular team.")
    fun onView(player: ScalaPlayer, @Optional team: Int?)
    {
        val teamId = team ?: player.bukkit().team?.id
        val team = CgsGameTeamService.teams[teamId]
            ?: throw ConditionFailedException(
                "Team #$teamId does not exist in this game."
            )
        val teamData = team.data()!!

        player.sendMessage(
            "${CC.WHITE}Team ${teamData.colored()}${CC.WHITE}:",
            "${CC.WHITE}Location: ${teamData.color}${teamData.spawnLocation.x}, ${teamData.spawnLocation.y}, ${teamData.spawnLocation.z}",
            "${CC.WHITE}Balance: ${teamData.color}$${teamData.balance}",
            "",
            "${CC.GRAY}Participants:"
        )

        if (team.participants.isEmpty())
        {
            player.sendMessage("${CC.RED}No participants!")
        } else
        {
            team.participants.forEach {
                player.sendMessage("${CC.WHITE}- ${it.username()}")
            }
        }
    }
}
