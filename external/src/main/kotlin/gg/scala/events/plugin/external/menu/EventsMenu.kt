package gg.scala.events.plugin.external.menu

import com.cryptomorin.xseries.XMaterial
import gg.scala.commons.agnostic.sync.server.ServerContainer
import gg.scala.commons.agnostic.sync.server.impl.GameServer
import gg.scala.events.plugin.external.feature.EventPlayerFeature
import gg.scala.events.plugin.external.feature.EventStatusFeature
import gg.scala.events.shared.ScalaEventsChannel
import gg.scala.events.shared.type.EventType
import gg.scala.lemon.LemonConstants
import gg.scala.lemon.redirection.impl.VelocityRedirectSystem
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.serializers.Serializers
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.time.TimeUtil
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*

/**
 * @author GrowlyX
 * @since 10/3/2022
 */
class EventsMenu : Menu("Events ${Constants.DOUBLE_ARROW_RIGHT} Main")
{
    init
    {
        autoUpdate = true
        placeholder = true
    }

    override fun size(buttons: Map<Int, Button>) = 9

    override fun getButtons(player: Player): Map<Int, Button>
    {
        val buttons = mutableMapOf<Int, Button>()
        val status = EventStatusFeature.status()

        val additionalDescription = mutableListOf<String>()
        additionalDescription += ""
        additionalDescription += "${CC.GREEN}Active event:"

        val material: ItemStack

        if (status == null)
        {
            additionalDescription += "${CC.WHITE}No event is running!"

            material = ItemBuilder
                .of(XMaterial.GRAY_DYE)
                .build()
        } else
        {
            val type = Class.forName(status.type)
                .kotlin.objectInstance as EventType

            additionalDescription += "${CC.GREEN}Event: ${CC.WHITE}${
                type.info.fancyNameRender
            }"

            val server = ServerContainer.getServer(status.serverId)

            if (server != null && server is GameServer)
            {
                additionalDescription += "${CC.GREEN}Players: ${CC.WHITE}${
                    "${server.getPlayersCount()}/${server.getMaxPlayers()}"
                }"
            }

            additionalDescription += "${CC.GREEN}Started: ${CC.WHITE}${
                TimeUtil.formatIntoMMSS(
                    ((System.currentTimeMillis() - status.startTime) / 1000).toInt()
                )
            }"

            material = type.icon
        }

        additionalDescription += ""
        additionalDescription += "${CC.GREEN}${
            if (status == null) "${CC.RED}No event is running." else "Click to join!"
        }"

        buttons[2] = ItemBuilder
            .copyOf(material)
            .name("${CC.PRI}Join an Event")
            .addToLore(
                "${CC.GRAY}You will be disconnected from",
                "${CC.GRAY}the server you are on if you",
                "${CC.GRAY}choose to join an event.",
            )
            .addToLore(
                *additionalDescription.toTypedArray()
            )
            .toButton { _, _ ->
                if (status != null)
                {
                    VelocityRedirectSystem.redirect(player, status.serverId)
                    return@toButton
                }

                player.sendMessage("${CC.RED}There is no event running!")
            }

        val profile = EventPlayerFeature.find(player)

        if (profile != null)
        {
            buttons[4] = ItemBuilder
                .of(Material.BREWING_STAND_ITEM)
                .name("${CC.PRI}Your Statistics")
                .addToLore(
                    "${CC.WHITE}Kills: ${CC.GOLD}${profile.kills.value}",
                    "${CC.WHITE}Deaths: ${CC.GOLD}${profile.deaths.value}",
                    "",
                    "${CC.WHITE}Wins: ${CC.GOLD}${profile.wins.value}",
                    "${CC.WHITE}Losses: ${CC.GOLD}${profile.losses.value}",
                )
                .toButton()
        }

        buttons[6] = ItemBuilder
            .of(XMaterial.FIRE_CHARGE)
            .name("${CC.PRI}Host an Event")
            .addToLore(
                "${CC.GRAY}Select an event to host.",
                "",
                "${CC.AQUA}Prime${CC.GRAY} rank is",
                "${CC.GRAY}required to host events.",
                "",
                if (player.hasPermission("scevents.host-events"))
                {
                    "${CC.GREEN}Click to view events!"
                } else
                {
                    "${CC.RED}Upgrade your rank to do this!"
                }
            )
            .toButton { _, _ ->
                if (!player.hasPermission("scevents.host-events"))
                {
                    player.sendMessage(
                        "${CC.RED}You don't have permission to host events!"
                    )
                    player.sendMessage(
                        "${CC.RED}Purchase the ${CC.AQUA}Prime${CC.RED} rank on ${CC.YELLOW}store.${LemonConstants.WEB_LINK}${CC.RED} to host events!"
                    )
                    return@toButton
                }

                if (status != null)
                {
                    player.sendMessage("${CC.RED}There's an event running right now! Please wait until it is over before trying to host an event.")
                    return@toButton
                }

                EventsViewMenu().openMenu(player)
            }


        return buttons
    }
}
