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
    }

    private val mappings = mutableMapOf(
        Material.GOLD_INGOT to 40 to 3,
        Material.DIAMOND to 50 to 3,
        Material.COAL to 10 to 3,
        Material.IRON_INGOT to 20 to 25
    )

    override fun getButtons(player: Player): Map<Int, Button>
    {
        var index = 1
        val buttons = mutableMapOf<Int, Button>()

        for (mapping in mappings)
        {
            val materialName = WordUtils
                .capitalizeFully(
                    mapping.key.first.name
                        .replace("_", " ")
                        .lowercase()
                )

            buttons[index] = ItemBuilder
                .of(mapping.key.first)
                .name(
                    "${CC.GREEN}Sell $materialName"
                )
                .addToLore(
                    "${CC.B_GRAY}Left-click:",
                    " ${CC.GRAY}Sell 1 for ${CC.WHITE}$${mapping.key.second}",
                    "",
                    "${CC.B_GRAY}Right-click:",
                    " ${CC.GRAY}Sell ${mapping.value} for ${CC.WHITE}$${
                        mapping.key.second * mapping.value
                    }"
                )
                .toButton { _, type ->
                    val totalAmount = player.inventory
                        .filter { it != null && it.type == mapping.key.first }
                        .sumOf { it.amount }

                    if (type!!.isRightClick)
                    {
                        if (totalAmount < mapping.value)
                        {
                            player.sendMessage("${CC.RED}You do not have enough of this material to bulk deposit!")
                            return@toButton
                        }

                        player.inventory.removeAmount(mapping.key.first, mapping.value)
                        player.account().balance += mapping.key.second * mapping.value

                        player.sendMessage("${CC.GREEN}You deposited ${mapping.value} ${materialName}s and gained $${mapping.key.second * mapping.value}!")
                        player.playSound(player.location, Sound.NOTE_PLING, 0.5f, 0.2f)
                    } else if (type.isLeftClick)
                    {
                        if (totalAmount < 1)
                        {
                            player.sendMessage("${CC.RED}You do not have enough of this material to deposit!")
                            return@toButton
                        }

                        player.inventory.removeAmount(mapping.key.first, 1)
                        player.account().balance += mapping.key.second

                        player.sendMessage("${CC.GREEN}You deposited one $materialName and gained $${mapping.key.second}!")
                        player.playSound(player.location, Sound.NOTE_PLING, 0.5f, 0.2f)
                    }
                }

            index += 2
        }

        return buttons
    }
}
