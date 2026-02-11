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
 * for better visual quality and lower network overhead. The border is rendered as:
 * <ul>
 *   <li>12 edge lines forming the cuboid wireframe (4 bottom edges, 4 top edges, 4 vertical edges)</li>
 *   <li>4 horizontal lines at the player's Y level (front, back, left, right) if player is within bounds</li>
 * </ul>
 * The border automatically disappears after {@link #DISPLAY_DURATION_TICKS} ticks.
 */
public class BorderRenderUtil {

    /** How long the border display remains visible, in ticks (20 ticks = 1 second). */
    private static final long DISPLAY_DURATION_TICKS = 200L; // 10 seconds

    /** Thickness of each border wall in blocks. */
    private static final float WALL_THICKNESS = 0.08f;

    /** Material used for border walls. */
    private static final Material BORDER_MATERIAL = Material.WHITE_STAINED_GLASS;

    public static final Color DEFAULT_GLOW_COLOR = Color.fromRGB(0, 180, 255);

    public static void showBorder(CommandSender sender, DominionDTO dominion) {
        if (!(sender instanceof Player player)) {
            return;
        }
        showBorder(player,
                dominion.getWorld(),
                dominion.getCuboid(),
                Color.fromRGB(dominion.getColorR(), dominion.getColorG(), dominion.getColorB())
        );
    }

    public static void showBorder(Player player, DominionDTO dominion) {
        showBorder(player,
                dominion.getWorld(),
                dominion.getCuboid(),
                Color.fromRGB(dominion.getColorR(), dominion.getColorG(), dominion.getColorB())
        );
    }

    public static void showBorder(Player player, World world, CuboidDTO cuboid, Color GLOW_COLOR) {
        Scheduler.runTask(() -> showBorderDisplay(player, world, cuboid, GLOW_COLOR));
    }

    /**
     * Creates and displays a holographic border around the specified cuboid region for the given player.
     * Any previously displayed border for this player is removed first. The border auto-removes
     * after {@link #DISPLAY_DURATION_TICKS} ticks.
     */
    private static void showBorderDisplay(Player player, World world, CuboidDTO cuboid, Color GLOW_COLOR) {
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

        // ===== 12条边框线 =====
        
        // 底部4条边 (y1层)
        border.addBlockDisplay("bottom_north", BORDER_MATERIAL)
                .offset(0, 0, 0)
                .scale(sizeX, WALL_THICKNESS, WALL_THICKNESS)
                .brightness(15, 15)
                .glowColor(GLOW_COLOR);

        border.addBlockDisplay("bottom_south", BORDER_MATERIAL)
                .offset(0, 0, sizeZ)
                .scale(sizeX, WALL_THICKNESS, WALL_THICKNESS)
                .brightness(15, 15)
                .glowColor(GLOW_COLOR);

        border.addBlockDisplay("bottom_west", BORDER_MATERIAL)
                .offset(0, 0, 0)
                .scale(WALL_THICKNESS, WALL_THICKNESS, sizeZ)
                .brightness(15, 15)
                .glowColor(GLOW_COLOR);

        border.addBlockDisplay("bottom_east", BORDER_MATERIAL)
                .offset(sizeX, 0, 0)
                .scale(WALL_THICKNESS, WALL_THICKNESS, sizeZ)
                .brightness(15, 15)
                .glowColor(GLOW_COLOR);

        // 顶部4条边 (y2层)
        border.addBlockDisplay("top_north", BORDER_MATERIAL)
                .offset(0, sizeY, 0)
                .scale(sizeX, WALL_THICKNESS, WALL_THICKNESS)
                .brightness(15, 15)
                .glowColor(GLOW_COLOR);

        border.addBlockDisplay("top_south", BORDER_MATERIAL)
                .offset(0, sizeY, sizeZ)
                .scale(sizeX, WALL_THICKNESS, WALL_THICKNESS)
                .brightness(15, 15)
                .glowColor(GLOW_COLOR);

        border.addBlockDisplay("top_west", BORDER_MATERIAL)
                .offset(0, sizeY, 0)
                .scale(WALL_THICKNESS, WALL_THICKNESS, sizeZ)
                .brightness(15, 15)
                .glowColor(GLOW_COLOR);

        border.addBlockDisplay("top_east", BORDER_MATERIAL)
                .offset(sizeX, sizeY, 0)
                .scale(WALL_THICKNESS, WALL_THICKNESS, sizeZ)
                .brightness(15, 15)
                .glowColor(GLOW_COLOR);

        // 4条竖直边
        border.addBlockDisplay("vertical_nw", BORDER_MATERIAL)
                .offset(0, 0, 0)
                .scale(WALL_THICKNESS, sizeY, WALL_THICKNESS)
                .brightness(15, 15)
                .glowColor(GLOW_COLOR);

        border.addBlockDisplay("vertical_ne", BORDER_MATERIAL)
                .offset(sizeX, 0, 0)
                .scale(WALL_THICKNESS, sizeY, WALL_THICKNESS)
                .brightness(15, 15)
                .glowColor(GLOW_COLOR);

        border.addBlockDisplay("vertical_sw", BORDER_MATERIAL)
                .offset(0, 0, sizeZ)
                .scale(WALL_THICKNESS, sizeY, WALL_THICKNESS)
                .brightness(15, 15)
                .glowColor(GLOW_COLOR);

        border.addBlockDisplay("vertical_se", BORDER_MATERIAL)
                .offset(sizeX, 0, sizeZ)
                .scale(WALL_THICKNESS, sizeY, WALL_THICKNESS)
                .brightness(15, 15)
                .glowColor(GLOW_COLOR);

        // ===== 玩家高度的水平线（前后左右四条）=====
        double playerY = player.getLocation().getY();
        if (playerY >= y1 && playerY <= y2) {
            float playerOffsetY = (float) (playerY - y1);

            border.addBlockDisplay("player_north", BORDER_MATERIAL)
                    .offset(0, playerOffsetY, 0)
                    .scale(sizeX, WALL_THICKNESS, WALL_THICKNESS)
                    .brightness(15, 15)
                    .glowColor(GLOW_COLOR);

            border.addBlockDisplay("player_south", BORDER_MATERIAL)
                    .offset(0, playerOffsetY, sizeZ)
                    .scale(sizeX, WALL_THICKNESS, WALL_THICKNESS)
                    .brightness(15, 15)
                    .glowColor(GLOW_COLOR);

            border.addBlockDisplay("player_west", BORDER_MATERIAL)
                    .offset(0, playerOffsetY, 0)
                    .scale(WALL_THICKNESS, WALL_THICKNESS, sizeZ)
                    .brightness(15, 15)
                    .glowColor(GLOW_COLOR);

            border.addBlockDisplay("player_east", BORDER_MATERIAL)
                    .offset(sizeX, playerOffsetY, 0)
                    .scale(WALL_THICKNESS, WALL_THICKNESS, sizeZ)
                    .brightness(15, 15)
                    .glowColor(GLOW_COLOR);
        }

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
