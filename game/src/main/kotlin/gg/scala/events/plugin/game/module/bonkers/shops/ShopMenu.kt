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
        purchaseStack: Boolean = false,
        stackQuantity: Int = 1,
        fillInventory: Boolean = false,
        grant: ((Player) -> Unit)? = null
    ) = ItemBuilder
        .copyOf(stack)
        .name("${CC.YELLOW}$name")
        .addToLore(
            "${CC.GRAY}Price: ${CC.WHITE}$${
                Numbers.format(amount)
            }",
            ""
        )
        .apply {
            if (purchaseStack)
            {
                this.addToLore(
                    "${CC.YELLOW}Right-Click to purchase ${stackQuantity}x"
                )
            } else if (fillInventory)
            {
                this.addToLore(
                    "${CC.YELLOW}Right-Click to fill your inventory!"
                )
            }
        }.addToLore(
            "${CC.GREEN}Left-Click to purchase!"
        )
        .toButton { player, clickType ->
            val balance = player!!.account().balance

            if (balance < amount)
            {
                player.sendMessage("${CC.RED}You don't have enough money for this!")
                return@toButton
            }

            if (clickType!!.isRightClick)
            {
                if (fillInventory)
                {
                    var inventoryIndex = 0
                    var amountPurchased = 1

                    while (balance >= (amount * amountPurchased) && inventoryIndex < 36)
                    {
                        val item = player.inventory.contents[inventoryIndex]

                        if (item == null || item.type == Material.AIR)
                        {
                            amountPurchased++
                            player.account().balance -= amount

                            player.inventory.setItem(inventoryIndex, stack.build())
                        }

                        inventoryIndex++
                    }

                    player.updateInventory()
                    player.sendMessage(
                        "${CC.GREEN}You purchased ${amountPurchased-1}x $name for ${CC.WHITE}$${amount * (amountPurchased - 1)}${CC.GREEN}!"
                    )

                    return@toButton
                } else
                {
                    val newAmount = (amount * stackQuantity)

                    if (balance < newAmount)
                    {
                        player.sendMessage("${CC.RED}You don't have enough money for this!")
                        return@toButton
                    }

                    player.inventory.addItem(
                        ItemBuilder.copyOf(stack.build())
                            .amount(stackQuantity)
                            .build()
                    )
                    player.account().balance -= newAmount

                    player.updateInventory()
                    player.sendMessage(
                        "${CC.GREEN}You purchased ${stackQuantity}x $name for ${CC.WHITE}$${newAmount}${CC.GREEN}!"
                    )

                    return@toButton
                }
            } else
            {
                if (grant == null)
                {
                    player.inventory.addItem(stack.build())
                } else
                {
                    grant(player)
                }
            }

            player.account().balance -= amount

            player.updateInventory()
            player.sendMessage(
                "${CC.GREEN}You purchased 1x $name for ${CC.WHITE}$${amount}${CC.GREEN}!"
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
            for ((i, piece) in it.inventory.armorContents.withIndex())
            {
                if (piece != null && piece.type != Material.AIR)
                {
                    it.inventory.armorContents[i] = null
                    it.inventory.addItem(piece)
                }
            }

            it.inventory.helmet = ItemStack(Material.DIAMOND_HELMET)
            it.inventory.chestplate = ItemStack(Material.DIAMOND_CHESTPLATE)
            it.inventory.leggings = ItemStack(Material.DIAMOND_LEGGINGS)
            it.inventory.boots = ItemStack(Material.DIAMOND_BOOTS)
        }

        buttons[12 + 9] = buildPurchasable(
            amount = 25,
            name = "Enderpearl",
            stack = ItemBuilder.of(Material.ENDER_PEARL),
            stackQuantity = 16,
            purchaseStack = true
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
                .amount(16),
            stackQuantity = 64,
            purchaseStack = true
        )

        buttons[15 + 9] = buildPurchasable(
            amount = 5,
            name = "Health Potion",
            stack = ItemBuilder
                .of(Material.POTION)
                .data(16421),
            fillInventory = true
        )

        buttons[16 + 9] = buildPurchasable(
            amount = 50,
            name = "Poison Potion",
            stack = ItemBuilder
                .of(Material.POTION)
                .data(16388),
            fillInventory = true
        )

        buttons[22] = buildPurchasable(
            amount = 100,
            name = "Antidote",
            stack = ItemBuilder
                .of(Material.POTION)
                .name("${CC.GREEN}Antidote")
                .data(8196)
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
