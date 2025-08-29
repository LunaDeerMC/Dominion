package cn.lunadeer.dominion.managers;

import cn.lunadeer.dominion.api.dtos.CuboidDTO;
import cn.lunadeer.dominion.hooks.PlaceholderAPIHook;
import cn.lunadeer.dominion.hooks.WorldGuardHook;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class HooksManager {

    public HooksManager(JavaPlugin plugin) {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderAPIHook(plugin);
        }
    }

    /**
     * Set placeholder for the message.
     * <p>
     * Use this method instead of PlaceholderAPI directly to avoid not installed PlaceholderAPI
     * throwing NoClassDefFoundError.
     *
     * @param player  the player
     * @param message the message
     * @return the message with placeholder
     */
    public static String setPlaceholder(Player player, String message) {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return PlaceholderAPIHook.setPlaceholders(player, message);
        } else {
            return message;
        }
    }

    /**
     * Checks if the specified cuboid conflicts with any WorldGuard regions in the given world.
     * <p>
     * This method first verifies if the WorldGuard plugin is enabled. If it is, it delegates
     * the conflict check to the WorldGuardProvider. If WorldGuard is not enabled, it returns false.
     *
     * @param cuboid the cuboid to check for conflicts
     * @param world  the world in which to check for conflicts
     * @return true if there is a conflict with WorldGuard regions, false otherwise
     */
    public static boolean isConflictWithWorldGuard(@NotNull CuboidDTO cuboid, @NotNull World world) {
        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            return WorldGuardHook.isConflict(cuboid, world);
        } else {
            return false;
        }
    }
}
