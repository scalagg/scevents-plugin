package gg.scala.events.plugin.game.module.bonkers.timers

import gg.scala.cgs.common.CgsGameEngine
import gg.scala.cgs.common.player.handler.CgsSpectatorHandler
import gg.scala.events.plugin.game.module.bonkers.data
import gg.scala.events.plugin.game.module.bonkers.team
import net.evilblock.cubed.util.CC
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.title.Title
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.scheduler.BukkitRunnable
import java.time.Duration

/**
 * @author GrowlyX
 * @since 4/12/2023
 */
class BonkersRespawnTimer(
    private val player: Player
) : BukkitRunnable()
{
    private var tick = 6

    init {
        player.setMetadata("respawning", FixedMetadataValue(CgsGameEngine.INSTANCE.plugin, true))
    }

    override fun run()
    {
        if (!this.player.isOnline)
        {
            this.cancel()
            return
        }

        val audience = CgsGameEngine.INSTANCE
            .audience.player(this.player)

        when (this.tick)
        {
            6 ->
            {
                CgsSpectatorHandler.setSpectator(this.player, temporary = true)

                this.player.teleport(
                    CgsGameEngine.INSTANCE.gameArena!!.getPreLobbyLocation()
                )
            }
            5, 4, 3, 2, 1 ->
            {
                audience.showTitle(
                    Title.title(
                        LegacyComponentSerializer.legacySection()
                            .deserialize(
                                "${CC.B_RED}YOU DIED"
                            ),
                        LegacyComponentSerializer.legacySection()
                            .deserialize(
                                "${CC.WHITE}You will respawn in ${CC.GREEN}$tick${CC.WHITE} seconds!"
                            ),
                        Title.Times.times(
                            Duration.ofMillis(0L),
                            Duration.ofMillis(1100L),
                            Duration.ofMillis(0L),
                        )
                    )
                )

                this.player.sendMessage(
                    "${CC.SEC}You will respawn in ${CC.GREEN}$tick${CC.SEC} seconds."
                )
                this.player.playSound(player.location, Sound.NOTE_STICKS, 1.0f, 1.0f)
            }
            0 ->
            {
                val team = this.player.team()?.data()
                    ?: return this.cancel()

                CgsSpectatorHandler
                    .removeSpectator(this.player)

                this.player.teleport(team.spawnLocation)

                audience.showTitle(
                    Title.title(
                        LegacyComponentSerializer.legacySection()
                            .deserialize(
                                "${CC.B_GREEN}RESPAWNED"
                            ),
                        LegacyComponentSerializer.legacySection()
                            .deserialize(
                                "${CC.WHITE}You have respawned!"
                            ),
                        Title.Times.times(
                            Duration.ofMillis(0L),
                            Duration.ofMillis(500L),
                            Duration.ofMillis(250L)
                        )
                    )
                )

                player.removeMetadata("respawning", CgsGameEngine.INSTANCE.plugin)
                this.player.sendMessage("${CC.GREEN}You have respawned!")
                this.player.playSound(player.location, Sound.NOTE_PLING, 1.0f, 1.0f)

                this.cancel()
            }
        }

        this.tick--
    }
}
