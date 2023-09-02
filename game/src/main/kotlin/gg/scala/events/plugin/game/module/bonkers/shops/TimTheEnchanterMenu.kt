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
        armorSlot: ArmorSlot,
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

            val item = armorSlot
                .fromPlayerInventory(player.inventory)
                ?.clone()

            if (item == null || item.type == Material.AIR)
            {
                player.sendMessage("${CC.RED}You do not have your ${armorSlot.name.lowercase()} equipped!")
                return@toButton
            }

            item.addEnchantment(enchantment, level)

            armorSlot.updatePlayerInventory(player.inventory, item)
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
            ArmorSlot.Hat
        )

        buttons[11 + 9] = buildPurchasable(
            amount = 275,
            name = "Protection I",
            stack = ItemBuilder.of(Material.DIAMOND_CHESTPLATE).build(),
            enchantment = Enchantment.PROTECTION_ENVIRONMENTAL,
            level = 1,
            ArmorSlot.Shirt
        )

        buttons[11 + 18] = buildPurchasable(
            amount = 250,
            name = "Protection I",
            stack = ItemBuilder.of(Material.DIAMOND_LEGGINGS).build(),
            enchantment = Enchantment.PROTECTION_ENVIRONMENTAL,
            level = 1,
            ArmorSlot.Jeans
        )

        buttons[11 + 27] = buildPurchasable(
            amount = 125,
            name = "Protection I",
            stack = ItemBuilder.of(Material.DIAMOND_BOOTS).build(),
            enchantment = Enchantment.PROTECTION_ENVIRONMENTAL,
            level = 1,
            ArmorSlot.Shoes
        )

        return buttons
    }

    enum class ArmorSlot(
        val fromPlayerInventory: (PlayerInventory) -> ItemStack?,
        val updatePlayerInventory: (PlayerInventory, ItemStack) -> Unit,
    )
    {
        Hat(
            PlayerInventory::getHelmet,
            PlayerInventory::setHelmet
        ),
        Shirt(
            PlayerInventory::getChestplate,
            PlayerInventory::setChestplate
        ),
        Jeans(
            PlayerInventory::getLeggings,
            PlayerInventory::setLeggings
        ),
        Shoes(
            PlayerInventory::getBoots,
            PlayerInventory::setBoots
        )
    }
}
