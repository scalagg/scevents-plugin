package gg.scala.events.plugin.game.module.bonkers.teams

import com.google.common.cache.CacheBuilder
import gg.scala.cgs.common.player.handler.CgsPlayerHandler
import gg.scala.cgs.common.teams.CgsGameTeamService
import gg.scala.events.plugin.game.ScalaEventsGamePlugin
import gg.scala.events.plugin.game.module.bonkers.data
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Service
import gg.scala.lemon.channel.ChatChannelBuilder
import gg.scala.lemon.channel.ChatChannelService
import gg.scala.events.plugin.game.module.bonkers.teams.channel.TeamChatChannelComposite
import gg.scala.lemon.channel.channels.DefaultChatChannel
import me.lucko.helper.utils.Players
import net.evilblock.cubed.util.CC
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * @author GrowlyX
 * @since 4/26/2023
 */
@Service
object GameTeamService
{
    @Inject
    lateinit var plugin: ScalaEventsGamePlugin

    val teamInvites = CacheBuilder.newBuilder()
        .expireAfterWrite(30L, TimeUnit.SECONDS)
        .build<UUID, Pair<Int, UUID>>()

    fun configureTeamResources()
    {
        CgsGameTeamService.teams.clear()

        DefaultChatChannel.provideAdditionalPrefix {
            LegacyComponentSerializer
                .legacySection()
                .deserialize("${CC.GRAY}[${it.team!!.data()!!.colored()}${CC.GRAY}] ")
        }

        for (id in 1..((Bukkit.getMaxPlayers() / gameType.teamSize) + 1))
        {
            CgsGameTeamService.teams[id] =
                CgsGameTeamService.engine.createTeam(id)

            val channel = ChatChannelBuilder.newBuilder()
                .import(TeamChatChannelComposite)
                .compose()
                .monitor()
                .displayToPlayer { player, player2 ->
                    player2.team?.id == id && player.team?.id == id
                }
                .override(11) {
                    it.hasMetadata("teamchat") && it.team?.id == id
                }

            ChatChannelService.register(channel)
        }

        Players.all()
            .forEach {
                CgsGameTeamService
                    .allocatePlayersToAvailableTeam(
                        player = CgsPlayerHandler.find(it)!!
                    )
            }
    }
}
