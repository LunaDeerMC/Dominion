package cn.lunadeer.dominion.utils;

import cn.lunadeer.dominion.api.dtos.CuboidDTO;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.utils.holograme.HoloItem;
import cn.lunadeer.dominion.utils.holograme.HoloManager;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Utility class for displaying dominion borders using client-side BlockDisplay holograms.
 * <p>
 * Replaces the previous particle-based implementation with NMS packet-based display entities
 * for better visual quality and lower network overhead. Each border is shown as 4 thin
 * semi-transparent glass walls that automatically disappear after {@link #DISPLAY_DURATION_TICKS} ticks.
 */
public class ParticleUtil {

    /** How long the border display remains visible, in ticks (20 ticks = 1 second). */
    private static final long DISPLAY_DURATION_TICKS = 200L; // 10 seconds

    /** Thickness of each border wall in blocks. */
    private static final float WALL_THICKNESS = 0.01f;

    /** Material used for border walls. */
    private static final Material BORDER_MATERIAL = Material.LIGHT_BLUE_STAINED_GLASS;

    /** Glow color for border walls. */
    private static final Color GLOW_COLOR = Color.fromRGB(0, 180, 255);

    public static void showBorder(CommandSender sender, DominionDTO dominion) {
        if (!(sender instanceof Player player)) {
            return;
        }
        showBorder(player,
                dominion.getWorld(),
                dominion.getCuboid()
        );
    }

    public static void showBorder(Player player, DominionDTO dominion) {
        showBorder(player,
                dominion.getWorld(),
                dominion.getCuboid()
        );
    }

    public static void showBorder(Player player, World world, CuboidDTO cuboid) {
        Scheduler.runTask(() -> showBorderDisplay(player, world, cuboid));
    }

    /**
     * Creates and displays a holographic border around the specified cuboid region for the given player.
     * Any previously displayed border for this player is removed first. The border auto-removes
     * after {@link #DISPLAY_DURATION_TICKS} ticks.
     */
    private static void showBorderDisplay(Player player, World world, CuboidDTO cuboid) {
        if (player == null || !player.isOnline() || world == null) return;

        String holoName = "border_" + player.getUniqueId();

        // Remove any existing border display for this player
        if (HoloManager.instance().exists(holoName)) {
            HoloManager.instance().remove(holoName);
        }

        int x1 = cuboid.x1(), y1 = cuboid.y1(), z1 = cuboid.z1();
        int x2 = cuboid.x2(), y2 = cuboid.y2(), z2 = cuboid.z2();

        float sizeX = x2 - x1;
        float sizeY = y2 - y1;
        float sizeZ = z2 - z1;

        if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) return;

        Location anchor = new Location(world, x1, y1, z1);
        HoloItem border = HoloManager.instance().create(holoName, anchor);

        // North wall (min Z face) — spans X and Y at z1
        border.addBlockDisplay("north", BORDER_MATERIAL)
                .offset(0, 0, 0)
                .scale(sizeX, sizeY, WALL_THICKNESS)
                .brightness(15, 15)
                .glowColor(GLOW_COLOR);

        // South wall (max Z face) — spans X and Y at z2
        border.addBlockDisplay("south", BORDER_MATERIAL)
                .offset(0, 0, sizeZ)
                .scale(sizeX, sizeY, WALL_THICKNESS)
                .brightness(15, 15)
                .glowColor(GLOW_COLOR);

        // West wall (min X face) — spans Z and Y at x1
        border.addBlockDisplay("west", BORDER_MATERIAL)
                .offset(0, 0, 0)
                .scale(WALL_THICKNESS, sizeY, sizeZ)
                .brightness(15, 15)
                .glowColor(GLOW_COLOR);

        // East wall (max X face) — spans Z and Y at x2
        border.addBlockDisplay("east", BORDER_MATERIAL)
                .offset(sizeX, 0, 0)
                .scale(WALL_THICKNESS, sizeY, sizeZ)
                .brightness(15, 15)
                .glowColor(GLOW_COLOR);

        // Only show to the requesting player (client-side only)
        border.show(player);

        // Auto-remove after the display duration
        Scheduler.runTaskLater(() -> {
            if (HoloManager.instance().exists(holoName)) {
                HoloManager.instance().remove(holoName);
            }
        }, DISPLAY_DURATION_TICKS);
    }

}
