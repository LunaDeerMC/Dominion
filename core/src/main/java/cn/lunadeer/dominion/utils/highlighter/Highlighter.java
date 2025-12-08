package cn.lunadeer.dominion.utils.highlighter;

import cn.lunadeer.dominion.api.dtos.CuboidDTO;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Interface for highlighting dominion borders to players.
 * Different implementations can use different methods (particles, block displays, etc.)
 */
public interface Highlighter {

    /**
     * Show the border of a dominion to a player.
     *
     * @param player   the player to show the border to
     * @param dominion the dominion whose border to show
     */
    void showBorder(Player player, DominionDTO dominion);

    /**
     * Show a cuboid border to a player.
     *
     * @param player the player to show the border to
     * @param world  the world the cuboid is in
     * @param cuboid the cuboid to show
     */
    void showBorder(Player player, World world, CuboidDTO cuboid);

    /**
     * Stop highlighting for a player (cleanup any entities/effects).
     *
     * @param player the player to stop highlighting for
     */
    void stopHighlighting(Player player);

    /**
     * Check if this highlighter can be used for the given player.
     * Some highlighters may not work for certain players (e.g., Bedrock players).
     *
     * @param player the player to check
     * @return true if this highlighter can be used for the player
     */
    boolean canUse(Player player);

    /**
     * Get the priority of this highlighter.
     * Higher priority highlighters are preferred when multiple are available.
     *
     * @return the priority (higher = more preferred)
     */
    int getPriority();

    /**
     * Get the name of this highlighter type.
     *
     * @return the name
     */
    String getName();
}