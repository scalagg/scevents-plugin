package gg.scala.events.plugin.game.module.bonkers.teams.commands

import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.PreCommand
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.events.plugin.game.ScalaEventsGamePlugin
import gg.scala.flavor.inject.Inject
import net.evilblock.cubed.util.CC
import org.bukkit.metadata.FixedMetadataValue

/**
 * @author GrowlyX
 * @since 4/27/2023
 */
@AutoRegister
object TeamChatCommand : ScalaCommand()
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

    @CommandAlias("teamchat|tc|factionchat|fc")
    fun onTeamChat(player: ScalaPlayer)
    {
        if (player.bukkit().hasMetadata("teamchat"))
        {
            player.bukkit().removeMetadata("teamchat", plugin)
            player.bukkit().sendMessage("${CC.RED}You are no longer chatting in the team channel.")
            return
        }

        player.bukkit().setMetadata("teamchat", FixedMetadataValue(plugin, true))
        player.bukkit().sendMessage("${CC.GREEN}You are now chatting in the team channel.")
    }
}
