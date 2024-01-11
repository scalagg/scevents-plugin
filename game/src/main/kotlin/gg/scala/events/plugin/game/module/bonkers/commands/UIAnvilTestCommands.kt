package gg.scala.events.plugin.game.module.bonkers.commands

import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandPermission
import gg.scala.commons.acf.annotation.Default
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.events.plugin.game.ScalaEventsGamePlugin
import gg.scala.events.plugin.game.module.bonkers.account
import gg.scala.flavor.inject.Inject
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.wesjd.anvilgui.AnvilGUI
import net.wesjd.anvilgui.AnvilGUI.ResponseAction
import org.bukkit.Material
import org.bukkit.entity.Damageable

/**
 * @author GrowlyX
 * @since 1/10/2024
 */
@AutoRegister
@CommandAlias("uianviltest")
@CommandPermission("op")
object UIAnvilTestCommands : ScalaCommand()
{
    @Inject
    lateinit var plugin: ScalaEventsGamePlugin

    @Default
    fun default(player: ScalaPlayer)
    {
        AnvilGUI.Builder()
            .onClick { slot: Int, stateSnapshot: AnvilGUI.StateSnapshot ->
                if (slot != AnvilGUI.Slot.OUTPUT)
                {
                    return@onClick emptyList<ResponseAction>()
                }

                if (stateSnapshot.leftItem == null || stateSnapshot.leftItem.type == Material.AIR)
                {
                    return@onClick listOf(ResponseAction.run {
                        stateSnapshot.player.sendMessage("${CC.RED}You must enter an item on the left!")
                        Button.playFail(stateSnapshot.player)
                    })
                }

                if (stateSnapshot.leftItem.itemMeta !is Damageable)
                {
                    return@onClick listOf(ResponseAction.run {
                        stateSnapshot.player.sendMessage("${CC.RED}This item is not repairable!")
                        Button.playFail(stateSnapshot.player)
                    })
                }

                return@onClick listOf(ResponseAction.close(), ResponseAction.run {
                    stateSnapshot.player.account().balance -= 500
                    stateSnapshot.player.inventory.addItem(
                        stateSnapshot.leftItem.clone()
                            .apply { durability = type.maxDurability }
                    )
                    stateSnapshot.player.updateInventory()
                    stateSnapshot.player.sendMessage("${CC.GREEN}Your item has been repaired!")

                    Button.playSuccess(stateSnapshot.player)
                })
            }
            .itemOutput(
                ItemBuilder
                    .of(Material.EMERALD)
                    .name("${CC.GREEN}Click to repair!")
                    .addToLore(
                        "${CC.GRAY}Clicking this item will",
                        "${CC.GRAY}repair your item back to",
                        "${CC.GRAY}full durability.",
                        "",
                        "${CC.GREEN}Price: ${CC.WHITE}$500"
                    )
                    .build()
            )
            .text("Enter an item")
            .plugin(plugin)
            .open(player.bukkit())
    }
}
