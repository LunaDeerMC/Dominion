package cn.lunadeer.dominion.utils.highlighter;

import cn.lunadeer.dominion.api.dtos.CuboidDTO;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.utils.scheduler.CancellableTask;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static cn.lunadeer.dominion.utils.highlighter.HighlighterUtil.RENDER_MAX_RADIUS;

/**
 * Highlighter implementation using BlockDisplay entities.
 * Only works on Paper servers.
 * Provides better visual feedback with glowing blocks.
 */
public class BlockDisplayHighlighter implements Highlighter {

    private static final float SCALAR = 0.002f;
    private static final Transformation SCALE_TRANSFORMATION = new Transformation(
            new Vector3f(-(SCALAR / 2), -(SCALAR / 2), -(SCALAR / 2)),
            new AxisAngle4f(0, 0, 0, 0),
            new Vector3f(1 + SCALAR, 1 + SCALAR, 1 + SCALAR),
            new AxisAngle4f(0, 0, 0, 0)
    );
    private static final Display.Brightness FULL_BRIGHT = new Display.Brightness(15, 15);

    // Store active displays per player
    private final Map<UUID, List<BlockDisplay>> activeDisplays = new ConcurrentHashMap<>();
    // Store cleanup tasks per player
    private final Map<UUID, CancellableTask> cleanupTasks = new ConcurrentHashMap<>();
    // Plugin instance for entity visibility
    private final Plugin plugin;

    public BlockDisplayHighlighter(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void showBorder(Player player, DominionDTO dominion) {
        showBorder(player, dominion.getWorld(), dominion.getCuboid());
    }

    @Override
    public void showBorder(Player player, World world, CuboidDTO cuboid) {
        // Clean up previous displays first
        stopHighlighting(player);

        // Use player's location for region-aware scheduling (Folia compatible)
        Location playerLoc = player.getLocation();
        
        Scheduler.runTaskAtLocation(() -> {
            List<BlockDisplay> displays = Collections.synchronizedList(new ArrayList<>());

            int minX = cuboid.x1();
            int minY = cuboid.y1();
            int minZ = cuboid.z1();
            int maxX = cuboid.x2();
            int maxY = cuboid.y2();
            int maxZ = cuboid.z2();

            int player_minx = playerLoc.getBlockX() - RENDER_MAX_RADIUS;
            int player_maxx = playerLoc.getBlockX() + RENDER_MAX_RADIUS;
            int player_miny = playerLoc.getBlockY() - RENDER_MAX_RADIUS / 2;
            int player_maxy = playerLoc.getBlockY() + RENDER_MAX_RADIUS / 2;
            int player_minz = playerLoc.getBlockZ() - RENDER_MAX_RADIUS;
            int player_maxz = playerLoc.getBlockZ() + RENDER_MAX_RADIUS;

            Material cornerMaterial = getCornerMaterial();
            Material edgeMaterial = getEdgeMaterial();
            Color glowColor = getGlowColor();
            boolean enableGlow = Configuration.highlighter.glowEffect;
            int spacing = Configuration.highlighter.edgeSpacing;

            addCornerIfInRange(displays, world, minX, minY, minZ, cornerMaterial, glowColor, enableGlow, player,
                    player_minx, player_maxx, player_miny, player_maxy, player_minz, player_maxz);
            addCornerIfInRange(displays, world, minX, minY, maxZ, cornerMaterial, glowColor, enableGlow, player,
                    player_minx, player_maxx, player_miny, player_maxy, player_minz, player_maxz);
            addCornerIfInRange(displays, world, maxX, minY, minZ, cornerMaterial, glowColor, enableGlow, player,
                    player_minx, player_maxx, player_miny, player_maxy, player_minz, player_maxz);
            addCornerIfInRange(displays, world, maxX, minY, maxZ, cornerMaterial, glowColor, enableGlow, player,
                    player_minx, player_maxx, player_miny, player_maxy, player_minz, player_maxz);
            addCornerIfInRange(displays, world, minX, maxY, minZ, cornerMaterial, glowColor, enableGlow, player,
                    player_minx, player_maxx, player_miny, player_maxy, player_minz, player_maxz);
            addCornerIfInRange(displays, world, minX, maxY, maxZ, cornerMaterial, glowColor, enableGlow, player,
                    player_minx, player_maxx, player_miny, player_maxy, player_minz, player_maxz);
            addCornerIfInRange(displays, world, maxX, maxY, minZ, cornerMaterial, glowColor, enableGlow, player,
                    player_minx, player_maxx, player_miny, player_maxy, player_minz, player_maxz);
            addCornerIfInRange(displays, world, maxX, maxY, maxZ, cornerMaterial, glowColor, enableGlow, player,
                    player_minx, player_maxx, player_miny, player_maxy, player_minz, player_maxz);

            addEdgeAlongX(displays, world, minX, maxX, minY, minZ, spacing, edgeMaterial, glowColor, enableGlow, player,
                    player_minx, player_maxx, player_miny, player_maxy, player_minz, player_maxz);
            addEdgeAlongX(displays, world, minX, maxX, minY, maxZ, spacing, edgeMaterial, glowColor, enableGlow, player,
                    player_minx, player_maxx, player_miny, player_maxy, player_minz, player_maxz);
            addEdgeAlongX(displays, world, minX, maxX, maxY, minZ, spacing, edgeMaterial, glowColor, enableGlow, player,
                    player_minx, player_maxx, player_miny, player_maxy, player_minz, player_maxz);
            addEdgeAlongX(displays, world, minX, maxX, maxY, maxZ, spacing, edgeMaterial, glowColor, enableGlow, player,
                    player_minx, player_maxx, player_miny, player_maxy, player_minz, player_maxz);

            addEdgeAlongZ(displays, world, minZ, maxZ, minY, minX, spacing, edgeMaterial, glowColor, enableGlow, player,
                    player_minx, player_maxx, player_miny, player_maxy, player_minz, player_maxz);
            addEdgeAlongZ(displays, world, minZ, maxZ, minY, maxX, spacing, edgeMaterial, glowColor, enableGlow, player,
                    player_minx, player_maxx, player_miny, player_maxy, player_minz, player_maxz);
            addEdgeAlongZ(displays, world, minZ, maxZ, maxY, minX, spacing, edgeMaterial, glowColor, enableGlow, player,
                    player_minx, player_maxx, player_miny, player_maxy, player_minz, player_maxz);
            addEdgeAlongZ(displays, world, minZ, maxZ, maxY, maxX, spacing, edgeMaterial, glowColor, enableGlow, player,
                    player_minx, player_maxx, player_miny, player_maxy, player_minz, player_maxz);

            addEdgeAlongY(displays, world, minY, maxY, minX, minZ, spacing, edgeMaterial, glowColor, enableGlow, player,
                    player_minx, player_maxx, player_miny, player_maxy, player_minz, player_maxz);
            addEdgeAlongY(displays, world, minY, maxY, minX, maxZ, spacing, edgeMaterial, glowColor, enableGlow, player,
                    player_minx, player_maxx, player_miny, player_maxy, player_minz, player_maxz);
            addEdgeAlongY(displays, world, minY, maxY, maxX, minZ, spacing, edgeMaterial, glowColor, enableGlow, player,
                    player_minx, player_maxx, player_miny, player_maxy, player_minz, player_maxz);
            addEdgeAlongY(displays, world, minY, maxY, maxX, maxZ, spacing, edgeMaterial, glowColor, enableGlow, player,
                    player_minx, player_maxx, player_miny, player_maxy, player_minz, player_maxz);

            activeDisplays.put(player.getUniqueId(), displays);

            // Schedule automatic cleanup after configured duration
            int duration = Configuration.highlighter.displayDuration;
            if (duration > 0) {
                CancellableTask task = Scheduler.runTaskAtLocationLater(() -> {
                    stopHighlighting(player);
                }, playerLoc, duration * 20L); // Convert seconds to ticks
                cleanupTasks.put(player.getUniqueId(), task);
            }
        }, playerLoc);
    }

    private void addCornerIfInRange(List<BlockDisplay> displays, World world, int x, int y, int z,
                                    Material material, Color glowColor, boolean enableGlow, Player player,
                                    int pMinX, int pMaxX, int pMinY, int pMaxY, int pMinZ, int pMaxZ) {
        if (isInRange(x, pMinX, pMaxX) && isInRange(y, pMinY, pMaxY) && isInRange(z, pMinZ, pMaxZ)) {
            BlockDisplay display = createBlockDisplay(world, x, y, z, material, glowColor, enableGlow, player);
            if (display != null) displays.add(display);
        }
    }

    private void addEdgeAlongX(List<BlockDisplay> displays, World world, int minX, int maxX, int y, int z,
                               int spacing, Material material, Color glowColor, boolean enableGlow, Player player,
                               int pMinX, int pMaxX, int pMinY, int pMaxY, int pMinZ, int pMaxZ) {
        if (!isInRange(y, pMinY, pMaxY) || !isInRange(z, pMinZ, pMaxZ)) return;
        
        int edgeLength = maxX - minX;
        // For edges of length 2 or less, there is no space between corners to place additional edge blocks.
        // Only the corners are marked in this case.
        if (edgeLength <= 2) return;
        
        // Always place a block next to the corner (minX + 1)
        if (isInRange(minX + 1, pMinX, pMaxX)) {
            BlockDisplay display = createBlockDisplay(world, minX + 1, y, z, material, glowColor, enableGlow, player);
            if (display != null) displays.add(display);
        }
        
        // Always place a block next to the other corner (maxX - 1)
        if (maxX - 1 > minX + 1 && isInRange(maxX - 1, pMinX, pMaxX)) {
            BlockDisplay display = createBlockDisplay(world, maxX - 1, y, z, material, glowColor, enableGlow, player);
            if (display != null) displays.add(display);
        }
        
        // Place blocks at regular intervals in between
        for (int x = minX + 1 + spacing; x < maxX - 1; x += spacing) {
            if (isInRange(x, pMinX, pMaxX)) {
                BlockDisplay display = createBlockDisplay(world, x, y, z, material, glowColor, enableGlow, player);
                if (display != null) displays.add(display);
            }
        }
    }

    private void addEdgeAlongZ(List<BlockDisplay> displays, World world, int minZ, int maxZ, int y, int x,
                               int spacing, Material material, Color glowColor, boolean enableGlow, Player player,
                               int pMinX, int pMaxX, int pMinY, int pMaxY, int pMinZ, int pMaxZ) {
        if (!isInRange(y, pMinY, pMaxY) || !isInRange(x, pMinX, pMaxX)) return;
        
        int edgeLength = maxZ - minZ;
        // For edges of length 2 or less, there is no space between corners to place additional edge blocks.
        // Only the corners are marked in this case.
        if (edgeLength <= 2) return;
        
        // Always place a block next to the corner (minZ + 1)
        if (isInRange(minZ + 1, pMinZ, pMaxZ)) {
            BlockDisplay display = createBlockDisplay(world, x, y, minZ + 1, material, glowColor, enableGlow, player);
            if (display != null) displays.add(display);
        }
        
        // Always place a block next to the other corner (maxZ - 1)
        if (maxZ - 1 > minZ + 1 && isInRange(maxZ - 1, pMinZ, pMaxZ)) {
            BlockDisplay display = createBlockDisplay(world, x, y, maxZ - 1, material, glowColor, enableGlow, player);
            if (display != null) displays.add(display);
        }
        
        // Place blocks at regular intervals in between
        for (int z = minZ + 1 + spacing; z < maxZ - 1; z += spacing) {
            if (isInRange(z, pMinZ, pMaxZ)) {
                BlockDisplay display = createBlockDisplay(world, x, y, z, material, glowColor, enableGlow, player);
                if (display != null) displays.add(display);
            }
        }
    }

    private void addEdgeAlongY(List<BlockDisplay> displays, World world, int minY, int maxY, int x, int z,
                               int spacing, Material material, Color glowColor, boolean enableGlow, Player player,
                               int pMinX, int pMaxX, int pMinY, int pMaxY, int pMinZ, int pMaxZ) {
        if (!isInRange(x, pMinX, pMaxX) || !isInRange(z, pMinZ, pMaxZ)) return;
        
        int edgeLength = maxY - minY;
        // For edges of length 2 or less, there is no space between corners to place additional edge blocks.
        // Only the corners are marked in this case.
        if (edgeLength <= 2) return;
        
        // Always place a block next to the corner (minY + 1)
        if (isInRange(minY + 1, pMinY, pMaxY)) {
            BlockDisplay display = createBlockDisplay(world, x, minY + 1, z, material, glowColor, enableGlow, player);
            if (display != null) displays.add(display);
        }
        
        // Always place a block next to the other corner (maxY - 1)
        if (maxY - 1 > minY + 1 && isInRange(maxY - 1, pMinY, pMaxY)) {
            BlockDisplay display = createBlockDisplay(world, x, maxY - 1, z, material, glowColor, enableGlow, player);
            if (display != null) displays.add(display);
        }
        
        // Place blocks at regular intervals in between
        for (int y = minY + 1 + spacing; y < maxY - 1; y += spacing) {
            if (isInRange(y, pMinY, pMaxY)) {
                BlockDisplay display = createBlockDisplay(world, x, y, z, material, glowColor, enableGlow, player);
                if (display != null) displays.add(display);
            }
        }
    }

    private boolean isInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    @SuppressWarnings("UnstableApiUsage")
    private BlockDisplay createBlockDisplay(World world, int x, int y, int z,
                                            Material material, Color glowColor,
                                            boolean enableGlow, Player player) {
        Location loc = new Location(world, x, y, z);
        
        // Check if world is loaded (thread-safe check)
        if (!loc.isWorldLoaded()) {
            return null;
        }

        try {
            BlockDisplay display = (BlockDisplay) world.spawnEntity(loc, EntityType.BLOCK_DISPLAY);
            display.setBlock(material.createBlockData());
            display.setViewRange(0.5f);
            display.setGravity(false);
            display.setPersistent(false);
            display.setBrightness(FULL_BRIGHT);
            display.setVisibleByDefault(false);

            // Scale to prevent z-fighting
            display.setTransformation(SCALE_TRANSFORMATION);

            // Apply glow effect if enabled
            if (enableGlow) {
                display.setGlowing(true);
                display.setGlowColorOverride(glowColor);
            }

            // Show only to the specific player
            player.showEntity(plugin, display);

            return display;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void stopHighlighting(Player player) {
        // Cancel any pending cleanup task
        CancellableTask task = cleanupTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }

        // Remove all displays for this player
        List<BlockDisplay> displays = activeDisplays.remove(player.getUniqueId());
        if (displays != null && !displays.isEmpty()) {
            // Remove each display on its own region thread (Folia compatible)
            for (BlockDisplay display : displays) {
                if (display != null && display.isValid()) {
                    Scheduler.runEntityTask(() -> {
                        if (display.isValid()) {
                            display.remove();
                        }
                    }, display);
                }
            }
        }
    }

    @Override
    public boolean canUse(Player player) {
        return true;
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public String getName() {
        return "BLOCK_DISPLAY";
    }

    private Material getCornerMaterial() {
        try {
            return Material.valueOf(Configuration.highlighter.cornerBlockType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.GLOWSTONE;
        }
    }

    private Material getEdgeMaterial() {
        try {
            return Material.valueOf(Configuration.highlighter.edgeBlockType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.GOLD_BLOCK;
        }
    }

    private Color getGlowColor() {
        String colorHex = Configuration.highlighter.glowColor;
        try {
            if (colorHex.startsWith("#")) {
                colorHex = colorHex.substring(1);
            }
            int rgb = Integer.parseInt(colorHex, 16);
            return Color.fromRGB(rgb);
        } catch (Exception e) {
            return Color.YELLOW;
        }
    }

}