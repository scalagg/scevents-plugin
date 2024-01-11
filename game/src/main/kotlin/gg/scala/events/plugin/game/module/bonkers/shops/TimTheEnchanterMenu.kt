package gg.scala.events.plugin.game.module.bonkers.shops

import gg.scala.events.plugin.game.module.bonkers.account
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.math.Numbers
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory

/**
 * @author GrowlyX
 * @since 4/9/2023
 */
class TimTheEnchanterMenu : Menu("Tim the Enchanter")
{
    init
    {
        placeholder = true
    }

    override fun size(buttons: Map<Int, Button>) = 54

    private fun buildPurchasable(
        amount: Int,
        name: String,
        stack: ItemStack,
        enchantment: Enchantment,
        level: Int,
        whiteGirlFitCheck: WhiteGirlFitCheck,
    ) = ItemBuilder
        .copyOf(stack)
        .name("${CC.YELLOW}$name")
        .addToLore(
            "${CC.GRAY}Price: ${CC.WHITE}$${
                Numbers.format(amount)
            }",
            "",
            "${CC.GREEN}Click to add to your item!"
        )
        .toButton { player, _ ->
            val balance = player!!.account().balance

            if (balance < amount)
            {
                player.sendMessage("${CC.RED}You don't have enough money for this!")
                return@toButton
            }

            val item = whiteGirlFitCheck
                .fromPlayerInventory(player.inventory)
                ?.clone()

            if (item == null || item.type == Material.AIR)
            {
                player.sendMessage("${CC.RED}You do not have your ${whiteGirlFitCheck.name.lowercase()} equipped!")
                return@toButton
            }

            if (item.containsEnchantment(enchantment))
            {
                player.sendMessage("${CC.RED}You already have this enchantment equipped on your ${whiteGirlFitCheck.name.lowercase()}!")
                return@toButton
            }

            if (whiteGirlFitCheck == WhiteGirlFitCheck.Sword)
            {
                if (item.type != Material.DIAMOND_SWORD)
                {
                    player.sendMessage("${CC.RED}You must be holding a sword in order to enchant it!")
                    return@toButton
                }
            }

            item.addEnchantment(enchantment, level)

            whiteGirlFitCheck.updatePlayerInventory(player.inventory, item)
            player.updateInventory()

            player.account().balance -= amount

            player.sendMessage(
                "${CC.GREEN}You purchased the $name enchant for ${CC.WHITE}$$amount${CC.GREEN}!"
            )
        }

    override fun getButtons(player: Player): Map<Int, Button>
    {
        val buttons = mutableMapOf<Int, Button>()

        buttons[11] = buildPurchasable(
            amount = 225,
            name = "Protection I",
            stack = ItemBuilder.of(Material.DIAMOND_HELMET).build(),
            enchantment = Enchantment.PROTECTION_ENVIRONMENTAL,
            level = 1,
            WhiteGirlFitCheck.Helmet
        )

        buttons[11 + 9] = buildPurchasable(
            amount = 275,
            name = "Protection I",
            stack = ItemBuilder.of(Material.DIAMOND_CHESTPLATE).build(),
            enchantment = Enchantment.PROTECTION_ENVIRONMENTAL,
            level = 1,
            WhiteGirlFitCheck.Chestplate
        )

        buttons[11 + 18] = buildPurchasable(
            amount = 250,
            name = "Protection I",
            stack = ItemBuilder.of(Material.DIAMOND_LEGGINGS).build(),
            enchantment = Enchantment.PROTECTION_ENVIRONMENTAL,
            level = 1,
            WhiteGirlFitCheck.Leggings
        )

        buttons[11 + 27] = buildPurchasable(
            amount = 125,
            name = "Protection I",
            stack = ItemBuilder.of(Material.DIAMOND_BOOTS).build(),
            enchantment = Enchantment.PROTECTION_ENVIRONMENTAL,
            level = 1,
            WhiteGirlFitCheck.Boots
        )

        buttons[15] = buildPurchasable(
            amount = 200,
            name = "Unbreaking III",
            stack = ItemBuilder.of(Material.GOLD_HELMET).build(),
            enchantment = Enchantment.DURABILITY,
            level = 3,
            WhiteGirlFitCheck.Helmet
        )

        buttons[15 + 9] = buildPurchasable(
            amount = 225,
            name = "Unbreaking III",
            stack = ItemBuilder.of(Material.GOLD_CHESTPLATE).build(),
            enchantment = Enchantment.DURABILITY,
            level = 3,
            WhiteGirlFitCheck.Chestplate
        )

        buttons[15 + 18] = buildPurchasable(
            amount = 200,
            name = "Unbreaking III",
            stack = ItemBuilder.of(Material.GOLD_LEGGINGS).build(),
            enchantment = Enchantment.DURABILITY,
            level = 3,
            WhiteGirlFitCheck.Leggings
        )

        buttons[15 + 27] = buildPurchasable(
            amount = 115,
            name = "Unbreaking III",
            stack = ItemBuilder.of(Material.GOLD_BOOTS).build(),
            enchantment = Enchantment.DURABILITY,
            level = 3,
            WhiteGirlFitCheck.Boots
        )

        buttons[13] = buildPurchasable(
            amount = 300,
            name = "Sharpness I",
            stack = ItemBuilder.of(Material.DIAMOND_SWORD).build(),
            enchantment = Enchantment.DAMAGE_ALL,
            level = 1,
            WhiteGirlFitCheck.Sword
        )


        buttons[13 + 9] = buildPurchasable(
            amount = 125,
            name = "Feather Falling IV",
            stack = ItemBuilder.of(Material.FEATHER).build(),
            enchantment = Enchantment.PROTECTION_FALL,
            level = 4,
            WhiteGirlFitCheck.Boots
        )

        return buttons
    }

    enum class WhiteGirlFitCheck(
        val fromPlayerInventory: (PlayerInventory) -> ItemStack?,
        val updatePlayerInventory: (PlayerInventory, ItemStack) -> Unit,
    )
    {
        Helmet(
            PlayerInventory::getHelmet,
            PlayerInventory::setHelmet
        ),
        Chestplate(
            PlayerInventory::getChestplate,
            PlayerInventory::setChestplate
        ),
        Leggings(
            PlayerInventory::getLeggings,
            PlayerInventory::setLeggings
        ),
        Boots(
            PlayerInventory::getBoots,
            PlayerInventory::setBoots
        ),
        Sword(
            {
                it.contents.firstOrNull { stack -> stack != null && stack.type.name.contains("SWORD") }
            },
            { inventory, stack ->
                inventory.setItem(
                    inventory.contents
                        .indexOfFirst { other ->
                            other != null && other.type.name.contains("SWORD")
                        },
                    stack
                )
            }
        )
    }
}
