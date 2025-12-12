package cn.lunadeer.dominion.utils.highlighter;

import cn.lunadeer.dominion.api.dtos.CuboidDTO;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import static cn.lunadeer.dominion.utils.highlighter.HighlighterUtil.RENDER_MAX_RADIUS;
import static cn.lunadeer.dominion.utils.highlighter.HighlighterUtil.adjustBoundary;

/**
 * Highlighter implementation using particles.
 * Works on all Bukkit/Spigot/Paper servers.
 */
public class ParticleHighlighter implements Highlighter {


    @Override
    public void showBorder(Player player, DominionDTO dominion) {
        showBorder(player, dominion.getWorld(), dominion.getCuboid());
    }

    @Override
    public void showBorder(Player player, World world, CuboidDTO cuboid) {
        showBoxFace(player, world,
                cuboid.x1(), cuboid.y1(), cuboid.z1(),
                cuboid.x2(), cuboid.y2(), cuboid.z2());
    }

    @Override
    public void stopHighlighting(Player player) {
        // Particles don't need cleanup - they disappear automatically
    }

    @Override
    public boolean canUse(Player player) {
        return true;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public String getName() {
        return "PARTICLE";
    }

    private void showBoxFace(Player player, World world,
                             int x1, int y1, int z1, int x2, int y2, int z2) {
        Location loc1 = new Location(world, x1, y1, z1);
        Location loc2 = new Location(world, x2, y2, z2);
        showBoxFace(player, loc1, loc2);
    }

    private void showBoxFace(Player player, Location loc1, Location loc2) {
        // Use runTaskAtLocation for Folia compatibility
        Scheduler.runTaskAtLocation(() -> {
            if (!loc1.getWorld().equals(loc2.getWorld())) {
                return;
            }
            int minX = Math.min(loc1.getBlockX(), loc2.getBlockX());
            int minY = Math.min(loc1.getBlockY(), loc2.getBlockY());
            int minZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
            int maxX = Math.max(loc1.getBlockX(), loc2.getBlockX());
            int maxY = Math.max(loc1.getBlockY(), loc2.getBlockY());
            int maxZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ());

            int player_minx = player.getLocation().getBlockX() - RENDER_MAX_RADIUS;
            int player_maxx = player.getLocation().getBlockX() + RENDER_MAX_RADIUS;
            int player_miny = player.getLocation().getBlockY() - RENDER_MAX_RADIUS / 2;
            int player_maxy = player.getLocation().getBlockY() + RENDER_MAX_RADIUS / 2;
            int player_minz = player.getLocation().getBlockZ() - RENDER_MAX_RADIUS;
            int player_maxz = player.getLocation().getBlockZ() + RENDER_MAX_RADIUS;

            boolean skip_minx = false;
            boolean skip_maxx = false;
            boolean skip_minz = false;
            boolean skip_maxz = false;

            int[] adjustedX = adjustBoundary(player_minx, player_maxx, minX, maxX);
            if (minX != adjustedX[0]) {
                skip_minx = true;
                minX = adjustedX[0];
            }
            if (maxX != adjustedX[1]) {
                skip_maxx = true;
                maxX = adjustedX[1];
            }

            int[] adjustedZ = adjustBoundary(player_minz, player_maxz, minZ, maxZ);
            if (minZ != adjustedZ[0]) {
                skip_minz = true;
                minZ = adjustedZ[0];
            }
            if (maxZ != adjustedZ[1]) {
                skip_maxz = true;
                maxZ = adjustedZ[1];
            }

            int[] adjustedY = adjustBoundary(player_miny, player_maxy, minY, maxY);
            minY = adjustedY[0];
            maxY = adjustedY[1];

            Particle particle = getParticleType();

            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    if (!skip_minz) {
                        spawnParticle(player, particle, x, y, minZ);
                    }
                    if (!skip_maxz) {
                        spawnParticle(player, particle, x, y, maxZ);
                    }
                }
                for (int z = minZ; z <= maxZ; z++) {
                    if (!skip_minx) {
                        spawnParticle(player, particle, minX, y, z);
                    }
                    if (!skip_maxx) {
                        spawnParticle(player, particle, maxX, y, z);
                    }
                }
            }
        }, player.getLocation());
    }

    private Particle getParticleType() {
        try {
            return Particle.valueOf(Configuration.highlighter.particleType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Particle.FLAME;
        }
    }

    private void spawnParticle(Player player, Particle particle, double x, double y, double z) {
        player.spawnParticle(particle, x, y, z, 2, 0, 0, 0, 0);
    }
}