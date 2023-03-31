package gg.scala.events.plugin.game.module.bonkers

import gg.scala.cgs.common.teams.CgsGameTeamService
import gg.scala.events.plugin.game.module.shuffle.WoolShuffleUtilities
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 3/31/2023
 */
class SelectTeamMenu : Menu("Select your team")
{
    init
    {
        autoUpdate = true
    }

    override fun getButtons(player: Player): Map<Int, Button>
    {
        val buttons = mutableMapOf<Int, Button>()

        CgsGameTeamService.teams
            .forEach {
                val mapping = BonkersModule
                    .mappings.getOrNull(it.value.id)
                    ?: return@forEach

                buttons[it.key - 1] = ItemBuilder
                    .of(Material.WOOL)
                    .data(
                        WoolShuffleUtilities.fromChatColor(mapping.second).toShort()
                    )
                    .name("${mapping.second}${CC.BOLD}${mapping.first}")
                    .apply {
                        it.value.participants
                            .forEach { player ->
                                val bukkit = Bukkit.getPlayer(player)
                                    ?: return@forEach

                                addToLore("${CC.GRAY}- ${bukkit.name}")
                            }

                        if (it.value.participants.isEmpty())
                        {
                            addToLore("${CC.RED}No players")
                        }
                    }
                    .addToLore(
                        "",
                        "${CC.GREEN}Join the team!"
                    )
                    .toButton { _, _ ->
                        if (player.uniqueId in it.value.participants)
                        {
                            player.sendMessage("${CC.RED}Already on this team!")
                            return@toButton
                        }

                        CgsGameTeamService.getTeamOf(player)
                            ?.participants?.remove(player.uniqueId)

                        it.value.participants.add(player.uniqueId)
                        player.sendMessage("${CC.GREEN}You joined team ${mapping.second}${CC.BOLD}${mapping.first}${CC.GREEN}.")

                        openMenu(player)
                    }
            }

        return buttons
    }
}
