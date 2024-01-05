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
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.events.plugin.game.module.bonkers.account
import gg.scala.events.plugin.game.module.bonkers.data
import gg.scala.lemon.player.LemonPlayer
import gg.scala.lemon.util.QuickAccess.username
import gg.scala.events.plugin.game.module.bonkers.teams.joinTeam
import gg.scala.events.plugin.game.module.bonkers.teams.sendTeamInviteTo
import gg.scala.events.plugin.game.module.bonkers.teams.team
import gg.scala.events.plugin.game.module.bonkers.teams.timers.CombatTimer
import gg.scala.events.plugin.game.module.bonkers.teams.timers.TeamHomeTimer
import net.evilblock.cubed.util.CC
import org.bukkit.Bukkit

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

    @Subcommand("focus")
    @Description("Set your current faction focus.")
    fun onFocus(player: ScalaPlayer, team: Int)
    {

        val found = CgsGameTeamService.teams[team]
            ?: throw ConditionFailedException(
                "Team ${CC.YELLOW}#$team ${CC.RED}does not exist in this game."
            )

        val senderTeam = player.bukkit().team
            ?: throw ConditionFailedException(
                "You are not currently on a team!"
            )

        val teamData = senderTeam.data()!!

        teamData.teamFocus = found.id
        player.sendMessage("${CC.GREEN}You have set your team focus to ${found.data()!!.colored()}${CC.GREEN}.")
    }

    @Subcommand("wiperally|wr")
    @Description("Wipe your team's current rally point")
    fun onTeamWipeRally(player: ScalaPlayer)
    {
        val senderTeam = player.bukkit().team
            ?: throw ConditionFailedException(
                "You are not currently on a team!"
            )
        val data = senderTeam.data()!!

        if (data.teamRally == null)
        {
            throw ConditionFailedException(
                "Unable to wipe your team's rally point as it was not set."
            )
        }

        data.teamRally = null
        //TODO: handle team rally removal

        player.sendMessage("${CC.GREEN}You have wiped your team's current rally point.")
    }

    @Subcommand("rally|r")
    @Description("Set your team's current rally point.")
    fun onTeamRally(player: ScalaPlayer)
    {
        val senderTeam = player.bukkit().team
            ?: throw ConditionFailedException(
                "You are not currently on a team!"
            )
        val data = senderTeam.data()!!

        val bukkitPlayer = player.bukkit()
        val playerLocation = bukkitPlayer.location

        data.teamRally = playerLocation
        //TODO: handle team rally waypoint

        senderTeam.participants
            .mapNotNull {
                Bukkit.getPlayer(it)
            }
            .forEach {
                it.sendMessage("${CC.GOLD}[Team] ${CC.GOLD}${bukkitPlayer.name}${CC.SEC} has updated your team's rally point!")
            }
    }

    @Subcommand("unfocus")
    @Description("Wipe your current faction focus.")
    fun onUnfocus(player: ScalaPlayer)
    {
        val senderTeam = player.bukkit().team
            ?: throw ConditionFailedException(
                "You are not currently on a team!"
            )

        val data = senderTeam.data()!!

        if (data.teamFocus == null)
        {
            throw ConditionFailedException(
                "Your faction focus is already empty!"
            )
        }

        data.teamFocus = null
        player.sendMessage("${CC.GREEN}You have cleared your current faction focus.")
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

    @Subcommand("withdraw|w")
    @Description("Withdraw a certain amount from your team's balance.")
    fun onWithdraw(player: ScalaPlayer, amount: String)
    {
        val account = player.bukkit().account()
        val team = player.bukkit().team
            ?: throw ConditionFailedException(
                "You are not in a team right now!"
            )
        val teamData = team.data()!!

        if (amount.equals("all", ignoreCase = true))
        {
            account.balance += teamData.balance
            teamData.balance = 0

            player.sendMessage("${CC.GREEN}You have withdrew ${CC.WHITE}$${account.balance} ${CC.GREEN}from your team's balance.")
        } else
        {
            val withdrawal = amount.toIntOrNull()
                ?: throw ConditionFailedException(
                    "${CC.YELLOW}$amount ${CC.RED}is not a valid integer."
                )

            if (withdrawal == 0)
            {
                throw ConditionFailedException(
                    "${CC.RED}You cannot withdraw $0."
                )
            }

            if (teamData.balance < withdrawal)
            {
                throw ConditionFailedException(
                    "${CC.RED}Your team does not have enough money to withdraw. You need ${CC.YELLOW}$${withdrawal - teamData.balance} ${CC.RED}more."
                )
            }

            account.balance += withdrawal
            teamData.balance -= withdrawal
            player.sendMessage("${CC.GREEN}You have withdrew ${CC.WHITE}$$withdrawal ${CC.GREEN}from your team's balance.")
        }
    }

    @Subcommand("join")
    @Description("Join a team via an invite.")
    fun onJoin(player: ScalaPlayer, team: Int)
    {
        player.bukkit().joinTeam(team)
    }

    @Subcommand("home|hq")
    @Description("Teleport to your team's set spawn location")
    fun onTeamSpawn(player: ScalaPlayer)
    {
        val team = player.bukkit().team
            ?: throw ConditionFailedException(
                "You are not in a team right now!"
            )

        if (TeamHomeTimer.isActive(player.bukkit()))
        {
            throw ConditionFailedException(
                "You are already on team home timer!"
            )
        }

        if (CombatTimer.isActive(player.bukkit()))
        {
            throw ConditionFailedException(
                "You must be off of Combat Tag in order to be put on team home timer."
            )
        }

        TeamHomeTimer.addOrOverride(player.bukkit())
        player.sendMessage("${CC.GREEN}You have started your team home timer!")
    }

    @Subcommand("view")
    @Description("View details for a particular team.")
    fun onView(player: ScalaPlayer, @Optional team: Int?)
    {
        val teamId = team ?: player.bukkit().team?.id
        ?: throw ConditionFailedException(
            "You are not currently on a team!"
        )

        val team = CgsGameTeamService.teams[teamId]
            ?: throw ConditionFailedException(
                "The team ${CC.YELLOW}#$teamId ${CC.RED}does not exist!"
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
