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

/**
 * Utility class for displaying dominion borders. Two distinct display modes:
 * <ul>
 *   <li><b>Area display</b> ({@link #showAreaBorder}): Used when creating/editing dominions.
 *       Renders 6 semi-transparent faces forming a full cuboid using HoloItem BlockDisplays.</li>
 *   <li><b>Crossing effect</b> ({@link #showCrossingEffect}): Used when a player crosses a dominion border.
 *       Renders the 12 edges of the cuboid as thin particle lines. Each edge is one
 *       {@code spawnParticle(count>0)} call where the client scatters {@code count} particles
 *       within a narrow offset volume along the edge. 12 calls = 12 packets, zero for-loops.</li>
 * </ul>
 */
public class BorderRenderUtil {

    /** How long the area border display remains visible, in ticks (20 ticks = 1 second). */
    private static final long DISPLAY_DURATION_TICKS = 200L;

    /** Thickness of each face wall in blocks. */
    private static final float WALL_THICKNESS = 0.05f;

    /** Default particle dust size for DUST/REDSTONE type. */
    private static final float DUST_SIZE = 1.0f;

    /**
     * Half-thickness of each edge line, in blocks. The client scatters particles
     * within ±LINE_RADIUS of the edge centerline in the two perpendicular axes.
     */
    private static final double LINE_RADIUS = 0.15;

    /** Default particles per unit edge block when config value is invalid. */
    private static final double DEFAULT_PER_EDGE_BLOCK = 4.0;

    public static final Color DEFAULT_GLOW_COLOR = Color.fromRGB(0, 180, 255);

    // ==================== Area Border (for creating/editing dominions) ====================

    public static void showAreaBorder(CommandSender sender, DominionDTO dominion) {
        if (!(sender instanceof Player player)) return;
        showAreaBorder(player, dominion);
    }

    public static void showAreaBorder(Player player, DominionDTO dominion) {
        showAreaBorder(player,
                dominion.getWorld(),
                dominion.getCuboid(),
                Color.fromRGB(dominion.getColorR(), dominion.getColorG(), dominion.getColorB()));
    }

    public static void showAreaBorder(Player player, World world, CuboidDTO cuboid, Color glowColor) {
        Scheduler.runTask(() -> showAreaBorderDisplay(player, world, cuboid, glowColor));
    }

    private static void showAreaBorderDisplay(Player player, World world, CuboidDTO cuboid, Color glowColor) {
        if (player == null || !player.isOnline() || world == null) return;

        String holoName = "area_border_" + player.getUniqueId();
        if (HoloManager.instance().exists(holoName)) {
            HoloManager.instance().remove(holoName);
        }

        int x1 = cuboid.x1(), y1 = cuboid.y1(), z1 = cuboid.z1();
        int x2 = cuboid.x2(), y2 = cuboid.y2(), z2 = cuboid.z2();
        float sizeX = x2 - x1, sizeY = y2 - y1, sizeZ = z2 - z1;
        if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) return;

        Material borderMaterial;
        try {
            borderMaterial = Material.valueOf(Configuration.borderDisplay.borderBlockMaterial.toUpperCase());
        } catch (IllegalArgumentException e) {
            borderMaterial = Material.WHITE_STAINED_GLASS;
        }

        Location anchor = new Location(world, x1, y1, z1);
        HoloItem border = HoloManager.instance().create(holoName, anchor);

        border.addBlockDisplay("face_bottom", borderMaterial)
                .offset(0, 0, 0).scale(sizeX, WALL_THICKNESS, sizeZ).brightness(15, 15);
        border.addBlockDisplay("face_top", borderMaterial)
                .offset(0, sizeY - WALL_THICKNESS, 0).scale(sizeX, WALL_THICKNESS, sizeZ).brightness(15, 15);
        border.addBlockDisplay("face_north", borderMaterial)
                .offset(0, 0, 0).scale(sizeX, sizeY, WALL_THICKNESS).brightness(15, 15);
        border.addBlockDisplay("face_south", borderMaterial)
                .offset(0, 0, sizeZ - WALL_THICKNESS).scale(sizeX, sizeY, WALL_THICKNESS).brightness(15, 15);
        border.addBlockDisplay("face_west", borderMaterial)
                .offset(0, 0, 0).scale(WALL_THICKNESS, sizeY, sizeZ).brightness(15, 15);
        border.addBlockDisplay("face_east", borderMaterial)
                .offset(sizeX - WALL_THICKNESS, 0, 0).scale(WALL_THICKNESS, sizeY, sizeZ).brightness(15, 15);

        border.show(player);
        Scheduler.runTaskLater(() -> {
            if (HoloManager.instance().exists(holoName)) HoloManager.instance().remove(holoName);
        }, DISPLAY_DURATION_TICKS);
    }

    // ==================== Crossing Effect (for player crossing border) ====================

    /**
     * Show the dominion boundary as 12 particle edge lines when the player crosses
     * the border.
     * <p>
     * Each edge is rendered with a single {@code spawnParticle(particle, center, count,
     * offsetX, offsetY, offsetZ, 0)} call. The client scatters {@code count} particles
     * randomly within a thin box centered on the edge segment. Two axes have a narrow
     * offset ({@link #LINE_RADIUS}) to create the line thickness, and the third axis
     * spans the edge length to stretch particles along it.
     * <p>
     * 12 edges = 12 API calls = 12 packets. Zero Java-side for-loops. Zero tick
     * scheduling.
     *
     * @param player   the player who crossed the border
     * @param dominion the dominion whose border was crossed
     */
    public static void showCrossingEffect(Player player, DominionDTO dominion) {
        if (player == null || !player.isOnline() || dominion == null) return;
        if (dominion.getWorld() == null) return;

        CuboidDTO c = dominion.getCuboid();
        double x1 = c.x1(), y1 = c.y1(), z1 = c.z1();
        double x2 = c.x2(), y2 = c.y2(), z2 = c.z2();

        // Parse particle type
        final Particle particleType;
        final boolean isDust;
        {
            Particle pt; boolean d;
            try {
                String tn = Configuration.borderDisplay.crossingParticleType.toUpperCase();
                pt = Particle.valueOf(tn); d = tn.equals("DUST") || tn.equals("REDSTONE");
            } catch (IllegalArgumentException e) { pt = Particle.END_ROD; d = false; }
            particleType = pt; isDust = d;
        }

        double density = Configuration.borderDisplay.boundaryParticlesPerEdgeBlock;
        if (density <= 0) density = DEFAULT_PER_EDGE_BLOCK;

        World world = dominion.getWorld();
        Color color = Color.fromRGB(dominion.getColorR(), dominion.getColorG(), dominion.getColorB());
        Particle.DustOptions dustOpts = isDust ? new Particle.DustOptions(color, DUST_SIZE) : null;

        double r = LINE_RADIUS;

        // ---- 4 bottom edges (Y=y1) ----
        // These are X-axis edges: scatter X = half-length, Y = ±r, Z = ±r
        spawnEdge(player, world, particleType, dustOpts, isDust, x1, y1, z1, x2, y1, z1, density, r);
        spawnEdge(player, world, particleType, dustOpts, isDust, x1, y1, z2, x2, y1, z2, density, r);
        // These are Z-axis edges: scatter X = ±r, Y = ±r, Z = half-length
        spawnEdge(player, world, particleType, dustOpts, isDust, x1, y1, z1, x1, y1, z2, density, r);
        spawnEdge(player, world, particleType, dustOpts, isDust, x2, y1, z1, x2, y1, z2, density, r);

        // ---- 4 top edges (Y=y2) ----
        spawnEdge(player, world, particleType, dustOpts, isDust, x1, y2, z1, x2, y2, z1, density, r);
        spawnEdge(player, world, particleType, dustOpts, isDust, x1, y2, z2, x2, y2, z2, density, r);
        spawnEdge(player, world, particleType, dustOpts, isDust, x1, y2, z1, x1, y2, z2, density, r);
        spawnEdge(player, world, particleType, dustOpts, isDust, x2, y2, z1, x2, y2, z2, density, r);

        // ---- 4 vertical pillars (Y direction) ----
        // Scatter X = ±r, Y = half-height, Z = ±r
        spawnEdge(player, world, particleType, dustOpts, isDust, x1, y1, z1, x1, y2, z1, density, r);
        spawnEdge(player, world, particleType, dustOpts, isDust, x2, y1, z1, x2, y2, z1, density, r);
        spawnEdge(player, world, particleType, dustOpts, isDust, x1, y1, z2, x1, y2, z2, density, r);
        spawnEdge(player, world, particleType, dustOpts, isDust, x2, y1, z2, x2, y2, z2, density, r);
    }

    /**
     * Spawns particles along a single edge using one bulk call.
     * <p>
     * The edge runs from (sx, sy, sz) to (ex, ey, ez), which must differ in exactly
     * one axis (axis-aligned). The center of the edge is the midpoint. The axis that
     * varies gets {@code halfLen} as its offset, and the other two axes get {@code r}
     * as their offset, forming a thin box stretched along the edge direction.
     */
    private static void spawnEdge(Player player, World world,
                                  Particle particleType, Particle.DustOptions dustOpts,
                                  boolean isDust,
                                  double sx, double sy, double sz,
                                  double ex, double ey, double ez,
                                  double density, double r) {
        double dx = ex - sx, dy = ey - sy, dz = ez - sz;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001) return;

        int count = (int) Math.ceil(len * density);
        if (count <= 0) count = 1;

        // Center = midpoint
        double cx = (sx + ex) / 2.0;
        double cy = (sy + ey) / 2.0;
        double cz = (sz + ez) / 2.0;

        // Determine which axis varies along this edge
        double ax = Math.abs(dx), ay = Math.abs(dy), az = Math.abs(dz);

        // Offset: the varying axis gets halfLen, the others get r
        double ox, oy, oz;
        if (ax > ay && ax > az) {
            // X-axis edge
            ox = len / 2.0; oy = r; oz = r;
        } else if (ay > ax && ay > az) {
            // Y-axis edge
            ox = r; oy = len / 2.0; oz = r;
        } else {
            // Z-axis edge
            ox = r; oy = r; oz = len / 2.0;
        }

        // Count>0: client scatters count particles randomly within
        // [cx-ox, cx+ox] × [cy-oy, cy+oy] × [cz-oz, cz+oz]
        if (isDust && dustOpts != null) {
            player.spawnParticle(particleType, cx, cy, cz, count, ox, oy, oz, 0, dustOpts);
        } else {
            player.spawnParticle(particleType, cx, cy, cz, count, ox, oy, oz, 0);
        }
    }

    // ==================== Legacy compatibility ====================

    /** @deprecated Use {@link #showAreaBorder(CommandSender, DominionDTO)} instead. */
    @Deprecated
    public static void showBorder(CommandSender sender, DominionDTO dominion) { showAreaBorder(sender, dominion); }

    /** @deprecated Use {@link #showAreaBorder(Player, DominionDTO)} instead. */
    @Deprecated
    public static void showBorder(Player player, DominionDTO dominion) { showAreaBorder(player, dominion); }

    /** @deprecated Use {@link #showAreaBorder(Player, World, CuboidDTO, Color)} instead. */
    @Deprecated
    public static void showBorder(Player player, World world, CuboidDTO cuboid, Color glowColor) {
        showAreaBorder(player, world, cuboid, glowColor);
    }
}
