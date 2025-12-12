package cn.lunadeer.dominion.utils.highlighter;

import cn.lunadeer.dominion.api.dtos.CuboidDTO;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.utils.Misc;
import cn.lunadeer.dominion.utils.XLogger;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Manager for highlighter implementations.
 * Automatically selects the best available highlighter for each player.
 */
public class HighlighterManager {

    private static HighlighterManager instance;
    private final List<Highlighter> highlighters = new ArrayList<>();
    private final Plugin plugin;

    public HighlighterManager(Plugin plugin) {
        instance = this;
        this.plugin = plugin;
        registerHighlighters();
    }

    private void registerHighlighters() {
        // Always register particle highlighter (works on all servers)
        highlighters.add(new ParticleHighlighter());

        // Register BlockDisplay highlighter only on Paper servers
        if (Misc.isPaper()) {
            try {
                highlighters.add(new BlockDisplayHighlighter(plugin));
                XLogger.info("BlockDisplay highlighter registered (Paper detected)");
            } catch (Exception e) {
                XLogger.warn("Failed to register BlockDisplay highlighter: {0}", e.getMessage());
            }
        }

        // Sort by priority (highest first)
        highlighters.sort(Comparator.comparingInt(Highlighter::getPriority).reversed());
    }

    /**
     * Get the best available highlighter for a player.
     *
     * @param player the player
     * @return the best highlighter, or null if none available
     */
    public Highlighter getHighlighter(Player player) {
        String preferredType = Configuration.highlighter.type.toUpperCase();

        // If a specific type is configured, try to use it
        if (!preferredType.equals("AUTO")) {
            for (Highlighter highlighter : highlighters) {
                if (highlighter.getName().equalsIgnoreCase(preferredType) && highlighter.canUse(player)) {
                    return highlighter;
                }
            }
        }

        // Auto-select: use the highest priority highlighter that can be used
        for (Highlighter highlighter : highlighters) {
            if (highlighter.canUse(player)) {
                return highlighter;
            }
        }

        return null;
    }

    /**
     * Show the border of a dominion to a player using the best available highlighter.
     *
     * @param player   the player
     * @param dominion the dominion
     */
    public static void showBorder(Player player, DominionDTO dominion) {
        if (instance == null) return;
        Highlighter highlighter = instance.getHighlighter(player);
        if (highlighter != null) {
            highlighter.showBorder(player, dominion);
        }
    }

    /**
     * Show a cuboid border to a player using the best available highlighter.
     *
     * @param player the player
     * @param world  the world
     * @param cuboid the cuboid
     */
    public static void showBorder(Player player, World world, CuboidDTO cuboid) {
        if (instance == null) return;
        Highlighter highlighter = instance.getHighlighter(player);
        if (highlighter != null) {
            highlighter.showBorder(player, world, cuboid);
        }
    }

    /**
     * Stop highlighting for a player.
     *
     * @param player the player
     */
    public static void stopHighlighting(Player player) {
        if (instance == null) return;
        for (Highlighter highlighter : instance.highlighters) {
            highlighter.stopHighlighting(player);
        }
    }

    public static HighlighterManager getInstance() {
        return instance;
    }
}