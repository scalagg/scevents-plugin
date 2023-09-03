package gg.scala.events.plugin.game.module.bonkers.shops

import gg.scala.events.plugin.game.module.bonkers.account
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.math.Numbers
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class CarpenterMenu : Menu("Carpenter")
{
    init
    {
        placeholder = true
    }

    override fun size(buttons: Map<Int, Button>) = 54

    private fun buildPurchasable(
        amount: Int,
        name: String,
        stack: ItemBuilder,
        stackQuantity: Int
    ) = ItemBuilder
        .copyOf(stack)
        .name("${CC.YELLOW}$name")
        .addToLore(
            "${CC.GRAY}Price: ${CC.WHITE}$${
                Numbers.format(amount)
            }",
            "",
            "${CC.GREEN}Click to purchase!"
        )
        .toButton { player, _ ->
            val balance = player!!.account().balance

            if (balance < amount)
            {
                player.sendMessage("${CC.RED}You don't have enough money for this!")
                return@toButton
            }

            player.inventory.addItem(stack.build())
            player.account().balance -= amount

            player.updateInventory()
            player.sendMessage(
                "${CC.GREEN}You purchased ${stackQuantity}x $name for ${CC.WHITE}$$amount${CC.GREEN}!"
            )
        }

    override fun getButtons(player: Player): Map<Int, Button>
    {
        val buttons = mutableMapOf<Int, Button>()

        buttons[11] = buildPurchasable(
            amount = 16,
            name = "Fence Gate",
            stack = ItemBuilder.of(Material.FENCE_GATE),
            stackQuantity = 1
        )

        buttons[12] = buildPurchasable(
            amount = 32,
            name = "Wood",
            stack = ItemBuilder.of(Material.WOOD),
            stackQuantity = 1
        )

        buttons[13] = buildPurchasable(
            amount = 10,
            name = "Stone Blocks",
            stack = ItemBuilder.of(Material.STONE),
            stackQuantity = 5
        )

        buttons[14] = buildPurchasable(
            amount = 60,
            name = "Stone Bricks",
            stack = ItemBuilder.of(Material.SMOOTH_BRICK),
            stackQuantity = 5
        )

        buttons[15] = buildPurchasable(
            amount = 30,
            name = "Chest",
            stack = ItemBuilder.of(Material.CHEST),
            stackQuantity = 1
        )

        buttons[21] = buildPurchasable(
            amount = 450,
            name = "Diamond Pickaxe",
            stack = ItemBuilder.of(Material.DIAMOND_PICKAXE),
            stackQuantity = 1
        )
        buttons[22] = buildPurchasable(
            amount = 300,
            name = "Diamond Axe",
            stack = ItemBuilder.of(Material.DIAMOND_AXE),
            stackQuantity = 1
        )
        buttons[23] = buildPurchasable(
            amount = 250,
            name = "Diamond Shovel",
            stack = ItemBuilder.of(Material.DIAMOND_SPADE),
            stackQuantity = 1
        )


        return buttons
    }
}