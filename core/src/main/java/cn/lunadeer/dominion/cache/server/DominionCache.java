package cn.lunadeer.dominion.cache.server;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.cache.DominionNode;
import cn.lunadeer.dominion.cache.DominionNodeSectored;
import cn.lunadeer.dominion.doos.DominionDOO;
import cn.lunadeer.dominion.misc.DominionException;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class DominionCache extends Cache {
    private final Integer serverId;

    private ConcurrentHashMap<Integer, DominionDTO> idDominions;            // Dominion ID -> DominionDTO
    private ConcurrentHashMap<Integer, CopyOnWriteArrayList<Integer>> dominionChildrenMap;  // Dominion ID -> Children Dominion ID
    private ConcurrentHashMap<String, Integer> dominionNameToId;            // Dominion name -> Dominion ID
    private ConcurrentHashMap<UUID, CopyOnWriteArrayList<Integer>> playerOwnDominions;          // Player UUID -> Dominion ID
    private ConcurrentHashMap<UUID, CopyOnWriteArrayList<DominionNode>> playerDominionNodes;  // Player UUID -> DominionNode
    private ConcurrentHashMap<Integer, DominionNode> dominionNodeMap;        // Dominion ID -> DominionNode

    // dominion nodes sectored by location, for fast location-based dominion lookup
    private final DominionNodeSectored dominionNodeSectored = new DominionNodeSectored();

    public DominionCache(Integer serverId) {
        this.serverId = serverId;
    }

    /**
     * Retrieves a DominionDTO by its ID.
     *
     * @param id the ID of the dominion to retrieve
     * @return the DominionDTO associated with the given ID
     * @throws DominionException if the dominion ID is not found
     */
    public @Nullable DominionDTO getDominion(@NotNull Integer id) {
        return idDominions.get(id);
    }

    /**
     * Retrieves a DominionDTO by its name.
     *
     * @param name the name of the dominion to retrieve
     * @return the DominionDTO associated with the given name
     * @throws DominionException if the dominion name is not found
     */
    public @Nullable DominionDTO getDominion(String name) {
        Integer id = dominionNameToId.get(name);
        try {
            if (id == null) return DominionDOO.select(name);
        } catch (Exception e) {
            return null;
        }
        return getDominion(id);
    }

    /**
     * Retrieves a DominionDTO by its location.
     *
     * @param location the location of the dominion to retrieve
     * @return the DominionDTO associated with the given location, or null if not found
     */
    public @Nullable DominionDTO getDominion(@NotNull Location location) {
        return dominionNodeSectored.getDominionByLocation(location);
    }

    /**
     * Retrieves the dominion nodes managed by a player.
     *
     * @param player the UUID of the player
     * @return a list of DominionNode objects managed by the player
     */
    public @NotNull CopyOnWriteArrayList<DominionNode> getPlayerDominionNodes(UUID player) {
        return playerDominionNodes.getOrDefault(player, new CopyOnWriteArrayList<>());
    }

    /**
     * Retrieves all dominion nodes.
     *
     * @return a list of all DominionNode objects
     */
    public @NotNull List<DominionNode> getAllDominionNodes() {
        return new ArrayList<>(dominionNodeMap.values());
    }

    /**
     * Retrieves the direct children of a dominion by its ID.
     *
     * @param id the ID of the parent dominion
     * @return a list of DominionDTO objects representing the children of the given dominion
     */
    public @NotNull List<DominionDTO> getChildrenOf(Integer id) {
        if (dominionChildrenMap.containsKey(id)) {
            return dominionChildrenMap.get(id).stream().map(this::getDominion).toList();
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Retrieves the names of all dominions.
     *
     * @return a list of all dominion names
     */
    public List<String> getAllDominionNames() {
        return new ArrayList<>(dominionNameToId.keySet());
    }

    /**
     * Retrieves the DominionDTOs of the dominions owned by a player.
     *
     * @param player the UUID of the player
     * @return a list of DominionDTOs owned by the player
     */
    public CopyOnWriteArrayList<DominionDTO> getPlayerOwnDominionDTOs(UUID player) {
        CopyOnWriteArrayList<Integer> dominionIds = playerOwnDominions.getOrDefault(player, new CopyOnWriteArrayList<>());
        CopyOnWriteArrayList<DominionDTO> dominions = new CopyOnWriteArrayList<>();
        for (Integer id : dominionIds) {
            dominions.add(getDominion(id));
        }
        return dominions;
    }

    public List<DominionDTO> getPlayerAdminDominionDTOs(UUID player) {
        List<DominionDTO> dominions = new ArrayList<>();
        List<MemberDTO> playerBelongedDominionMembers = Objects.requireNonNull(CacheManager.instance.getCache(serverId)).getMemberCache().getMemberBelongedDominions(player);
        for (MemberDTO member : playerBelongedDominionMembers) {
            if (member.getGroupId() != -1) {
                GroupDTO group = CacheManager.instance.getGroup(member.getGroupId());
                if (group == null) {
                    continue;
                }
                if (group.getFlagValue(Flags.ADMIN)) {
                    dominions.add(getDominion(member.getDomID()));
                }
            } else {
                if (member.getFlagValue(Flags.ADMIN)) {
                    dominions.add(getDominion(member.getDomID()));
                }
            }
        }
        return dominions;
    }

    public @NotNull List<DominionDTO> getAllDominions() {
        return new ArrayList<>(idDominions.values());
    }

    @Override
    void loadExecution() throws Exception {
        // Create temporary maps to avoid race conditions
        ConcurrentHashMap<Integer, DominionDTO> tempIdDominions = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer, CopyOnWriteArrayList<Integer>> tempDominionChildrenMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Integer> tempDominionNameToId = new ConcurrentHashMap<>();
        ConcurrentHashMap<UUID, CopyOnWriteArrayList<Integer>> tempPlayerOwnDominions = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer, DominionNode> tempDominionNodeMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<UUID, CopyOnWriteArrayList<DominionNode>> tempPlayerDominionNodes = new ConcurrentHashMap<>();

        CopyOnWriteArrayList<DominionDTO> dominions = new CopyOnWriteArrayList<>(DominionDOO.selectAll(serverId));

        dominions.forEach(dominion -> tempIdDominions.put(dominion.getId(), dominion));

        // build other caches
        dominions.forEach(dominion -> {
            tempDominionNameToId.put(dominion.getName(), dominion.getId());
            tempPlayerOwnDominions.computeIfAbsent(dominion.getOwner(), k -> new CopyOnWriteArrayList<>()).add(dominion.getId());
            tempDominionChildrenMap.computeIfAbsent(dominion.getParentDomId(), k -> new CopyOnWriteArrayList<>()).add(dominion.getId());
        });

        // Build tree synchronously using temporary cache
        CopyOnWriteArrayList<DominionNode> nodeTree = DominionNode.BuildNodeTree(-1, dominions);
        parseNodeTree(tempPlayerDominionNodes, tempDominionNodeMap, nodeTree, tempIdDominions);

        // Atomically replace all cache data
        synchronized (this) {
            idDominions = tempIdDominions;
            dominionChildrenMap = tempDominionChildrenMap;
            dominionNameToId = tempDominionNameToId;
            playerOwnDominions = tempPlayerOwnDominions;
            dominionNodeMap = tempDominionNodeMap;
            playerDominionNodes = tempPlayerDominionNodes;
            dominionNodeSectored.build(nodeTree);
        }
    }

    @Override
    void loadExecution(Integer idToLoad) throws Exception {
        DominionDTO dominion = DominionDOO.select(idToLoad);
        if (dominion == null) {
            return;
        }
        DominionDTO oldData = idDominions.put(dominion.getId(), dominion);
        // remove old data
        if (oldData != null) {
            dominionNameToId.entrySet().removeIf(entry -> entry.getValue().equals(oldData.getId()));
            playerOwnDominions.computeIfAbsent(oldData.getOwner(), k -> new CopyOnWriteArrayList<>()).remove(oldData.getId());
            dominionChildrenMap.computeIfAbsent(oldData.getParentDomId(), k -> new CopyOnWriteArrayList<>()).remove(oldData.getId());
        }
        // update data
        dominionNameToId.put(dominion.getName(), dominion.getId());
        playerOwnDominions.computeIfAbsent(dominion.getOwner(), k -> new CopyOnWriteArrayList<>()).add(dominion.getId());
        dominionChildrenMap.computeIfAbsent(dominion.getParentDomId(), k -> new CopyOnWriteArrayList<>()).add(dominion.getId());
        // update node tree
        rebuildTreeAsync();
    }

    @Override
    void deleteExecution(Integer idToDelete) throws Exception {
        DominionDTO dominionToDelete = idDominions.remove(idToDelete);
        // remove children map
        dominionChildrenMap.remove(idToDelete);
        if (dominionChildrenMap.containsKey(dominionToDelete.getParentDomId())) {
            dominionChildrenMap.get(dominionToDelete.getParentDomId()).remove(idToDelete);
        }
        // remove name map
        dominionNameToId.entrySet().removeIf(entry -> entry.getValue().equals(idToDelete));
        playerOwnDominions.entrySet().removeIf(entry -> entry.getValue().contains(dominionToDelete.getId()));
        // update node tree
        dominionNodeMap.remove(dominionToDelete.getId());
        rebuildTreeAsync();
    }

    private CompletableFuture<Void> rebuildTreeAsync() {
        return CompletableFuture.runAsync(() -> {
            synchronized (this) {
                ConcurrentHashMap<UUID, CopyOnWriteArrayList<DominionNode>> tempPlayerDominionNodes = new ConcurrentHashMap<>();
                ConcurrentHashMap<Integer, DominionNode> tempDominionNodeMap = new ConcurrentHashMap<>();

                CopyOnWriteArrayList<DominionNode> nodeTree = DominionNode.BuildNodeTree(-1, new CopyOnWriteArrayList<>(idDominions.values()));
                parseNodeTree(tempPlayerDominionNodes, tempDominionNodeMap, nodeTree, idDominions);

                playerDominionNodes = tempPlayerDominionNodes;
                dominionNodeMap = tempDominionNodeMap;
                dominionNodeSectored.build(nodeTree);
            }
        });
    }

    private static void parseNodeTree(ConcurrentHashMap<UUID, CopyOnWriteArrayList<DominionNode>> tempPlayerDominionNodes, ConcurrentHashMap<Integer, DominionNode> tempDominionNodeMap, CopyOnWriteArrayList<DominionNode> nodeTree, ConcurrentHashMap<Integer, DominionDTO> idDominions) {
        nodeTree.forEach(node -> {
            Integer dominionId = node.getDominionId();
            DominionDTO dominionDTO = idDominions.get(dominionId);
            if (dominionDTO != null) {
                tempDominionNodeMap.put(dominionId, node);
                tempPlayerDominionNodes.computeIfAbsent(dominionDTO.getOwner(), k -> new CopyOnWriteArrayList<>()).add(node);
            }
        });
    }

    public Integer count() {
        return idDominions.size();
    }
}
