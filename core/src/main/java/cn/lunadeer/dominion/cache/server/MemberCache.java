package cn.lunadeer.dominion.cache.server;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.doos.MemberDOO;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class MemberCache extends Cache {
    private final Integer serverId;

    private volatile ConcurrentHashMap<Integer, MemberDTO> idMembers;            // Member ID -> MemberDTO
    private volatile ConcurrentHashMap<Integer, CopyOnWriteArrayList<Integer>> dominionMembersMap;  // Dominion ID -> Members ID
    private volatile ConcurrentHashMap<UUID, Map<Integer, Integer>> playerDominionMemberMap;  // Player UUID -> (Dominion ID -> Member ID)
    private volatile ConcurrentHashMap<Integer, CopyOnWriteArrayList<Integer>> groupMembersMap;  // Group ID -> Members ID

    public MemberCache(Integer serverId) {
        this.serverId = serverId;
    }

    public @Nullable MemberDTO getMember(@Nullable DominionDTO dominion, @NotNull Player player) {
        return getMember(dominion, player.getUniqueId());
    }

    public @Nullable MemberDTO getMember(@Nullable DominionDTO dominion, @NotNull UUID player_uuid) {
        if (dominion == null) return null;

        // Use local variables to avoid NPE and ensure consistency
        ConcurrentHashMap<UUID, Map<Integer, Integer>> currentPlayerDominionMemberMap = playerDominionMemberMap;
        ConcurrentHashMap<Integer, MemberDTO> currentIdMembers = idMembers;

        if (currentPlayerDominionMemberMap == null || currentIdMembers == null ||
            !currentPlayerDominionMemberMap.containsKey(player_uuid)) {
            return null;
        }

        Map<Integer, Integer> playerMemberMap = currentPlayerDominionMemberMap.get(player_uuid);
        if (playerMemberMap == null) return null;

        Integer member_id = playerMemberMap.get(dominion.getId());
        if (member_id == null) return null;

        return currentIdMembers.get(member_id);
    }

    public List<MemberDTO> getMemberBelongedDominions(@NotNull UUID player) {
        // Use local variables to avoid NPE and ensure consistency
        ConcurrentHashMap<UUID, Map<Integer, Integer>> currentPlayerDominionMemberMap = playerDominionMemberMap;
        ConcurrentHashMap<Integer, MemberDTO> currentIdMembers = idMembers;

        if (currentPlayerDominionMemberMap == null || currentIdMembers == null ||
            !currentPlayerDominionMemberMap.containsKey(player)) {
            return new ArrayList<>();
        }

        Map<Integer, Integer> playerMemberMap = currentPlayerDominionMemberMap.get(player);
        if (playerMemberMap == null) return new ArrayList<>();

        Collection<Integer> member_ids = playerMemberMap.values();
        List<MemberDTO> members = new ArrayList<>();
        for (Integer member_id : member_ids) {
            MemberDTO member = currentIdMembers.get(member_id);
            if (member != null) {
                members.add(member);
            }
        }
        return members;
    }

    public @NotNull List<MemberDTO> getDominionMembers(@NotNull DominionDTO dominion) {
        return getDominionMembers(dominion.getId());
    }

    public @NotNull List<MemberDTO> getDominionMembers(@NotNull Integer dominionId) {
        // Use local variables to avoid NPE and ensure consistency
        ConcurrentHashMap<Integer, CopyOnWriteArrayList<Integer>> currentDominionMembersMap = dominionMembersMap;
        ConcurrentHashMap<Integer, MemberDTO> currentIdMembers = idMembers;

        if (currentDominionMembersMap == null || currentIdMembers == null ||
            !currentDominionMembersMap.containsKey(dominionId)) {
            return new ArrayList<>();
        }

        CopyOnWriteArrayList<Integer> memberIds = currentDominionMembersMap.get(dominionId);
        if (memberIds == null) return new ArrayList<>();

        List<MemberDTO> members = new ArrayList<>();
        for (Integer member_id : memberIds) {
            MemberDTO member = currentIdMembers.get(member_id);
            if (member != null) {
                members.add(member);
            }
        }
        return members;
    }

    public @NotNull List<MemberDTO> getGroupMembers(@NotNull GroupDTO group) {
        return getGroupMembers(group.getId());
    }

    public @NotNull List<MemberDTO> getGroupMembers(@NotNull Integer groupId) {
        // Use local variables to avoid NPE and ensure consistency
        ConcurrentHashMap<Integer, CopyOnWriteArrayList<Integer>> currentGroupMembersMap = groupMembersMap;
        ConcurrentHashMap<Integer, MemberDTO> currentIdMembers = idMembers;

        if (currentGroupMembersMap == null || currentIdMembers == null ||
            !currentGroupMembersMap.containsKey(groupId)) {
            return new ArrayList<>();
        }

        CopyOnWriteArrayList<Integer> memberIds = currentGroupMembersMap.get(groupId);
        if (memberIds == null) return new ArrayList<>();

        List<MemberDTO> members = new ArrayList<>();
        for (Integer member_id : memberIds) {
            MemberDTO member = currentIdMembers.get(member_id);
            if (member != null) {
                members.add(member);
            }
        }
        return members;
    }

    @Override
    void loadExecution() throws Exception {
        // Create temporary maps to avoid race conditions
        ConcurrentHashMap<Integer, MemberDTO> tempIdMembers = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer, CopyOnWriteArrayList<Integer>> tempDominionMembersMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<UUID, Map<Integer, Integer>> tempPlayerDominionMemberMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer, CopyOnWriteArrayList<Integer>> tempGroupMembersMap = new ConcurrentHashMap<>();

        List<MemberDOO> allMembers = MemberDOO.select();
        for (MemberDOO member : allMembers) {
            DominionDTO dominion = CacheManager.instance.getDominion(member.getDomID());
            if (dominion == null || !Objects.equals(dominion.getServerId(), serverId)) continue;

            tempIdMembers.put(member.getId(), member);
            tempDominionMembersMap.computeIfAbsent(member.getDomID(), k -> new CopyOnWriteArrayList<>())
                    .add(member.getId());
            tempPlayerDominionMemberMap.computeIfAbsent(member.getPlayerUUID(), k -> new ConcurrentHashMap<>())
                    .put(member.getDomID(), member.getId());
            if (member.getGroupId() != -1) {
                tempGroupMembersMap.computeIfAbsent(member.getGroupId(), k -> new CopyOnWriteArrayList<>())
                        .add(member.getId());
            }
        }

        // Atomically replace all cache data
        synchronized (this) {
            idMembers = tempIdMembers;
            dominionMembersMap = tempDominionMembersMap;
            playerDominionMemberMap = tempPlayerDominionMemberMap;
            groupMembersMap = tempGroupMembersMap;
        }
    }

    @Override
    void loadExecution(Integer idToLoad) throws Exception {
        MemberDTO member = MemberDOO.select(idToLoad);
        if (member == null) {
            // If member doesn't exist, remove it from cache if present
            synchronized (this) {
                if (idMembers != null) {
                    MemberDTO removed = idMembers.remove(idToLoad);
                    if (removed != null) {
                        removeFromMappings(removed);
                    }
                }
            }
            return;
        }

        // Ensure cache is initialized
        if (idMembers == null || dominionMembersMap == null ||
            playerDominionMemberMap == null || groupMembersMap == null) {
            loadExecution();
            return;
        }

        synchronized (this) {
            MemberDTO old = idMembers.put(member.getId(), member);

            // Remove old member from all mappings if it existed
            if (old != null) {
                removeFromMappings(old);
            }

            // Add new member to all mappings
            addToMappings(member);
        }
    }

    @Override
    void deleteExecution(Integer idToDelete) {
        if (idMembers == null) {
            return;
        }

        synchronized (this) {
            MemberDTO member = idMembers.remove(idToDelete);
            if (member != null) {
                removeFromMappings(member);
            }
        }
    }

    private void removeFromMappings(MemberDTO member) {
        // Remove from dominion members map
        if (dominionMembersMap != null) {
            CopyOnWriteArrayList<Integer> dominionMembers = dominionMembersMap.get(member.getDomID());
            if (dominionMembers != null) {
                dominionMembers.remove(member.getId());
                // Clean up empty lists to prevent memory leaks
                if (dominionMembers.isEmpty()) {
                    dominionMembersMap.remove(member.getDomID());
                }
            }
        }

        // Remove from player dominion member map
        if (playerDominionMemberMap != null) {
            Map<Integer, Integer> playerMemberMap = playerDominionMemberMap.get(member.getPlayerUUID());
            if (playerMemberMap != null) {
                playerMemberMap.remove(member.getDomID());
                // Clean up empty maps to prevent memory leaks
                if (playerMemberMap.isEmpty()) {
                    playerDominionMemberMap.remove(member.getPlayerUUID());
                }
            }
        }

        // Remove from group members map
        if (member.getGroupId() != -1 && groupMembersMap != null) {
            CopyOnWriteArrayList<Integer> groupMembers = groupMembersMap.get(member.getGroupId());
            if (groupMembers != null) {
                groupMembers.remove(member.getId());
                // Clean up empty lists to prevent memory leaks
                if (groupMembers.isEmpty()) {
                    groupMembersMap.remove(member.getGroupId());
                }
            }
        }
    }

    private void addToMappings(MemberDTO member) {
        // Add to dominion members map
        if (dominionMembersMap != null) {
            dominionMembersMap.computeIfAbsent(member.getDomID(), k -> new CopyOnWriteArrayList<>())
                    .addIfAbsent(member.getId());
        }

        // Add to player dominion member map
        if (playerDominionMemberMap != null) {
            playerDominionMemberMap.computeIfAbsent(member.getPlayerUUID(), k -> new ConcurrentHashMap<>())
                    .put(member.getDomID(), member.getId());
        }

        // Add to group members map
        if (member.getGroupId() != -1 && groupMembersMap != null) {
            groupMembersMap.computeIfAbsent(member.getGroupId(), k -> new CopyOnWriteArrayList<>())
                    .addIfAbsent(member.getId());
        }
    }

    public Integer count() {
        ConcurrentHashMap<Integer, MemberDTO> currentIdMembers = idMembers;
        return currentIdMembers != null ? currentIdMembers.size() : 0;
    }
}
