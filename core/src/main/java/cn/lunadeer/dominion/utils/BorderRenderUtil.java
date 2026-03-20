package cn.lunadeer.dominion.utils;

import cn.lunadeer.dominion.api.dtos.CuboidDTO;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.utils.holograme.HoloItem;
import cn.lunadeer.dominion.utils.holograme.HoloManager;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class for displaying dominion borders. Two distinct display modes:
 * <ul>
 *   <li><b>Area display</b> ({@link #showAreaBorder}): Used when creating/editing dominions.
 *       Renders 6 semi-transparent faces forming a full cuboid using HoloItem BlockDisplays.</li>
 *   <li><b>Crossing effect</b> ({@link #showCrossingEffect}): Used when a player crosses a dominion border.
 *       Spawns particles that ripple outward from the crossing point along the border plane.</li>
 * </ul>
 */
public class BorderRenderUtil {

    /** How long the area border display remains visible, in ticks (20 ticks = 1 second). */
    private static final long DISPLAY_DURATION_TICKS = 200L; // 10 seconds

    /** Thickness of each face wall in blocks. */
    private static final float WALL_THICKNESS = 0.05f;

    /** Default number of particles for the crossing effect when config is invalid. */
    private static final int DEFAULT_CROSSING_PARTICLE_COUNT = 30;

    /** Speed of outward-moving particles along the border plane. */
    private static final double CROSSING_PARTICLE_SPEED = 0.35;

    public static final Color DEFAULT_GLOW_COLOR = Color.fromRGB(0, 180, 255);

    // ==================== Area Border (for creating/editing dominions) ====================

    /**
     * Show the area border for creating/editing dominions.
     * Renders a full cuboid of 6 semi-transparent faces using HoloItem BlockDisplays.
     */
    public static void showAreaBorder(CommandSender sender, DominionDTO dominion) {
        if (!(sender instanceof Player player)) {
            return;
        }
        showAreaBorder(player, dominion);
    }

    /**
     * Show the area border for creating/editing dominions.
     */
    public static void showAreaBorder(Player player, DominionDTO dominion) {
        showAreaBorder(player,
                dominion.getWorld(),
                dominion.getCuboid(),
                Color.fromRGB(dominion.getColorR(), dominion.getColorG(), dominion.getColorB())
        );
    }

    /**
     * Show the area border with custom color.
     */
    public static void showAreaBorder(Player player, World world, CuboidDTO cuboid, Color glowColor) {
        Scheduler.runTask(() -> showAreaBorderDisplay(player, world, cuboid, glowColor));
    }

    /**
     * Creates and displays a full cuboid area border around the specified region.
     * Uses 6 face walls (top, bottom, north, south, west, east) to form a closed box.
     */
    private static void showAreaBorderDisplay(Player player, World world, CuboidDTO cuboid, Color glowColor) {
        if (player == null || !player.isOnline() || world == null) return;

        String holoName = "area_border_" + player.getUniqueId();

        // Remove any existing area border display for this player
        if (HoloManager.instance().exists(holoName)) {
            HoloManager.instance().remove(holoName);
        }

        int x1 = cuboid.x1(), y1 = cuboid.y1(), z1 = cuboid.z1();
        int x2 = cuboid.x2(), y2 = cuboid.y2(), z2 = cuboid.z2();

        float sizeX = x2 - x1;
        float sizeY = y2 - y1;
        float sizeZ = z2 - z1;

        if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) return;

        Material borderMaterial;
        try {
            borderMaterial = Material.valueOf(Configuration.borderDisplay.borderBlockMaterial.toUpperCase());
        } catch (IllegalArgumentException e) {
            borderMaterial = Material.WHITE_STAINED_GLASS;
        }

        Location anchor = new Location(world, x1, y1, z1);
        HoloItem border = HoloManager.instance().create(holoName, anchor);

        // ===== 底面 (Y=y1) =====
        border.addBlockDisplay("face_bottom", borderMaterial)
                .offset(0, 0, 0)
                .scale(sizeX, WALL_THICKNESS, sizeZ)
                .brightness(15, 15);

        // ===== 顶面 (Y=y2) =====
        border.addBlockDisplay("face_top", borderMaterial)
                .offset(0, sizeY - WALL_THICKNESS, 0)
                .scale(sizeX, WALL_THICKNESS, sizeZ)
                .brightness(15, 15);

        // ===== 北面 (Z=z1) =====
        border.addBlockDisplay("face_north", borderMaterial)
                .offset(0, 0, 0)
                .scale(sizeX, sizeY, WALL_THICKNESS)
                .brightness(15, 15);

        // ===== 南面 (Z=z2) =====
        border.addBlockDisplay("face_south", borderMaterial)
                .offset(0, 0, sizeZ - WALL_THICKNESS)
                .scale(sizeX, sizeY, WALL_THICKNESS)
                .brightness(15, 15);

        // ===== 西面 (X=x1) =====
        border.addBlockDisplay("face_west", borderMaterial)
                .offset(0, 0, 0)
                .scale(WALL_THICKNESS, sizeY, sizeZ)
                .brightness(15, 15);

        // ===== 东面 (X=x2) =====
        border.addBlockDisplay("face_east", borderMaterial)
                .offset(sizeX - WALL_THICKNESS, 0, 0)
                .scale(WALL_THICKNESS, sizeY, sizeZ)
                .brightness(15, 15);

        // Show to the requesting player only (client-side)
        border.show(player);

        // Auto-remove after the display duration
        Scheduler.runTaskLater(() -> {
            if (HoloManager.instance().exists(holoName)) {
                HoloManager.instance().remove(holoName);
            }
        }, DISPLAY_DURATION_TICKS);
    }

    // ==================== Crossing Effect (for player crossing border) ====================

    /**
     * Show a particle effect at the point where the player crosses the dominion border.
     * Spawns particles at the crossing point with outward velocity vectors along the border
     * plane, creating a visible "spreading outward" motion effect.
     * <p>
     * Uses {@code count=0} mode where offX/offY/offZ act as a direction vector and
     * the extra parameter acts as speed, so each particle moves outward from the crossing
     * point along a random direction on the face plane.
     */
    public static void showCrossingEffect(Player player, DominionDTO dominion) {
        if (player == null || !player.isOnline() || dominion == null) return;
        if (dominion.getWorld() == null) return;

        Location playerLoc = player.getLocation();
        CuboidDTO cuboid = dominion.getCuboid();

        double px = playerLoc.getX();
        double py = playerLoc.getY();
        double pz = playerLoc.getZ();

        double distWest = Math.abs(px - cuboid.x1());
        double distEast = Math.abs(px - cuboid.x2());
        double distNorth = Math.abs(pz - cuboid.z1());
        double distSouth = Math.abs(pz - cuboid.z2());
        double distBottom = Math.abs(py - cuboid.y1());
        double distTop = Math.abs(py - cuboid.y2());

        // Find the closest face and determine which axis is perpendicular to it
        // faceAxis: 0 = X-perpendicular (west/east), 1 = Y-perpendicular (top/bottom), 2 = Z-perpendicular (north/south)
        double minDist = distWest;
        double spawnX = cuboid.x1(), spawnY = py, spawnZ = pz;
        int faceAxis = 0;

        if (distEast < minDist) {
            minDist = distEast;
            spawnX = cuboid.x2(); spawnY = py; spawnZ = pz;
            faceAxis = 0;
        }
        if (distNorth < minDist) {
            minDist = distNorth;
            spawnX = px; spawnY = py; spawnZ = cuboid.z1();
            faceAxis = 2;
        }
        if (distSouth < minDist) {
            minDist = distSouth;
            spawnX = px; spawnY = py; spawnZ = cuboid.z2();
            faceAxis = 2;
        }
        if (distBottom < minDist) {
            minDist = distBottom;
            spawnX = px; spawnY = cuboid.y1(); spawnZ = pz;
            faceAxis = 1;
        }
        if (distTop < minDist) {
            spawnX = px; spawnY = cuboid.y2(); spawnZ = pz;
            faceAxis = 1;
        }

        Particle particleType;
        try {
            particleType = Particle.valueOf(Configuration.borderDisplay.crossingParticleType.toUpperCase());
        } catch (IllegalArgumentException e) {
            particleType = Particle.END_ROD;
        }

        int particleCount = Configuration.borderDisplay.crossingParticleCount;
        if (particleCount <= 0) {
            particleCount = DEFAULT_CROSSING_PARTICLE_COUNT;
        }

        // Spawn particles with outward velocity along the border plane.
        // count=0 turns offX/offY/offZ into a direction vector, extra into speed.
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < particleCount; i++) {
            double angle = rng.nextDouble() * 2 * Math.PI;
            double dirX, dirY, dirZ;
            switch (faceAxis) {
                case 0: // X-perpendicular face → spread in Y-Z plane
                    dirX = 0;
                    dirY = Math.sin(angle);
                    dirZ = Math.cos(angle);
                    break;
                case 1: // Y-perpendicular face → spread in X-Z plane
                    dirX = Math.cos(angle);
                    dirY = 0;
                    dirZ = Math.sin(angle);
                    break;
                default: // Z-perpendicular face → spread in X-Y plane
                    dirX = Math.cos(angle);
                    dirY = Math.sin(angle);
                    dirZ = 0;
                    break;
            }
            player.spawnParticle(particleType, spawnX, spawnY, spawnZ,
                    0, dirX, dirY, dirZ, CROSSING_PARTICLE_SPEED);
        }
    }

    // ==================== Legacy compatibility ====================

    /**
     * @deprecated Use {@link #showAreaBorder(CommandSender, DominionDTO)} instead.
     */
    @Deprecated
    public static void showBorder(CommandSender sender, DominionDTO dominion) {
        showAreaBorder(sender, dominion);
    }

    /**
     * @deprecated Use {@link #showAreaBorder(Player, DominionDTO)} instead.
     */
    @Deprecated
    public static void showBorder(Player player, DominionDTO dominion) {
        showAreaBorder(player, dominion);
    }

    /**
     * @deprecated Use {@link #showAreaBorder(Player, World, CuboidDTO, Color)} instead.
     */
    @Deprecated
    public static void showBorder(Player player, World world, CuboidDTO cuboid, Color glowColor) {
        showAreaBorder(player, world, cuboid, glowColor);
    }

}
