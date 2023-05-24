package gg.scala.events.plugin.game.module.bonkers.teams.channel

import gg.scala.lemon.channel.ChatChannelComposite
import gg.scala.lemon.player.rank.Rank
import gg.scala.lemon.util.QuickAccess.username
import gg.scala.events.plugin.game.module.bonkers.teams.team
import net.evilblock.cubed.util.CC
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

/**
 * @author GrowlyX
 * @since 4/26/2023
 */
object TeamChatChannelComposite : ChatChannelComposite
{
    override fun format(
        sender: UUID, receiver: Player?,
        message: String, server: String, rank: Rank
    ): TextComponent
    {
        val senderPlayer = Bukkit.getPlayer(sender)
            ?: return Component.text("")

        return LegacyComponentSerializer.legacySection()
            .deserialize("${CC.GOLD}[Team] ${CC.GRAY}[#${senderPlayer.team?.id}] ${rank.color}${
                sender.username()
            }${CC.GRAY}: ${CC.RESET}$message")
    }

    override fun identifier() = "teamchat"
}
