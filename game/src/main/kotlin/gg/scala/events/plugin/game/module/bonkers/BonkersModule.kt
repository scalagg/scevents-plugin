package gg.scala.events.plugin.game.module.bonkers

import gg.scala.cgs.common.CgsGameEngine
import gg.scala.cgs.common.player.handler.CgsSpectatorHandler
import gg.scala.cgs.common.states.CgsGameState
import gg.scala.cgs.common.teams.CgsGameTeamService
import gg.scala.events.plugin.game.ScalaEventsGamePlugin
import gg.scala.events.plugin.game.module.EventBind
import gg.scala.events.plugin.game.module.EventModule
import gg.scala.events.shared.type.EventType
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Service
import gg.scala.flavor.service.ignore.IgnoreAutoScan
import me.lucko.helper.Commands
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import net.evilblock.cubed.entity.EntityHandler
import net.evilblock.cubed.entity.npc.NpcEntity
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.EventUtils
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.bukkit.Tasks.delayed
import net.evilblock.cubed.util.bukkit.cuboid.Cuboid
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.inventory.ItemStack
import java.util.*

/**
 * @author GrowlyX
 * @since 3/31/2023
 */
@Service
@IgnoreAutoScan
@EventBind(EventType.Bonkers::class)
object BonkersModule : EventModule
{
    @Inject
    lateinit var plugin: ScalaEventsGamePlugin

    private fun loc(x: Double, y: Double, z: Double) =
        Location(
            Bukkit.getWorld("world"), x, y, z
        )

    val mappings = mutableMapOf(
        "Blue" to TeamData(
            teamId = 1,
            color = ChatColor.BLUE,
            spawnLocation = loc(500.500, 53.0, 322.500),
            region = Cuboid(
                loc(454.500, 0.0, 301.500),
                loc(546.500, 144.0, 351.500)
            )
        ),
        "Red" to TeamData(
            teamId = 2,
            color = ChatColor.RED,
            spawnLocation = loc(500.500, 53.0, 678.500),
            region = Cuboid(
                loc(546.500, 0.0, 649.500),
                loc(454.500, 144.0, 699.500)
            )
        ),
        "Green" to TeamData(
            teamId = 3,
            color = ChatColor.GREEN,
            spawnLocation = loc(678.500, 53.0, 500.500),
            region = Cuboid(
                loc(699.500, 0.0, 454.500),
                loc(649.500, 144.0, 546.500)
            )
        ),
        "Yellow" to TeamData(
            teamId = 4,
            color = ChatColor.YELLOW,
            spawnLocation = loc(322.500, 53.0, 500.500),
            region = Cuboid(
                loc(301.500, 0.0, 546.500),
                loc(351.500, 144.0, 454.500)
            )
        )
    )

    override val gameSnapshot = GameSnapshot
    override val nametagProvider = NametagAdapter
    override val type = EventType.Bonkers
    override val scoreboardFormatter = ScoreboardRenderer
    override val visibilityAdapter = VisibilityAdapter

    val playerAccounts = mutableMapOf<UUID, Account>()
    val conquests = mutableListOf(
        Cuboid(
            loc(385.0, 46.0, 358.0),
            loc(372.0, 60.0, 372.0)
        ),
        Cuboid(
            loc(642.0, 46.0, 372.0),
            loc(628.0, 60.0, 358.0)
        ),
        Cuboid(
            loc(358.0, 46.0, 628.0),
            loc(372.0, 60.0, 642.0)
        ),
        Cuboid(
            loc(628.0, 46.0, 642.0),
            loc(642.0, 60.0, 628.0)
        )
    )

    fun startNewConquestEvent(region: Cuboid)
    {
        ConquestKothMix(region, 60 * 5).configure()
    }

    private var spawnCampInvincibility = false

    private fun createGracePeriodRunner() =
        createRunner(
            seconds = 60 * 4,
            end = {
                startNewConquestEvent(conquests.random())
            },
            update = {
            }
        )

    private fun createSpawnCampInvincibilityRunner() =
        createRunner(
            seconds = 15,
            end = {
                spawnCampInvincibility = true
                Bukkit.broadcastMessage("${CC.GRAY}You can now take damage.")
            },
            update = {

            }
        )

    val claims = mutableMapOf<String, Cuboid>()

    override fun configure()
    {
        this.mappings.forEach {
            claims[it.key + "_base"] = it.value.region
            claims[it.key + "_mine"] = it.value.region
        }

        claims["central_mine"] = Cuboid(
            loc(559.500, 0.0, 559.500),
            loc(441.500, 144.0, 441.500)
        )

        Events
            .subscribe(PlayerJoinEvent::class.java)
            .handler {
                playerAccounts[it.player.uniqueId] = Account()

                delayed(1L) {
                    SelectTeamMenu().openMenu(it.player)
                }
            }

        Events
            .subscribe(EntityDamageEvent::class.java)
            .filter {
                !spawnCampInvincibility
            }
            .handler {
                it.isCancelled = true
            }

        Events
            .subscribe(PlayerQuitEvent::class.java)
            .handler {
                playerAccounts.remove(it.player.uniqueId)
            }

        val mappings = mutableMapOf(
            Material.GOLD_ORE to Material.GOLD_INGOT,
            Material.DIAMOND_ORE to Material.DIAMOND,
            Material.COAL_ORE to Material.COAL,
            Material.IRON_ORE to Material.IRON_INGOT,
            Material.STONE to Material.COBBLESTONE
        )

        Events
            .subscribe(BlockBreakEvent::class.java, EventPriority.HIGH)
            .handler {
                val claim = it.player.findClaim()

                if (claim == null)
                {
                    it.isCancelled = true
                    return@handler
                }

                val central = it.block
                    .findClaimContextual("central")
                    ?: return@handler

                if (it.player.location.y > 42)
                {
                    it.isCancelled = true
                }
            }

        Events
            .subscribe(BlockBreakEvent::class.java, EventPriority.HIGHEST)
            .filter {
                it.player.location.y < 42
            }
            .handler {
                val claim = it.block.findClaimContextual("mine")

                if (claim == null)
                {
                    it.isCancelled = true
                    return@handler
                }

                val mapping = this.mappings[claim.key.split("_")[0]]

                if (mapping != null)
                {
                    if (it.player.team()?.id != mapping.teamId)
                    {
                        it.isCancelled = true
                        return@handler
                    }
                }

                if (mappings[it.block.type] == null)
                {
                    it.isCancelled = true
                    return@handler
                }

                val original = it.block.type
                it.isCancelled = true
                it.block.type = Material.COBBLESTONE

                it.player.inventory.addItem(
                    ItemBuilder
                        .of(mappings[original]!!)
                        .build()
                )

                delayed(6 * 20L) {
                    it.block.type = original
                }
            }

        Events
            .subscribe(PlayerInteractEvent::class.java, EventPriority.HIGHEST)
            .filter { it.clickedBlock != null }
            .handler {
                val claim = it.clickedBlock
                    .findClaimContextual("base")
                    ?: return@handler

                val mapping = this.mappings[claim.key.split("_")[0]]
                    ?: return@handler run {
                        it.isCancelled = true
                        it.player.sendMessage("${CC.RED}You cannot do this in ${claim.key.split("_")[0]} territory!")
                    }

                if (it.player.team()?.id != mapping.teamId)
                {
                    it.player.sendMessage("${CC.RED}You cannot do this in enemy territory!")
                    it.isCancelled = true
                    return@handler
                }

                it.isCancelled = false
            }

        fun handleGenericMovementEvent(it: GenericMovementEvent)
        {
            val claim = it.player.findClaimWithName()
            val oldClaim = it.from.block.findClaim()

            if (oldClaim == null && claim == null)
            {
                return
            }

            if (claim == null && oldClaim != null)
            {
                it.player.sendMessage("${CC.YELLOW}You exited: ${CC.RED}${oldClaim.key}")
                it.player.playSound(it.player.location, Sound.NOTE_PLING, 0.5f, 0.3f)
            }

            if (claim != null && oldClaim == null)
            {
                it.player.sendMessage("${CC.YELLOW}You entered: ${CC.GREEN}${claim.key}")
                it.player.playSound(it.player.location, Sound.NOTE_PLING, 0.5f, 1.2f)
            }
        }

        Events
            .subscribe(PlayerMoveEvent::class.java)
            .filter(EventUtils::hasPlayerMoved)
            .handler {
                handleGenericMovementEvent(
                    GenericMovementEvent(player = it.player, to = it.to, from = it.from)
                )
            }

        Events
            .subscribe(PlayerTeleportEvent::class.java)
            .handler {
                handleGenericMovementEvent(
                    GenericMovementEvent(player = it.player, to = it.to, from = it.from)
                )
            }

        Events
            .subscribe(CgsGameEngine.CgsGameParticipantDeathEvent::class.java)
            .handler {
                it.participant.team()
                    ?.apply {
                        val data = this.data()!!
                        val oldDtr = data.dtr

                        val previouslyRaidable = data.raidable()
                        data.dtr -= 1.0

                        this.participants.forEach { id ->
                            Bukkit.getPlayer(id)?.apply {
                                sendMessage("${CC.RED}Teammate Death: ${CC.WHITE}${it.participant.name}")
                                sendMessage("${CC.YELLOW}DTR: ${CC.WHITE}$oldDtr -> ${data.dtr}")

                                if (data.raidable() && !previouslyRaidable)
                                {
                                    sendMessage("${CC.D_RED}BITCH U GONE RAIDABLE! SAVE YOURSELF!")
                                    playSound(location, Sound.ENDERDRAGON_GROWL, 1.0f, 1.0f)
                                }
                            }
                        }

                        delayed(1L) {
                            if (data.raidable() && previouslyRaidable)
                            {
                                CgsSpectatorHandler.setSpectator(it.participant, true)
                                return@delayed
                            }

                            it.participant.teleport(data.spawnLocation)
                        }
                    }

                it.killer?.apply {
                    account().balance += 250
                    sendMessage(
                        "${CC.GOLD}You earned $250 for killing ${it.participant.colorfulName()}${CC.GOLD}!"
                    )
                }
            }

        Events
            .subscribe(CgsGameEngine.CgsGameStartEvent::class.java)
            .handler {
                createGracePeriodRunner()
                createSpawnCampInvincibilityRunner()

                CgsGameTeamService.teams
                    .forEach { (_, team) ->
                        val it = this.mappings.entries
                            .firstOrNull {
                                it.value.teamId == team.id
                            }
                            ?: return@forEach

                        val entity = object : NpcEntity(
                            lines = listOf(
                                "${CC.GREEN}Sell your items",
                                "${CC.GRAY}Click to open menu!"
                            ),
                            location = it.value.spawnLocation
                        )
                        {
                            override fun onRightClick(player: Player) =
                                SellOreMenu().openMenu(player)
                        }

                        entity.initializeData()
                        EntityHandler.trackEntity(entity)

                        team.participants.forEach { player ->
                            val bukkitPlayer = Bukkit.getPlayer(player)
                                ?: return@forEach

                            bukkitPlayer
                                .teleport(it.value.spawnLocation)

                            bukkitPlayer.inventory
                                .addItem(
                                    ItemStack(Material.STONE_PICKAXE),
                                    ItemStack(Material.STONE_AXE)
                                )
                        }
                    }

                Schedulers.async()
                    .runRepeating({ _ ->
                        playerAccounts
                            .forEach { (_, account) ->
                                account.balance += 3
                            }
                    }, 0L, 60L)
            }

        Commands
            .create()
            .assertPlayer()
            .handler {
                if (CgsGameEngine.INSTANCE.gameState.isAfter(CgsGameState.STARTED))
                {
                    it.sender().sendMessage("${CC.RED}You cannot switch teams right now!")
                    return@handler
                }

                SelectTeamMenu().openMenu(it.sender())
            }
            .register("selectteam")
    }
}
