package cn.lunadeer.dominion.cache.server;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.doos.GroupDOO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class GroupCache extends Cache {

    private final Integer serverId;
    private volatile ConcurrentHashMap<Integer, GroupDTO> idGroups;            // Group ID -> GroupDTO
    private volatile ConcurrentHashMap<Integer, CopyOnWriteArrayList<Integer>> dominionGroupsMap;  // Dominion ID -> Groups ID

    public GroupCache(Integer serverId) {
        this.serverId = serverId;
    }

    public @Nullable GroupDTO getGroup(Integer id) {
        ConcurrentHashMap<Integer, GroupDTO> currentGroups = idGroups;
        return currentGroups != null ? currentGroups.get(id) : null;
    }

    public @NotNull List<GroupDTO> getDominionGroups(DominionDTO dominion) {
        if (dominion == null) return new ArrayList<>();
        return getDominionGroups(dominion.getId());
    }

    public @NotNull List<GroupDTO> getDominionGroups(Integer dominionId) {
        ConcurrentHashMap<Integer, CopyOnWriteArrayList<Integer>> currentDominionGroupsMap = dominionGroupsMap;
        ConcurrentHashMap<Integer, GroupDTO> currentIdGroups = idGroups;

        if (currentDominionGroupsMap == null || currentIdGroups == null ||
            !currentDominionGroupsMap.containsKey(dominionId)) {
            return List.of();
        }

        List<GroupDTO> groups = new ArrayList<>();
        CopyOnWriteArrayList<Integer> groupIds = currentDominionGroupsMap.get(dominionId);
        if (groupIds != null) {
            groupIds.forEach(groupId -> {
                GroupDTO group = currentIdGroups.get(groupId);
                if (group != null) {
                    groups.add(group);
                }
            });
        }
        return groups;
    }

    @Override
    void loadExecution() throws Exception {
        // Create temporary maps to avoid race conditions
        ConcurrentHashMap<Integer, GroupDTO> tempIdGroups = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer, CopyOnWriteArrayList<Integer>> tempDominionGroupsMap = new ConcurrentHashMap<>();

        List<GroupDOO> allGroups = GroupDOO.select();
        for (GroupDOO group : allGroups) {
            DominionDTO dominion = CacheManager.instance.getDominion(group.getDomID());
            if (dominion == null || !Objects.equals(dominion.getServerId(), serverId)) continue;

            tempIdGroups.put(group.getId(), group);
            tempDominionGroupsMap.computeIfAbsent(dominion.getId(), k -> new CopyOnWriteArrayList<>())
                    .add(group.getId());
        }

        // Atomically replace all cache data
        synchronized (this) {
            idGroups = tempIdGroups;
            dominionGroupsMap = tempDominionGroupsMap;
        }
    }

    @Override
    void loadExecution(Integer idToLoad) throws Exception {
        GroupDTO group = GroupDOO.select(idToLoad);
        if (group == null) {
            // If group doesn't exist, remove it from cache if present
            synchronized (this) {
                if (idGroups != null) {
                    GroupDTO removed = idGroups.remove(idToLoad);
                    if (removed != null && dominionGroupsMap != null) {
                        CopyOnWriteArrayList<Integer> groupList = dominionGroupsMap.get(removed.getDomID());
                        if (groupList != null) {
                            groupList.remove(removed.getId());
                        }
                    }
                }
            }
            return;
        }

        // Ensure cache is initialized
        if (idGroups == null || dominionGroupsMap == null) {
            loadExecution();
            return;
        }

        synchronized (this) {
            GroupDTO old = idGroups.put(group.getId(), group);

            // Remove old group from dominion mapping if it existed and was in different dominion
            if (old != null && !Objects.equals(old.getDomID(), group.getDomID())) {
                CopyOnWriteArrayList<Integer> oldGroupList = dominionGroupsMap.get(old.getDomID());
                if (oldGroupList != null) {
                    oldGroupList.remove(old.getId());
                }
            }

            // Add to new dominion mapping
            dominionGroupsMap.computeIfAbsent(group.getDomID(), k -> new CopyOnWriteArrayList<>())
                    .addIfAbsent(group.getId());
        }
    }

    @Override
    void deleteExecution(Integer idToDelete) {
        if (idGroups == null || dominionGroupsMap == null) {
            return;
        }

        synchronized (this) {
            GroupDTO group = idGroups.remove(idToDelete);
            if (group != null) {
                CopyOnWriteArrayList<Integer> groupList = dominionGroupsMap.get(group.getDomID());
                if (groupList != null) {
                    groupList.remove(group.getId());
                    // Clean up empty lists to prevent memory leaks
                    if (groupList.isEmpty()) {
                        dominionGroupsMap.remove(group.getDomID());
                    }
                }
            }
        }
    }

    public Integer count() {
        ConcurrentHashMap<Integer, GroupDTO> currentGroups = idGroups;
        return currentGroups != null ? currentGroups.size() : 0;
    }
}
