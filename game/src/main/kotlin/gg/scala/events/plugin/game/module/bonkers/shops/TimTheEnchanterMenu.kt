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

            when (armorSlot)
            {
                ArmorSlot.HELMET ->
                {
                    val helmet = player.inventory.helmet

                    if (helmet == null || helmet.type == Material.AIR)
                    {
                        player.sendMessage("${CC.RED}You do not have an armor piece equipped in this slot!")
                        return@toButton
                    }

                    helmet.addEnchantment(enchantment, level)
                }

                ArmorSlot.CHESTPLATE ->
                {
                    val chestplate = player.inventory.chestplate

                    if (chestplate == null || chestplate.type == Material.AIR)
                    {
                        player.sendMessage("${CC.RED}You do not have an armor piece equipped in this slot!")
                        return@toButton
                    }

                    chestplate.addEnchantment(enchantment, level)
                }


                ArmorSlot.LEGGINGS ->
                {
                    val leggings = player.inventory.chestplate

                    if (leggings == null || leggings.type == Material.AIR)
                    {
                        player.sendMessage("${CC.RED}You do not have an armor piece equipped in this slot!")
                        return@toButton
                    }

                    leggings.addEnchantment(enchantment, level)
                }

                ArmorSlot.BOOTS ->
                {
                    val boots = player.inventory.chestplate

                    if (boots == null || boots.type == Material.AIR)
                    {
                        player.sendMessage("${CC.RED}You do not have an armor piece equipped in this slot!")
                        return@toButton
                    }

                    boots.addEnchantment(enchantment, level)
                }
            }

            player.account().balance -= amount
            player.updateInventory()

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
            ArmorSlot.HELMET
        )

        buttons[11 + 9] = buildPurchasable(
            amount = 275,
            name = "Protection I",
            stack = ItemBuilder.of(Material.DIAMOND_CHESTPLATE).build(),
            enchantment = Enchantment.PROTECTION_ENVIRONMENTAL,
            level = 1,
            ArmorSlot.CHESTPLATE
        )

        buttons[11 + 18] = buildPurchasable(
            amount = 250,
            name = "Protection I",
            stack = ItemBuilder.of(Material.DIAMOND_LEGGINGS).build(),
            enchantment = Enchantment.PROTECTION_ENVIRONMENTAL,
            level = 1,
            ArmorSlot.LEGGINGS
        )

        buttons[11 + 27] = buildPurchasable(
            amount = 125,
            name = "Protection I",
            stack = ItemBuilder.of(Material.DIAMOND_BOOTS).build(),
            enchantment = Enchantment.PROTECTION_ENVIRONMENTAL,
            level = 1,
            ArmorSlot.BOOTS
        )

        return buttons
    }

    enum class ArmorSlot
    {
        HELMET, CHESTPLATE, LEGGINGS, BOOTS
    }
}