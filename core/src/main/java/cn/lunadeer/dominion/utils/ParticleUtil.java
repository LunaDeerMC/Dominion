package cn.lunadeer.dominion.utils;

import cn.lunadeer.dominion.api.dtos.CuboidDTO;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.utils.highlighter.HighlighterManager;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Utility class for showing dominion borders to players.
 * This class now delegates to the HighlighterManager which supports
 * multiple highlighting methods (particles, block displays, etc.)
 */
public class ParticleUtil {

    /**
     * Show the border of a dominion to a command sender.
     *
     * @param sender   the command sender (must be a player)
     * @param dominion the dominion whose border to show
     */
    public static void showBorder(CommandSender sender, DominionDTO dominion) {
        if (!(sender instanceof Player player)) {
            return;
        }
        HighlighterManager.showBorder(player, dominion);
    }

    /**
     * Show the border of a dominion to a player.
     *
     * @param player   the player to show the border to
     * @param dominion the dominion whose border to show
     */
    public static void showBorder(Player player, DominionDTO dominion) {
        HighlighterManager.showBorder(player, dominion);
    }

    /**
     * Show a cuboid border to a player.
     *
     * @param player the player to show the border to
     * @param world  the world the cuboid is in
     * @param cuboid the cuboid to show
     */
    public static void showBorder(Player player, World world, CuboidDTO cuboid) {
        HighlighterManager.showBorder(player, world, cuboid);
    }

    /**
     * Stop highlighting for a player.
     *
     * @param player the player to stop highlighting for
     */
    public static void stopHighlighting(Player player) {
        HighlighterManager.stopHighlighting(player);
    }
}
