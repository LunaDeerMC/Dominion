package cn.lunadeer.dominion.cache.server;

import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.doos.PlayerDOO;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerCache extends Cache {

    private volatile ConcurrentHashMap<Integer, PlayerDTO> playerCache;
    private volatile ConcurrentHashMap<UUID, Integer> playerIdCache;
    private volatile ConcurrentHashMap<UUID, String> playerNameCache;
    private volatile ConcurrentHashMap<String, Integer> playerNameToId;
    private volatile ConcurrentHashMap<UUID, Integer> playerUsingTitleId;

    public PlayerCache() {
    }

    @Override
    void loadExecution() throws Exception {
        // Create temporary maps to avoid race conditions
        ConcurrentHashMap<UUID, String> tempPlayerNameCache = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Integer> tempPlayerNameToId = new ConcurrentHashMap<>();
        ConcurrentHashMap<UUID, Integer> tempPlayerIdCache = new ConcurrentHashMap<>();
        ConcurrentHashMap<UUID, Integer> tempPlayerUsingTitleId = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer, PlayerDTO> tempPlayerCache = new ConcurrentHashMap<>();

        List<PlayerDTO> players = PlayerDOO.all();
        for (PlayerDTO player : players) {
            tempPlayerIdCache.put(player.getUuid(), player.getId());
            tempPlayerNameToId.put(player.getLastKnownName(), player.getId());
            tempPlayerCache.put(player.getId(), player);
            tempPlayerNameCache.put(player.getUuid(), player.getLastKnownName());
            tempPlayerUsingTitleId.put(player.getUuid(), player.getUsingGroupTitleID());
        }

        // Atomically replace all cache data
        synchronized (this) {
            playerNameCache = tempPlayerNameCache;
            playerNameToId = tempPlayerNameToId;
            playerIdCache = tempPlayerIdCache;
            playerUsingTitleId = tempPlayerUsingTitleId;
            playerCache = tempPlayerCache;
        }
    }

    @Override
    void loadExecution(Integer idToLoad) throws Exception {
        ConcurrentHashMap<Integer, PlayerDTO> currentPlayerCache = playerCache;
        if (currentPlayerCache == null) return;

        PlayerDTO player = currentPlayerCache.remove(idToLoad);
        if (player != null) {
            ConcurrentHashMap<UUID, Integer> currentPlayerIdCache = playerIdCache;
            ConcurrentHashMap<UUID, String> currentPlayerNameCache = playerNameCache;
            ConcurrentHashMap<UUID, Integer> currentPlayerUsingTitleId = playerUsingTitleId;
            ConcurrentHashMap<String, Integer> currentPlayerNameToId = playerNameToId;

            if (currentPlayerIdCache != null) currentPlayerIdCache.remove(player.getUuid());
            if (currentPlayerNameCache != null) currentPlayerNameCache.remove(player.getUuid());
            if (currentPlayerUsingTitleId != null) currentPlayerUsingTitleId.remove(player.getUuid());
            if (currentPlayerNameToId != null) currentPlayerNameToId.remove(player.getLastKnownName());
        }

        player = PlayerDOO.selectById(idToLoad);
        if (player == null) {
            return;
        }

        ConcurrentHashMap<Integer, PlayerDTO> currentPlayerCacheForUpdate = playerCache;
        ConcurrentHashMap<UUID, Integer> currentPlayerIdCacheForUpdate = playerIdCache;
        ConcurrentHashMap<UUID, String> currentPlayerNameCacheForUpdate = playerNameCache;
        ConcurrentHashMap<UUID, Integer> currentPlayerUsingTitleIdForUpdate = playerUsingTitleId;
        ConcurrentHashMap<String, Integer> currentPlayerNameToIdForUpdate = playerNameToId;

        if (currentPlayerCacheForUpdate != null) currentPlayerCacheForUpdate.put(player.getId(), player);
        if (currentPlayerIdCacheForUpdate != null) currentPlayerIdCacheForUpdate.put(player.getUuid(), player.getId());
        if (currentPlayerNameCacheForUpdate != null) currentPlayerNameCacheForUpdate.put(player.getUuid(), player.getLastKnownName());
        if (currentPlayerUsingTitleIdForUpdate != null) currentPlayerUsingTitleIdForUpdate.put(player.getUuid(), player.getUsingGroupTitleID());
        if (currentPlayerNameToIdForUpdate != null) currentPlayerNameToIdForUpdate.put(player.getLastKnownName(), player.getId());
    }

    @Override
    void deleteExecution(Integer idToDelete) throws Exception {
        ConcurrentHashMap<Integer, PlayerDTO> currentPlayerCache = playerCache;
        if (currentPlayerCache == null) return;

        PlayerDTO player = currentPlayerCache.remove(idToDelete);
        if (player != null) {
            ConcurrentHashMap<UUID, Integer> currentPlayerIdCache = playerIdCache;
            ConcurrentHashMap<UUID, String> currentPlayerNameCache = playerNameCache;
            ConcurrentHashMap<UUID, Integer> currentPlayerUsingTitleId = playerUsingTitleId;
            ConcurrentHashMap<String, Integer> currentPlayerNameToId = playerNameToId;

            if (currentPlayerIdCache != null) currentPlayerIdCache.remove(player.getUuid());
            if (currentPlayerNameCache != null) currentPlayerNameCache.remove(player.getUuid());
            if (currentPlayerUsingTitleId != null) currentPlayerUsingTitleId.remove(player.getUuid());
            if (currentPlayerNameToId != null) currentPlayerNameToId.remove(player.getLastKnownName());
        }
    }

    public @Nullable PlayerDTO getPlayer(UUID uuid) {
        ConcurrentHashMap<UUID, Integer> currentPlayerIdCache = playerIdCache;
        ConcurrentHashMap<Integer, PlayerDTO> currentPlayerCache = playerCache;

        if (currentPlayerIdCache != null && currentPlayerCache != null && currentPlayerIdCache.containsKey(uuid)) {
            return currentPlayerCache.get(currentPlayerIdCache.get(uuid));
        } else {
            return null;
        }
    }

    public @Nullable PlayerDTO getPlayer(String name) {
        ConcurrentHashMap<String, Integer> currentPlayerNameToId = playerNameToId;
        ConcurrentHashMap<Integer, PlayerDTO> currentPlayerCache = playerCache;

        if (currentPlayerNameToId != null && currentPlayerCache != null && currentPlayerNameToId.containsKey(name)) {
            return currentPlayerCache.get(currentPlayerNameToId.get(name));
        } else {
            return null;
        }
    }

    public String getPlayerName(UUID uuid) {
        ConcurrentHashMap<UUID, String> currentPlayerNameCache = playerNameCache;
        if (currentPlayerNameCache != null && currentPlayerNameCache.containsKey(uuid)) {
            return currentPlayerNameCache.get(uuid);
        } else {
            return "Unknown Player: %s".formatted(uuid);
        }
    }

    public List<String> getPlayerNames() {
        ConcurrentHashMap<UUID, String> currentPlayerNameCache = playerNameCache;
        return currentPlayerNameCache != null ? new ArrayList<>(currentPlayerNameCache.values()) : new ArrayList<>();
    }

    public Integer getPlayerUsingTitleId(UUID uuid) {
        ConcurrentHashMap<UUID, Integer> currentPlayerUsingTitleId = playerUsingTitleId;
        return currentPlayerUsingTitleId != null ? currentPlayerUsingTitleId.getOrDefault(uuid, -1) : -1;
    }

    public List<GroupDTO> getPlayerGroupTitleList(UUID uuid) {
        List<GroupDTO> groupTitleList = new ArrayList<>();
        List<MemberDTO> playerBelongedDominionMembers = Objects.requireNonNull(CacheManager.instance.getCache()).getMemberCache().getMemberBelongedDominions(uuid);
        if (Configuration.multiServer.enable) {
            for (ServerCache serverCache : CacheManager.instance.getOtherServerCaches().values()) {
                playerBelongedDominionMembers.addAll(serverCache.getMemberCache().getMemberBelongedDominions(uuid));
            }
        }
        for (MemberDTO member : playerBelongedDominionMembers) {
            if (member.getGroupId() == -1) {
                continue;
            }
            GroupDTO group = CacheManager.instance.getGroup(member.getGroupId());
            if (group == null) {
                continue;
            }
            groupTitleList.add(group);
        }
        return groupTitleList;
    }
}
