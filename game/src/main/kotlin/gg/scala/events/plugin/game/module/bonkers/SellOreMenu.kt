package gg.scala.events.plugin.game.module.bonkers

import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.apache.commons.lang3.text.WordUtils
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player

/**
 * @author GrowlyX
 * @since 4/9/2023
 */
class SellOreMenu : Menu("Sell your ores")
{
    init
    {
        placeholder = true
        updateAfterClick = true
    }

    private val mappings = mutableMapOf(
        Material.COAL to 10,
        Material.IRON_INGOT to 20,
        Material.GOLD_INGOT to 40,
        Material.DIAMOND to 50,
        Material.NETHER_STAR to 400,
    )

    override fun getButtons(player: Player): Map<Int, Button>
    {
        var index = 2
        val buttons = mutableMapOf<Int, Button>()

        for (mapping in mappings)
        {
            val materialName = if (mapping.key == Material.NETHER_STAR)
                "Shard" else WordUtils
                .capitalizeFully(
                    mapping.key.name
                        .replace("_", " ")
                        .lowercase()
                )

            val totalAmount = player.inventory
                .filter { it != null && it.type == mapping.key }
                .sumOf { it.amount }

            buttons[index++] = ItemBuilder
                .of(mapping.key)
                .name(
                    "${CC.GREEN}Sell $materialName"
                )
                .addToLore(
                    "${CC.B_GRAY}Left-click:",
                    " ${CC.GRAY}Sell 1 for ${CC.WHITE}$${mapping.value}",
                    "",
                    "${CC.B_GRAY}Right-click:",
                    " ${CC.GRAY}Sell $totalAmount for ${CC.WHITE}$${
                        mapping.value * totalAmount
                    }"
                )
                .toButton { _, type ->
                    if (type!!.isRightClick)
                    {
                        if (totalAmount <= 0)
                        {
                            player.sendMessage("${CC.RED}You do not have enough of this material to bulk deposit!")
                            return@toButton
                        }

                        player.inventory.removeAmount(mapping.key, totalAmount)
                        player.account().balance += mapping.value * totalAmount

                        player.sendMessage("${CC.GREEN}You deposited all of your ${materialName}s and gained $${mapping.value * totalAmount}!")
                        player.playSound(player.location, Sound.NOTE_PLING, 0.5f, 0.2f)
                    } else if (type.isLeftClick)
                    {
                        if (totalAmount < 1)
                        {
                            player.sendMessage("${CC.RED}You do not have enough of this material to deposit!")
                            return@toButton
                        }

                        player.inventory.removeAmount(mapping.key, 1)
                        player.account().balance += mapping.value

                        player.sendMessage("${CC.GREEN}You deposited one $materialName and gained $${mapping.value}!")
                        player.playSound(player.location, Sound.NOTE_PLING, 0.5f, 0.2f)
                    }
                }
        }

        return buttons
    }
}
