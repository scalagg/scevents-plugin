package gg.scala.events.plugin.game.module.bonkers

import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.math.Numbers
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * @author GrowlyX
 * @since 4/9/2023
 */
class ShopMenu : Menu("Shop")
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
        grant: ((Player) -> Unit)? = null
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

            if (grant == null)
            {
                player.inventory.addItem(stack.build())
            } else
            {
                grant(player)
            }

            player!!.account().balance -= amount

            player.updateInventory()
            player.sendMessage(
                "${CC.GREEN}You purchased 1x $name for ${CC.WHITE}$$amount${CC.GREEN}!"
            )
        }

    override fun getButtons(player: Player): Map<Int, Button>
    {
        val buttons = mutableMapOf<Int, Button>()

        buttons[11] = buildPurchasable(
            amount = 225,
            name = "Diamond Helmet",
            stack = ItemBuilder.of(Material.DIAMOND_HELMET)
        ) {
            it.inventory.helmet = ItemStack(Material.DIAMOND_HELMET)
        }

        buttons[11 + 9] = buildPurchasable(
            amount = 275,
            name = "Diamond Chestplate",
            stack = ItemBuilder.of(Material.DIAMOND_CHESTPLATE)
        ) {
            it.inventory.chestplate = ItemStack(Material.DIAMOND_CHESTPLATE)
        }

        buttons[11 + 18] = buildPurchasable(
            amount = 250,
            name = "Diamond Leggings",
            stack = ItemBuilder.of(Material.DIAMOND_LEGGINGS)
        ) {
            it.inventory.leggings = ItemStack(Material.DIAMOND_LEGGINGS)
        }

        buttons[11 + 27] = buildPurchasable(
            amount = 125,
            name = "Diamond Boots",
            stack = ItemBuilder.of(Material.DIAMOND_BOOTS)
        ) {
            it.inventory.boots = ItemStack(Material.DIAMOND_BOOTS)
        }

        buttons[12] = buildPurchasable(
            amount = 875,
            name = "Diamond Set",
            stack = ItemBuilder.of(Material.DIAMOND)
        ) {
            it.inventory.helmet = ItemStack(Material.DIAMOND_HELMET)
            it.inventory.chestplate = ItemStack(Material.DIAMOND_CHESTPLATE)
            it.inventory.leggings = ItemStack(Material.DIAMOND_LEGGINGS)
            it.inventory.boots = ItemStack(Material.DIAMOND_BOOTS)
        }

        buttons[12 + 9] = buildPurchasable(
            amount = 25,
            name = "Enderpearl",
            stack = ItemBuilder.of(Material.ENDER_PEARL)
        )

        buttons[10 + 9] = buildPurchasable(
            amount = 100,
            name = "Diamond Sword",
            stack = ItemBuilder.of(Material.DIAMOND_SWORD)
        )

        buttons[14 + 18] = buildPurchasable(
            amount = 75,
            name = "Steak",
            stack = ItemBuilder
                .of(Material.COOKED_BEEF)
                .amount(16)
        )

        buttons[15 + 9] = buildPurchasable(
            amount = 5,
            name = "Health Potion",
            stack = ItemBuilder
                .of(Material.POTION)
                .data(16421)
        )

        buttons[16 + 9] = buildPurchasable(
            amount = 50,
            name = "Poison Potion",
            stack = ItemBuilder
                .of(Material.POTION)
                .data(16388)
        )

        buttons[16 + 18] = buildPurchasable(
            amount = 50,
            name = "Slowness Potion",
            stack = ItemBuilder
                .of(Material.POTION)
                .data(16394)
        )

        buttons[15 + 18] = buildPurchasable(
            amount = 1250,
            name = "Invisibility Potion",
            stack = ItemBuilder
                .of(Material.POTION)
                .data(16398)
        )

        buttons[10] = buildPurchasable(
            amount = 10,
            name = "Speed II Potion",
            stack = ItemBuilder
                .of(Material.POTION)
                .data(8226)
        )

        return buttons
    }
}
