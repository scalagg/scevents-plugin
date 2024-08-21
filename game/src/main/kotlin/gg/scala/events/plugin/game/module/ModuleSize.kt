package gg.scala.events.plugin.game.module

/**
 * @author TastyCake
 * @date 8/22/2024
 */
enum class ModuleSize(
    var playersEachTeam: Int,
    var text: String
) {
    ONE_VS_ONE(1, ""),
    TWO_VS_TWO(2, "2v2"),
    FFA(-1, "FFA"); // Def good idea for some gamemodes
}