package gg.scala.events.plugin.game.module.bonkers

import gg.scala.cgs.common.CgsGameEngine
import gg.scala.cgs.common.player.handler.CgsGameDisqualificationHandler
import gg.scala.cgs.common.states.CgsGameState
import gg.scala.cgs.common.teams.CgsGameTeamService
import gg.scala.commons.tablist.TablistService
import gg.scala.events.plugin.game.ScalaEventsGamePlugin
import gg.scala.events.plugin.game.module.EventBind
import gg.scala.events.plugin.game.module.EventModule
import gg.scala.events.plugin.game.module.bonkers.commands.BalanceCommand
import gg.scala.events.plugin.game.module.bonkers.commands.PayCommand
import gg.scala.events.plugin.game.module.bonkers.commands.TagMeCommand
import gg.scala.events.plugin.game.module.bonkers.menus.SelectTeamMenu
import gg.scala.events.plugin.game.module.bonkers.shops.CarpenterMenu
import gg.scala.events.plugin.game.module.bonkers.shops.SellOreMenu
import gg.scala.events.plugin.game.module.bonkers.shops.ShopMenu
import gg.scala.events.plugin.game.module.bonkers.shops.TimTheEnchanterMenu
import gg.scala.events.plugin.game.module.bonkers.teams.GameTeamService
import gg.scala.events.plugin.game.module.bonkers.teams.commands.TeamChatCommand
import gg.scala.events.plugin.game.module.bonkers.teams.commands.TeamCommands
import gg.scala.events.plugin.game.module.bonkers.teams.commands.TeamInvitesCommand
import gg.scala.events.plugin.game.module.bonkers.teams.timers.CombatTimer
import gg.scala.events.plugin.game.module.bonkers.timers.BonkersRespawnTimer
import gg.scala.events.plugin.game.module.bonkers.visual.PleaseKillMeNowTabPopulator
import gg.scala.events.shared.type.EventType
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Service
import gg.scala.flavor.service.ignore.IgnoreAutoScan
import me.lucko.helper.Commands
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import net.evilblock.cubed.entity.EntityHandler
import net.evilblock.cubed.entity.hologram.HologramEntity
import net.evilblock.cubed.entity.hologram.HologramHandler
import net.evilblock.cubed.entity.hologram.command.HologramCommand
import net.evilblock.cubed.entity.npc.NpcEntity
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.EventUtils
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.bukkit.Tasks.delayed
import net.evilblock.cubed.util.bukkit.cuboid.Cuboid
import org.bukkit.*
import org.bukkit.block.Chest
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.DoubleChestInventory
import org.bukkit.inventory.ItemStack
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
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
            Bukkit.getWorld("Bunkers"), x, y, z
        )

    val mappings = mutableMapOf(
        "${CC.BLUE}Blue" to TeamData(
            teamId = 1,
            color = ChatColor.BLUE,
            teamName = "Blue",
            spawnLocation = loc(500.500, 53.0, 322.500),
            region = Cuboid(
                loc(454.500, 0.0, 301.500),
                loc(546.500, 144.0, 351.500)
            )
        ),
        "${CC.RED}Red" to TeamData(
            teamId = 2,
            color = ChatColor.RED,
            teamName = "Red",
            spawnLocation = loc(500.500, 53.0, 678.500),
            region = Cuboid(
                loc(546.500, 0.0, 649.500),
                loc(454.500, 144.0, 699.500)
            )
        ),
        "${CC.GREEN}Green" to TeamData(
            teamId = 3,
            color = ChatColor.GREEN,
            teamName = "Green",
            spawnLocation = loc(678.500, 53.0, 500.500),
            region = Cuboid(
                loc(699.500, 0.0, 454.500),
                loc(649.500, 144.0, 546.500)
            )
        ),
        "${CC.YELLOW}Yellow" to TeamData(
            teamId = 4,
            color = ChatColor.YELLOW,
            teamName = "Yellow",
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
    val conquests = mutableMapOf(
        "${CC.BLUE}North ${CC.YELLOW}West " to Cuboid(
            loc(385.0, 46.0, 358.0),
            loc(372.0, 60.0, 372.0)
        ),
        "${CC.BLUE}North ${CC.GREEN}East " to Cuboid(
            loc(642.0, 46.0, 372.0),
            loc(628.0, 60.0, 358.0)
        ),
        "${CC.RED}South ${CC.YELLOW}West " to Cuboid(
            loc(358.0, 46.0, 628.0),
            loc(372.0, 60.0, 642.0)
        ),
        "${CC.RED}South ${CC.GREEN}East " to Cuboid(
            loc(628.0, 46.0, 642.0),
            loc(642.0, 60.0, 628.0)
        ),
    )
    var currentConquest: ConquestKothMix? = null
    var currentCentralKoth: ConquestKothMix? = null

    fun startNewConquestEvent(region: Cuboid, name: String)
    {
        ConquestKothMix(region, active = true, 60 * 5, name = name, startNewOneAfter = true).configure()
    }

    private var spawnCampInvincibility = false

    private fun createGracePeriodRunner(duels: Boolean = true) =
        createRunner(
            // TODO: change back to 60
            seconds = 5 * 4,
            end = {
                if (!duels)
                {
                    conquests.entries.random()
                        .apply {
                            startNewConquestEvent(value, key)
                        }
                }
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

    private fun createCentralKothCreationRunner() =
        createRunner(
            seconds = 60 * 10,
            end = {
                ConquestKothMix(
                    Cuboid(
                        loc(495.0, 65.0, 495.0),
                        loc(505.0, 60.0, 505.0),
                    ),
                    active = true,
                    60 * 8,
                    startNewOneAfter = false,
                    name = "${CC.BLUE}Classic ",
                    specialCentral = true
                ).configure()
            }
        )

    val claims = mutableMapOf<String, Cuboid>()

    override fun configure()
    {
        plugin.logger.info("I've gone bonkers! Send help! (locali9zation/messages parker influneced)")

        this.mappings.forEach {
            claims[it.key + "_base"] = it.value.region
            claims[it.key + "_mine"] = it.value.region
        }

        TablistService.register(PleaseKillMeNowTabPopulator)

        claims["central_mine"] = Cuboid(
            loc(559.500, 0.0, 559.500),
            loc(441.500, 144.0, 441.500)
        )

//        listOf(
//            TeamChatCommand,
//            TeamCommands,
//            TeamInvitesCommand,
//            TagMeCommand,
//            BalanceCommand,
//            PayCommand
//        ).forEach {
//            plugin.flavor().inject(it)
//            plugin.commandManager.registerCommand(it)
//        }

        plugin.flavor().inject(GameTeamService)
        GameTeamService.configureTeamResources()

        Events
            .subscribe(PlayerJoinEvent::class.java)
            .handler {
                playerAccounts[it.player.uniqueId] = Account()

                if (CgsGameEngine.INSTANCE.gameState != CgsGameState.STARTED)
                {
                    delayed(1L) {
                        SelectTeamMenu().openMenu(it.player)
                    }
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
            .filter {
                !it.player.hasMetadata("spectator")
            }
            .handler {
                val claim = it.block.findClaim()

                if (claim == null)
                {
                    it.isCancelled = true
                    return@handler
                }

                val central = it.block
                    .findClaimContextual("central")

                if (central == null)
                {
                    it.isCancelled = true
                    return@handler
                }

                if (it.player.location.y > 42)
                {
                    it.isCancelled = true
                }
            }

        Events
            .subscribe(BlockBreakEvent::class.java, EventPriority.HIGHEST)
            .filter {
                it.player.location.y < 42 && !it.player.hasMetadata("spectator")
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
                    if (it.player.team()?.id != mapping.teamId && !mapping.raidable())
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

//                it.player.sendMessage("${ChatColor.LIGHT_PURPLE}YAS QUEEN! U SLAYED. GO GET UR ${original.name} BESTIEEEE")

                delayed(6 * 20L) {
                    it.block.type = original
//                    it.player.sendMessage("${ChatColor.GRAY}.... c'mere bestie i got more for uuu ")
                }
            }

        Events
            .subscribe(BlockBreakEvent::class.java, EventPriority.HIGHEST)
            .filter {
                it.block != null && !it.player.hasMetadata("spectator")
            }
            .handler {
                val claim = it.block
                    .findClaimContextual("base")

                if (claim == null)
                {
                    it.isCancelled = true
                    return@handler
                }

                val mapping = this.mappings[claim.key.split("_")[0]]

                if (mapping != null)
                {
                    if (it.player.team()?.id != mapping.teamId && !mapping.raidable())
                    {
                        it.player.sendMessage("${CC.RED}You cannot do this in enemy territory!")
                        it.isCancelled = true
                        return@handler
                    }
                }
            }

        Events
            .subscribe(BlockPlaceEvent::class.java, EventPriority.HIGHEST)
            .filter {
                it.block != null && !it.player.hasMetadata("spectator")
            }
            .handler {
                val claim = it.block
                    .findClaimContextual("base")

                if (claim == null)
                {
                    val central = it.block
                        .findClaimContextual("central")

                    if (central == null)
                    {
                        it.isCancelled = true
                        return@handler
                    }

                    if (it.player.location.y < 42)
                    {
                        it.player.sendMessage("${CC.RED}You cannot do this in the mine!")
                    }

                    it.isCancelled = true
                    return@handler
                }

                val mapping = this.mappings[claim.key.split("_")[0]]

                if (mapping != null)
                {
                    if (it.player.team()?.id != mapping.teamId && !mapping.raidable())
                    {
                        it.player.sendMessage("${CC.RED}You cannot do this in enemy territory!")
                        it.isCancelled = true
                    } else
                    {
                        if (it.player.location.y < 42 && it.blockPlaced.type.name.contains("FENCE"))
                        {
                            val teamData = it.player.team()?.data()
                                ?: return@handler

                            teamData.activeHolograms.entries
                                .firstOrNull { entry -> entry.value.withinDestroyDistance(it.blockPlaced.location) }
                                .apply {
                                    if (this != null)
                                    {
                                        teamData.activeHolograms.remove(this.key)
                                        it.isCancelled = false

                                        EntityHandler.forgetEntity(this.value)
                                        EntityHandler.close()
                                    }
                                }
                        }
                    }

                    return@handler
                }
            }


        Events
            .subscribe(PlayerInteractEvent::class.java, EventPriority.MONITOR)
            .filter { it.clickedBlock != null && (it.item == null || it.item.type != Material.ENDER_PEARL) && !it.player.hasMetadata("spectator") }
            .handler {
                val claim = it.clickedBlock
                    .findClaimContextual("base")
                    ?: return@handler

                val mapping = this.mappings[claim.key.split("_")[0]]
                    ?: return@handler run {
                        it.isCancelled = true
                        it.player.sendMessage("${CC.RED}You cannot do this in ${claim.key.split("_")[0]} territory!")
                    }

                if (it.clickedBlock.type.name.contains("CHEST"))
                {
                    if (it.player.team()?.id != mapping.teamId && !mapping.raidable())
                    {
                        it.player.sendMessage("${CC.RED}You cannot do this in enemy territory!")
                        it.isCancelled = true
                        return@handler
                    }

                    val teamData = it.player.team()?.data() ?: return@handler
                    val blockState = it.clickedBlock.state

                    if (blockState is Chest)
                    {
                        val chestInventory = blockState.inventory

                        if (chestInventory is DoubleChestInventory)
                        {
                            val doubleChest = chestInventory.holder

                            if (it.player.isSneaking && it.action == Action.RIGHT_CLICK_BLOCK)
                            {
                                val balance = it.player.account().balance

                                if (teamData.ownedChests.contains(doubleChest.location))
                                {
                                    it.player.sendMessage("${CC.RED}You have already purchased this chest!")
                                    it.isCancelled = true
                                    return@handler
                                }

                                if (balance < 400)
                                {
                                    it.player.sendMessage("${CC.RED}You do not have enough to purchase this chest! You need ${CC.YELLOW}$${400 - balance} ${CC.RED}more!")
                                    it.isCancelled = true
                                    return@handler
                                }

                                it.player.account().balance -= 400
                                it.player.sendMessage("${CC.GREEN}You have just purchased this chest for your team!")
                                it.player.playSound(
                                    it.player.location,
                                    Sound.CHEST_OPEN,
                                    1.0f, 1.0f
                                )

                                teamData.ownedChests.add(doubleChest.location)
                                it.isCancelled = true
                                return@handler
                            } else
                            {
                                if (!teamData.ownedChests.contains(doubleChest.location))
                                {
                                    it.player.sendMessage("${CC.RED}Your team has not unlocked this chest! ${CC.YELLOW}Shift-Right-Click ${CC.RED}in order to unlock it.")
                                    it.isCancelled = true
                                    return@handler
                                } else
                                {
                                    it.isCancelled = false
                                }
                            }
                        }
                    }
                }

                if (it.clickedBlock.type.name.contains("FENCE") || it.clickedBlock.type.name.contains("DOOR") || it.clickedBlock.type.name.contains(
                        "PLATE"
                    )
                )
                {
                    if (it.player.team()?.id != mapping.teamId && !mapping.raidable())
                    {
                        it.player.sendMessage("${CC.RED}You cannot do this in enemy territory!")
                        it.isCancelled = true
                        return@handler
                    }
                }

                it.isCancelled = false
            }

        fun handleGenericMovementEvent(it: GenericMovementEvent)
        {
            val claim = it.to.block.findClaim()
            val oldClaim = it.from.block.findClaim()

            if (oldClaim == null && claim == null)
            {
                return
            }

            if (claim == null && oldClaim != null)
            {
                it.player.sendMessage(
                    if (oldClaim.key.equals("central_mine")) "${CC.YELLOW}You exited: ${CC.GREEN}${
                        oldClaim.key.replace(
                            "central_mine",
                            "Classic"
                        )
                    }" else "${CC.YELLOW}You exited: ${CC.GREEN}${oldClaim.key.replace("_base", "")}"
                )
                it.player.sendMessage("${CC.YELLOW}You entered: ${CC.RED}Warzone")
//                it.player.playSound(it.player.location, Sound.NOTE_PLING, 0.5f, 0.3f)
            }

            if (claim != null && oldClaim == null)
            {
                it.player.sendMessage("${CC.YELLOW}You exited: ${CC.RED}Warzone")
                it.player.sendMessage(
                    if (claim.key.equals("central_mine")) "${CC.YELLOW}You entered: ${CC.GREEN}${
                        claim.key.replace(
                            "central_mine",
                            "Classic"
                        )
                    }" else "${CC.YELLOW}You entered: ${CC.GREEN}${claim.key.replace("_base", "")}"
                )
//                it.player.playSound(it.player.location, Sound.NOTE_PLING, 0.5f, 1.2f)
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
            .subscribe(PlayerItemConsumeEvent::class.java)
            .handler { event ->
                val player = event.player

                val item = event.item
                val antidote = ItemBuilder
                    .of(Material.POTION)
                    .name("${CC.GREEN}Antidote")
                    .data(8196)
                    .build()

                if (item.isSimilar(antidote))
                {
                    val preventedEffectTypes = listOf(
                        PotionEffectType.POISON,
                        PotionEffectType.SLOW
                    )

                    preventedEffectTypes.forEach {
                        if (player.hasPotionEffect(it))
                        {
                            player.removePotionEffect(it)
                        }
                    }

                    player.setMetadata("antidote", FixedMetadataValue(plugin, true)).apply {
                        object : BukkitRunnable()
                        {
                            override fun run()
                            {

                                if (!player.hasMetadata("antidote"))
                                {
                                    cancel()
                                } else
                                {
                                    preventedEffectTypes.forEach {
                                        if (player.hasPotionEffect(it))
                                        {
                                            player.removePotionEffect(it)
                                        }
                                    }
                                }
                            }
                        }.runTaskTimer(plugin, 0L, 20L)
                    }

                    createRunner(8,
                        end = {
                            player.sendMessage("${CC.YELLOW}Your ${CC.GREEN}Antidote ${CC.YELLOW}has expired!")
                            player.removeMetadata("antidote", plugin)
                        })

                    player.sendMessage("${CC.YELLOW}You have just consumed an ${CC.GREEN}Antidote${CC.YELLOW}.")
                }
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
                it.participant.spigot().respawn()

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
                                    playSound(location, Sound.ENDERDRAGON_GROWL, 1.0f, 1.0f)
                                }
                            }
                        }

                        if (data.raidable() && previouslyRaidable)
                        {
                            CgsGameDisqualificationHandler.disqualifyPlayer(
                                player = it.participant,
                                broadcastNotification = true, setSpectator = true
                            )
                            return@apply
                        }

                        if (!data.raidable())
                        {
                            BonkersRespawnTimer(it.participant)
                                .runTaskTimer(
                                    CgsGameEngine.INSTANCE.plugin, 0L, 20L
                                )
                        }

                        if (CombatTimer.isActive(it.participant))
                        {
                            CombatTimer.reset(it.participant)
                        }

                        val deathLocation = it.deathLocation

                        deathLocation.world.strikeLightningEffect(deathLocation)
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
                createCentralKothCreationRunner()

                CgsGameTeamService.teams
                    .forEach { (_, team) ->
                        val it = this.mappings.entries
                            .firstOrNull {
                                it.value.teamId == team.id
                            }
                            ?: return@forEach

                        fun buildActionEntity(
                            action: String,
                            location: Location,
                            actionLambda: (Player) -> Unit
                        )
                        {
                            val entity = object : NpcEntity(
                                lines = listOf(
                                    "${CC.GREEN}$action",
                                    "${CC.GRAY}Click to open menu!"
                                ),
                                location = location
                            )
                            {
                                override fun onRightClick(player: Player)
                                {
                                    if (player.hasMetadata("spectator"))
                                    {
                                        return
                                    }

                                    actionLambda(player)
                                }
                            }
                            entity.initializeData()

                            EntityHandler.trackEntity(entity)
                        }

                        buildActionEntity(
                            action = "Sell your ore",
                            location = it.value.spawnLocation
                        ) {
                            SellOreMenu().openMenu(it)
                        }

                        buildActionEntity(
                            action = "Shop",
                            location = it.value.spawnLocation
                                .clone()
                                .subtract(1.0, 0.0, 1.0)
                        ) {
                            ShopMenu().openMenu(it)
                        }

                        buildActionEntity(
                            action = "Tim The Enchanter",
                            location = it.value.spawnLocation
                                .clone()
                                .add(1.0, 0.0, 1.0)
                        ) {
                            TimTheEnchanterMenu().openMenu(it)
                        }

                        buildActionEntity(
                            action = "Carpenter",
                            location = it.value.spawnLocation
                                .clone()
                                .add(1.0, 0.0, 0.0)
                        ) {
                            CarpenterMenu().openMenu(it)
                        }

                        listOf(
                            HologramEntity(
                                text = "&e&lWARNING! &cBlock Up Area",
                                location = it.value.spawnLocation
                                    .clone()
                                    .subtract(13.0, 24.0, -46.0)
                            ),
                            HologramEntity(
                                text = "&e&lWARNING! &cBlock Up Area",
                                location = it.value.spawnLocation
                                    .clone()
                                    .subtract(13.0, 24.0, 46.0)
                            )
                        ).forEach { hologram ->
                            hologram.initializeData()
                            it.value.activeHolograms[hologram.location] = hologram

                            EntityHandler.trackEntity(hologram)
                            EntityHandler.close()
                        }

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
