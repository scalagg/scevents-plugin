package gg.scala.events.plugin.host.command

import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.lemon.Lemon
import gg.scala.lemon.util.QuickAccess
import gg.scala.lemon.util.QuickAccess.sendGlobalFancyBroadcast
import gg.scala.commons.acf.BaseCommand
import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandPermission
import gg.scala.commons.acf.annotation.Optional
import gg.scala.events.plugin.host.service.HostEventService
import gg.scala.flavor.inject.Inject
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import net.evilblock.cubed.util.bukkit.FancyMessage
import net.md_5.bungee.api.chat.ClickEvent
import org.apache.commons.lang3.time.DurationFormatUtils
import org.bukkit.entity.Player
import java.util.concurrent.TimeUnit

@AutoRegister
object HostAnnounceCommand : ScalaCommand()
{
    @JvmStatic
    val COOL_DOWN = TimeUnit.SECONDS.toMillis(45L)

    @Inject
    lateinit var hostEventService: HostEventService

    private var lastAnnouncement = -1L
    private var lastAnnouncementCreator = ""

    @CommandAlias("hostannounce|heannounce|announce")
    @CommandPermission("events.host.announce")
    fun onHostAnnounce(player: Player, @Optional message: String?)
    {
        // Only allow hosts to announce
        if (!hostEventService.isHost(player))
        {
            throw ConditionFailedException("Only the event host can announce the event!")
        }

        // Check cooldown (except for ops)
        if (lastAnnouncement + COOL_DOWN > System.currentTimeMillis() && !player.isOp)
        {
            throw ConditionFailedException("You must wait ${
                DurationFormatUtils.formatDurationWords(
                    lastAnnouncement + COOL_DOWN - System.currentTimeMillis(), true, true
                )
            } before announcing again as $lastAnnouncementCreator${CC.RED} already announced recently.")
        }

        lastAnnouncement = System.currentTimeMillis()
        lastAnnouncementCreator = QuickAccess.coloredName(player) ?: "${CC.D_RED}Console"

        val eventName = hostEventService.getEventName()
        val alivePlayers = hostEventService.getAlivePlayers().size
        val totalPlayers = hostEventService.getDeadPlayers().size + alivePlayers
        val serverId = Lemon.instance.settings.id

        // Custom message or default
        val customMessage = message?.let { " - ${CC.WHITE}$it" } ?: ""

        val fancyMessage = FancyMessage()
        fancyMessage.withMessage(
            "${CC.B_GOLD}Custom Event ${CC.B_GRAY}${Constants.DOUBLE_ARROW_RIGHT} ${CC.WHITE}$eventName${CC.YELLOW} hosted by $lastAnnouncementCreator${CC.YELLOW} on ${CC.GREEN}$serverId${CC.YELLOW}! ${CC.GRAY}($alivePlayers alive, $totalPlayers total)$customMessage ${CC.GREEN}(Join)"
        )
        
        fancyMessage.andHoverOf(
            "${CC.GREEN}Click to join ${CC.WHITE}$eventName${CC.GREEN}!",
            "",
            "${CC.YELLOW}Event: ${CC.WHITE}$eventName",
            "${CC.YELLOW}Host: $lastAnnouncementCreator",
            "${CC.YELLOW}Server: ${CC.WHITE}$serverId",
            "${CC.YELLOW}Players: ${CC.WHITE}$alivePlayers alive, $totalPlayers total",
            "",
            "${CC.B_RED}Warning:",
            "${CC.RED}This will switch your server!"
        )
        
        fancyMessage.andCommandOf(
            ClickEvent.Action.RUN_COMMAND,
            "/join $serverId"
        )

        sendGlobalFancyBroadcast(
            fancyMessage = fancyMessage,
            permission = null
        )

        player.sendMessage("${CC.GREEN}Event announced network-wide! Players can now join your event.")
        
        val broadcastMessage = if (message != null) {
            "Network announcement: $message"
        } else {
            "Event announced network-wide - players incoming!"
        }
        hostEventService.announceEvent(broadcastMessage)
    }

    @CommandAlias("hostannounce|heannounce") 
    @CommandPermission("events.host.announce")
    fun onHostAnnounceHelp(player: Player)
    {
        player.sendMessage("")
        player.sendMessage("${CC.GOLD}${CC.BOLD}Host Event Announcement")
        player.sendMessage("${CC.GRAY}Promote your custom event across the network!")
        player.sendMessage("")
        player.sendMessage("${CC.YELLOW}/hostannounce ${CC.GRAY}- Announce your event")
        player.sendMessage("${CC.YELLOW}/hostannounce <message> ${CC.GRAY}- Announce with custom message")
        player.sendMessage("")
        player.sendMessage("${CC.GRAY}Cooldown: ${CC.WHITE}45 seconds")
        player.sendMessage("${CC.GRAY}Permission: ${CC.WHITE}Only event hosts")
        player.sendMessage("")
    }
} 