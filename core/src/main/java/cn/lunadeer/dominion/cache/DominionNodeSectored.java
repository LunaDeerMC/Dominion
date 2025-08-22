package cn.lunadeer.dominion.cache;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.utils.AutoTimer;
import cn.lunadeer.dominion.utils.XLogger;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;

import static cn.lunadeer.dominion.cache.DominionNode.getDominionNodeByLocation;

/**
 * The DominionNodeSectored class manages the dominion nodes in different sectors of the world
 * with thread-safe operations.
 */
public class DominionNodeSectored {
    /*
        D | C
        --+--
        B | A
     */

    private volatile ConcurrentHashMap<UUID, CopyOnWriteArrayList<DominionNode>> world_dominion_tree_sector_a; // x >= 0, z >= 0
    private volatile ConcurrentHashMap<UUID, CopyOnWriteArrayList<DominionNode>> world_dominion_tree_sector_b; // x <= 0, z >= 0
    private volatile ConcurrentHashMap<UUID, CopyOnWriteArrayList<DominionNode>> world_dominion_tree_sector_c; // x >= 0, z <= 0
    private volatile ConcurrentHashMap<UUID, CopyOnWriteArrayList<DominionNode>> world_dominion_tree_sector_d; // x <= 0, z <= 0
    private volatile Integer section_origin_x = 0;
    private volatile Integer section_origin_z = 0;

    /**
     * Gets the DominionDTO for a given location.
     *
     * @param loc the location to check
     * @return the DominionDTO if found, otherwise null
     */
    public DominionDTO getDominionByLocation(@NotNull Location loc) {
        try (AutoTimer ignored = new AutoTimer(Configuration.timer)) {
            CopyOnWriteArrayList<DominionNode> nodes = getNodes(loc);
            if (nodes == null) return null;
            if (nodes.isEmpty()) return null;
            DominionNode dominionNode = getDominionNodeByLocation(nodes, loc);
            return dominionNode == null ? null : dominionNode.getDominion();
        }
    }

    /**
     * Gets the list of DominionNodes for a given location.
     *
     * @param loc the location to check
     * @return the list of DominionNodes
     */
    public CopyOnWriteArrayList<DominionNode> getNodes(@NotNull Location loc) {
        return getNodes(loc.getWorld().getUID(), loc.getBlockX(), loc.getBlockZ());
    }

    /**
     * Gets the list of DominionNodes for a given world and coordinates.
     *
     * @param world the world to check
     * @param x     the x-coordinate
     * @param z     the z-coordinate
     * @return the list of DominionNodes
     */
    public CopyOnWriteArrayList<DominionNode> getNodes(World world, int x, int z) {
        return getNodes(world.getUID(), x, z);
    }

    /**
     * Gets the list of DominionNodes for a given world UUID and coordinates.
     *
     * @param world the world UUID to check
     * @param x     the x-coordinate
     * @param z     the z-coordinate
     * @return the list of DominionNodes
     */
    public CopyOnWriteArrayList<DominionNode> getNodes(UUID world, int x, int z) {
        // Capture current references to avoid race conditions
        ConcurrentHashMap<UUID, CopyOnWriteArrayList<DominionNode>> sectorA = world_dominion_tree_sector_a;
        ConcurrentHashMap<UUID, CopyOnWriteArrayList<DominionNode>> sectorB = world_dominion_tree_sector_b;
        ConcurrentHashMap<UUID, CopyOnWriteArrayList<DominionNode>> sectorC = world_dominion_tree_sector_c;
        ConcurrentHashMap<UUID, CopyOnWriteArrayList<DominionNode>> sectorD = world_dominion_tree_sector_d;
        int originX = section_origin_x;
        int originZ = section_origin_z;

        if (x >= originX && z >= originZ) {
            if (sectorA == null) return null;
            return sectorA.get(world);
        }
        if (x <= originX && z >= originZ) {
            if (sectorB == null) return null;
            return sectorB.get(world);
        }
        if (x >= originX) {
            if (sectorC == null) return null;
            return sectorC.get(world);
        }
        if (sectorD == null) return null;
        return sectorD.get(world);
    }

    /**
     * Initializes the dominion nodes asynchronously with thread-safe operations.
     * This method returns immediately and performs the build operation in the background.
     *
     * @param nodes the list of DominionNodes to initialize
     * @return CompletableFuture that completes when the build operation is finished
     */
    public CompletableFuture<Void> buildAsync(CopyOnWriteArrayList<DominionNode> nodes) {
        return CompletableFuture.runAsync(() -> {
            try (AutoTimer ignored = new AutoTimer(Configuration.timer)) {
                // Create temporary maps to avoid race conditions
                ConcurrentHashMap<UUID, CopyOnWriteArrayList<DominionNode>> tempSectorA = new ConcurrentHashMap<>();
                ConcurrentHashMap<UUID, CopyOnWriteArrayList<DominionNode>> tempSectorB = new ConcurrentHashMap<>();
                ConcurrentHashMap<UUID, CopyOnWriteArrayList<DominionNode>> tempSectorC = new ConcurrentHashMap<>();
                ConcurrentHashMap<UUID, CopyOnWriteArrayList<DominionNode>> tempSectorD = new ConcurrentHashMap<>();

                // calculate the section origin point
                int max_x = nodes.parallelStream().mapToInt(n -> n.getDominion().getCuboid().x2()).max().orElse(0);
                int min_x = nodes.parallelStream().mapToInt(n -> n.getDominion().getCuboid().x1()).min().orElse(0);
                int max_z = nodes.parallelStream().mapToInt(n -> n.getDominion().getCuboid().z2()).max().orElse(0);
                int min_z = nodes.parallelStream().mapToInt(n -> n.getDominion().getCuboid().z1()).min().orElse(0);
                int tempOriginX = (max_x + min_x) / 2;
                int tempOriginZ = (max_z + min_z) / 2;
                XLogger.debug("Cache init section origin: {0}, {1}", tempOriginX, tempOriginZ);

                // Process nodes in parallel for better performance
                nodes.parallelStream().forEach(n -> {
                    DominionDTO d = n.getDominion();
                    // Ensure world sectors exist
                    tempSectorA.computeIfAbsent(d.getWorldUid(), k -> new CopyOnWriteArrayList<>());
                    tempSectorB.computeIfAbsent(d.getWorldUid(), k -> new CopyOnWriteArrayList<>());
                    tempSectorC.computeIfAbsent(d.getWorldUid(), k -> new CopyOnWriteArrayList<>());
                    tempSectorD.computeIfAbsent(d.getWorldUid(), k -> new CopyOnWriteArrayList<>());

                    // Place dominions into appropriate sectors
                    placeDominionInSectors(n, d, tempOriginX, tempOriginZ, tempSectorA, tempSectorB, tempSectorC, tempSectorD);
                });

                // Atomically replace all sector data
                synchronized (this) {
                    world_dominion_tree_sector_a = tempSectorA;
                    world_dominion_tree_sector_b = tempSectorB;
                    world_dominion_tree_sector_c = tempSectorC;
                    world_dominion_tree_sector_d = tempSectorD;
                    section_origin_x = tempOriginX;
                    section_origin_z = tempOriginZ;
                }
            }
        }, ForkJoinPool.commonPool());
    }

    /**
     * Helper method to place a dominion node into the appropriate sectors.
     */
    private void placeDominionInSectors(DominionNode n, DominionDTO d, int tempOriginX, int tempOriginZ,
                                        ConcurrentHashMap<UUID, CopyOnWriteArrayList<DominionNode>> tempSectorA,
                                        ConcurrentHashMap<UUID, CopyOnWriteArrayList<DominionNode>> tempSectorB,
                                        ConcurrentHashMap<UUID, CopyOnWriteArrayList<DominionNode>> tempSectorC,
                                        ConcurrentHashMap<UUID, CopyOnWriteArrayList<DominionNode>> tempSectorD) {
        if (d.getCuboid().x1() >= tempOriginX && d.getCuboid().z1() >= tempOriginZ) {
            tempSectorA.get(d.getWorldUid()).add(n);
        } else if (d.getCuboid().x1() <= tempOriginX && d.getCuboid().z1() >= tempOriginZ) {
            if (d.getCuboid().x2() >= tempOriginX) {
                tempSectorA.get(d.getWorldUid()).add(n);
                tempSectorB.get(d.getWorldUid()).add(n);
            } else {
                tempSectorB.get(d.getWorldUid()).add(n);
            }
        } else if (d.getCuboid().x1() >= tempOriginX && d.getCuboid().z1() <= tempOriginZ) {
            if (d.getCuboid().z2() >= tempOriginZ) {
                tempSectorA.get(d.getWorldUid()).add(n);
                tempSectorC.get(d.getWorldUid()).add(n);
            } else {
                tempSectorC.get(d.getWorldUid()).add(n);
            }
        } else {
            if (d.getCuboid().x2() >= tempOriginX && d.getCuboid().z2() >= tempOriginZ) {
                tempSectorA.get(d.getWorldUid()).add(n);
                tempSectorB.get(d.getWorldUid()).add(n);
                tempSectorC.get(d.getWorldUid()).add(n);
                tempSectorD.get(d.getWorldUid()).add(n);
            } else if (d.getCuboid().x2() >= tempOriginX && d.getCuboid().z2() <= tempOriginZ) {
                tempSectorC.get(d.getWorldUid()).add(n);
                tempSectorD.get(d.getWorldUid()).add(n);
            } else if (d.getCuboid().z2() >= tempOriginZ && d.getCuboid().x2() <= tempOriginX) {
                tempSectorB.get(d.getWorldUid()).add(n);
                tempSectorD.get(d.getWorldUid()).add(n);
            } else {
                tempSectorD.get(d.getWorldUid()).add(n);
            }
        }
    }
}
