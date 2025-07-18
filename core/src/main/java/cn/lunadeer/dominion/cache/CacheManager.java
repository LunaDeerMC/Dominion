package cn.lunadeer.dominion.cache;

import cn.lunadeer.dominion.Dominion;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.cache.server.DominionCache;
import cn.lunadeer.dominion.cache.server.PlayerCache;
import cn.lunadeer.dominion.cache.server.ResidenceDataCache;
import cn.lunadeer.dominion.cache.server.ServerCache;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.doos.PlayerDOO;
import cn.lunadeer.dominion.events.PlayerCrossDominionBorderEvent;
import cn.lunadeer.dominion.events.PlayerMoveInDominionEvent;
import cn.lunadeer.dominion.events.PlayerMoveOutDominionEvent;
import cn.lunadeer.dominion.handler.CacheEventHandler;
import cn.lunadeer.dominion.misc.DominionException;
import cn.lunadeer.dominion.utils.AutoTimer;
import cn.lunadeer.dominion.utils.XLogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static cn.lunadeer.dominion.misc.Others.*;

/**
 * Manages the cache for the server and other servers.
 */
public class CacheManager {
    private final ServerCache thisServerCache;
    private final ConcurrentHashMap<Integer, ServerCache> otherServerCaches;
    private final PlayerCache playerCache;
    private final ResidenceDataCache residenceDataCache = new ResidenceDataCache();

    private boolean recheckPlayerStatus = false;
    private final ConcurrentHashMap<UUID, Integer> playerCurrentDominionId = new ConcurrentHashMap<>();

    public static CacheManager instance;

    public static final Long UPDATE_INTERVAL = 1000 * 4L;

    /**
     * Constructs a CacheManager and initializes the server cache.
     */
    public CacheManager() {
        instance = this;
        this.thisServerCache = new ServerCache(Configuration.multiServer.serverId);
        this.thisServerCache.getDominionCache().load();
        this.thisServerCache.getMemberCache().load();
        this.thisServerCache.getGroupCache().load();

        this.otherServerCaches = new ConcurrentHashMap<>();

        this.playerCache = new PlayerCache();
        this.playerCache.load();

        Bukkit.getPluginManager().registerEvents(new CacheEventHandler(), Dominion.instance);
    }

    // ******************************************************************************************************************
    // * Cache Management Methods
    // ******************************************************************************************************************

    /**
     * Gets the cache for this server.
     *
     * @return the server cache
     */
    public ServerCache getCache() {
        return thisServerCache;
    }

    /**
     * Gets the caches for other servers.
     *
     * @return a map of server IDs to server caches
     */
    public Map<Integer, ServerCache> getOtherServerCaches() {
        return otherServerCaches;
    }

    /**
     * Retrieves the cache for a specific server by its ID.
     * <p>
     * This method returns the cache for the specified server ID. If the ID matches the current server's ID,
     * it returns the cache for this server. Otherwise, it returns the cache from the map of other server caches.
     *
     * @param id the ID of the server whose cache should be retrieved
     * @return the ServerCache associated with the given ID, or null if not found
     */
    public @Nullable ServerCache getCache(Integer id) {
        if (thisServerCache.getServerId().equals(id)) {
            return thisServerCache;
        }
        return otherServerCaches.get(id);
    }

    /**
     * Adds a new server cache to the map of other server caches.
     * <p>
     * This method creates a new ServerCache for the specified server ID and adds it to the map of other server caches.
     *
     * @param serverId the ID of the server whose cache should be added
     */
    public void addServerCache(@NotNull Integer serverId) {
        ServerCache serverCache = new ServerCache(serverId);
        otherServerCaches.put(serverId, serverCache);
    }

    /**
     * Reloads the cache for this server.
     */
    public void reloadCache() {
        thisServerCache.getDominionCache().load();
        thisServerCache.getMemberCache().load();
        thisServerCache.getGroupCache().load();
    }

    /**
     * Reloads the cache for a specific server.
     * <p>
     * This is an asynchronous operation.
     *
     * @param serverId the ID of the server whose cache should be reloaded
     */
    public void reloadServerCache(@NotNull Integer serverId) {
        CompletableFuture.runAsync(() -> {
            if (!otherServerCaches.containsKey(serverId)) {
                XLogger.debug("Server cache not found for serverId: {0}", serverId);
                return;
            }
            otherServerCaches.get(serverId).getDominionCache().load();
            otherServerCaches.get(serverId).getMemberCache().load();
            otherServerCaches.get(serverId).getGroupCache().load();
        });
    }

    /**
     * Retrieves the PlayerCache instance.
     *
     * @return the PlayerCache instance
     */
    public PlayerCache getPlayerCache() {
        return playerCache;
    }

    /**
     * Retrieves the ResidenceDataCache instance.
     *
     * @return the ResidenceDataCache instance
     */
    public ResidenceDataCache getResidenceCache() {
        return residenceDataCache;
    }

    // ******************************************************************************************************************
    // * Player Cache Methods
    // ******************************************************************************************************************

    /**
     * Updates the player's name in the cache.
     * <p>
     * This method updates the last known name of the player in the cache. If the player is not found in the cache,
     * it creates a new PlayerDTO for the player.
     *
     * @param bukkitPlayer the Player object representing the player
     */
    public void updatePlayerName(@NotNull Player bukkitPlayer) throws SQLException {
        PlayerDTO player = playerCache.getPlayer(bukkitPlayer.getUniqueId());
        URL skin = null;
        try{
            skin = bukkitPlayer.getPlayerProfile().getTextures().getSkin();
        }catch (NoSuchMethodError ignored){}
        if (player != null) {
            player.updateLastKnownName(bukkitPlayer.getName(), skin);
        } else {
            PlayerDOO.create(bukkitPlayer);
        }
    }

    /**
     * Retrieves a PlayerDTO by the player's name.
     *
     * @param name the name of the player
     * @return the PlayerDTO associated with the given name, or null if not found
     */
    public @Nullable PlayerDTO getPlayer(String name) {
        return playerCache.getPlayer(name);
    }

    /**
     * Retrieves a PlayerDTO by the player's UUID.
     *
     * @param player the UUID of the player
     * @return the PlayerDTO associated with the given UUID, or null if not found
     */
    public @Nullable PlayerDTO getPlayer(@NotNull UUID player) {
        return playerCache.getPlayer(player);
    }

    /**
     * Retrieves the name of a player by their UUID.
     *
     * @param uuid the UUID of the player
     * @return the name of the player associated with the given UUID
     */
    public @NotNull String getPlayerName(@NotNull UUID uuid) {
        return playerCache.getPlayerName(uuid);
    }

    public @NotNull List<String> getPlayerNames() {
        return playerCache.getPlayerNames();
    }

    // ******************************************************************************************************************
    // * Dominion Cache Methods
    // ******************************************************************************************************************

    /**
     * Retrieves the names of the dominions own & managed by the player.
     * <p>
     * This method retrieves the names of dominions managed by the player, including dominions where the player is an
     * admin member or admin group.
     * <p>
     * If multi-server-mode is enabled, this method will also retrieve the names of dominions managed by the player on
     * other servers.
     *
     * @param player the UUID of the player
     * @return a list of dominion names managed by the player
     */
    public List<String> getPlayerManageDominionNames(@NotNull UUID player) {
        List<DominionDTO> dominions = new ArrayList<>(thisServerCache.getDominionCache().getPlayerOwnDominionDTOs(player));
        dominions.addAll(thisServerCache.getDominionCache().getPlayerAdminDominionDTOs(player));
        // add names from other servers
        if (Configuration.multiServer.enable) {
            for (ServerCache serverCache : otherServerCaches.values()) {
                dominions.addAll(serverCache.getDominionCache().getPlayerOwnDominionDTOs(player));
                dominions.addAll(serverCache.getDominionCache().getPlayerAdminDominionDTOs(player));
            }
        }
        List<String> names = new ArrayList<>(dominions.size());
        for (DominionDTO dominion : dominions) {
            names.add(dominion.getName());
        }
        return names;
    }

    /**
     * Retrieves the names of all dominions.
     * <p>
     * This method retrieves the names of all dominions, including dominions on other servers if multi-server-mode is
     * enabled.
     *
     * @return a list of all dominion names
     */
    public List<String> getAllDominionNames() {
        List<String> names = new ArrayList<>(thisServerCache.getDominionCache().getAllDominionNames());
        // add names from other servers
        if (Configuration.multiServer.enable) {
            for (ServerCache serverCache : otherServerCaches.values()) {
                names.addAll(serverCache.getDominionCache().getAllDominionNames());
            }
        }
        return names;
    }

    /**
     * Retrieves all DominionDTO objects.
     * <p>
     * This method retrieves all dominions from the cache of this server. If multi-servers mode is enabled,
     * it also retrieves dominions from the caches of other servers.
     *
     * @return a list of all DominionDTO objects
     */
    public List<DominionDTO> getAllDominions() {
        List<DominionDTO> dominions = new ArrayList<>(thisServerCache.getDominionCache().getAllDominions());
        // add dominions from other servers
        if (Configuration.multiServer.enable) {
            for (ServerCache serverCache : otherServerCaches.values()) {
                dominions.addAll(serverCache.getDominionCache().getAllDominions());
            }
        }
        return dominions;
    }

    /**
     * Retrieves the child dominions of a given parent dominion.
     * <p>
     * This method retrieves the child dominions of the specified parent dominion from the cache of this server.
     * If multi-servers mode is enabled, it also retrieves child dominions from the caches of other servers.
     *
     * @param parent the parent DominionDTO whose children are to be retrieved
     * @return a list of child DominionDTO objects
     */
    public List<DominionDTO> getChildrenDominionOf(DominionDTO parent) {
        List<DominionDTO> dominions = new ArrayList<>(thisServerCache.getDominionCache().getChildrenOf(parent.getId()));
        // add dominions from other servers
        if (Configuration.multiServer.enable) {
            for (ServerCache serverCache : otherServerCaches.values()) {
                dominions.addAll(serverCache.getDominionCache().getChildrenOf(parent.getId()));
            }
        }
        return dominions;
    }

    /**
     * Retrieves a DominionDTO by its ID.
     * <p>
     * This method will first attempt to retrieve the DominionDTO from the cache of this server. If the DominionDTO
     * is not found, it will then attempt to retrieve the DominionDTO from the caches of other servers.
     *
     * @param id the ID of the dominion to retrieve
     * @return the DominionDTO associated with the given ID
     * @throws DominionException if the dominion ID is not found
     */
    public @Nullable DominionDTO getDominion(Integer id) {
        DominionDTO dominion = thisServerCache.getDominionCache().getDominion(id);
        if (dominion != null) {
            return dominion;
        } else if (Configuration.multiServer.enable) {
            for (ServerCache serverCache : otherServerCaches.values()) {
                dominion = serverCache.getDominionCache().getDominion(id);
                if (dominion != null) {
                    return dominion;
                }
            }
        }
        return null;
    }

    /**
     * Retrieves a DominionDTO by its name.
     * <p>
     * This method will first attempt to retrieve the DominionDTO from the cache of this server. If the DominionDTO
     * is not found, it will then attempt to retrieve the DominionDTO from the caches of other servers.
     *
     * @param name the name of the dominion to retrieve
     * @return the DominionDTO associated with the given name
     * @throws DominionException if the dominion name is not found
     */
    public @NotNull DominionDTO getDominion(String name) {
        DominionDTO dominion = thisServerCache.getDominionCache().getDominion(name);
        if (dominion != null) {
            return dominion;
        } else if (Configuration.multiServer.enable) {
            for (ServerCache serverCache : otherServerCaches.values()) {
                dominion = serverCache.getDominionCache().getDominion(name);
                if (dominion != null) {
                    return dominion;
                }
            }
        }
        throw new DominionException(Language.convertsText.unknownDominion, name);
    }

    /**
     * Retrieves a DominionDTO by its location.
     * <p>
     * This method retrieves the DominionDTO associated with the given location from the cache of this server.
     *
     * @param location the location to retrieve the dominion for
     * @return the DominionDTO associated with the given location, or null if not found
     */
    public @Nullable DominionDTO getDominion(Location location) {
        return thisServerCache.getDominionCache().getDominion(location);
    }

    /**
     * Retrieves the dominions owned by a player.
     * <p>
     * This method retrieves the dominions owned by the player from the cache of this server. If multi-servers mode is enabled,
     * it also retrieves the dominions owned by the player from the caches of other servers.
     *
     * @param player the UUID of the player
     * @return a list of DominionDTO objects representing the dominions owned by the player
     */
    public List<DominionDTO> getPlayerOwnDominionDTOs(UUID player) {
        List<DominionDTO> dominions = new ArrayList<>(thisServerCache.getDominionCache().getPlayerOwnDominionDTOs(player));
        // add dominions from other servers
        if (Configuration.multiServer.enable) {
            for (ServerCache serverCache : otherServerCaches.values()) {
                dominions.addAll(serverCache.getDominionCache().getPlayerOwnDominionDTOs(player));
            }
        }
        return dominions;
    }

    /**
     * Retrieves the dominions where a player is an admin.
     * <p>
     * This method retrieves the dominions where the player is an admin from the cache of this server. If multi-servers mode is enabled,
     * it also retrieves the dominions where the player is an admin from the caches of other servers.
     *
     * @param player the UUID of the player
     * @return a list of DominionDTO objects representing the dominions where the player is an admin
     */
    public List<DominionDTO> getPlayerAdminDominionDTOs(UUID player) {
        List<DominionDTO> dominions = new ArrayList<>(thisServerCache.getDominionCache().getPlayerAdminDominionDTOs(player));
        // add dominions from other servers
        if (Configuration.multiServer.enable) {
            for (ServerCache serverCache : otherServerCaches.values()) {
                dominions.addAll(serverCache.getDominionCache().getPlayerAdminDominionDTOs(player));
            }
        }
        return dominions;
    }

    // ******************************************************************************************************************
    // * Member Cache Methods
    // ******************************************************************************************************************

    /**
     * Retrieves a MemberDTO by the player's UUID.
     * <p>
     * This method retrieves the MemberDTO associated with the given player from the specified dominion.
     *
     * @param dominion the DominionDTO to retrieve the member from
     * @param player   the Player object representing the player
     * @return the MemberDTO associated with the given player, or null if not found
     */
    public @Nullable MemberDTO getMember(@Nullable DominionDTO dominion, @NotNull Player player) {
        return getMember(dominion, player.getUniqueId());
    }

    /**
     * Retrieves a MemberDTO by the player's UUID.
     * <p>
     * This method retrieves the MemberDTO associated with the given player from the specified dominion. If the member is not found
     * in the cache of this server, it will attempt to retrieve the member from the caches of other servers if multi-servers mode is enabled.
     *
     * @param dominion the DominionDTO to retrieve the member from
     * @param player   the UUID of the player
     * @return the MemberDTO associated with the given player, or null if not found
     */
    public @Nullable MemberDTO getMember(@Nullable DominionDTO dominion, @NotNull UUID player) {
        MemberDTO member = thisServerCache.getMemberCache().getMember(dominion, player);
        if (member != null) {
            return member;
        } else if (Configuration.multiServer.enable) {
            for (ServerCache serverCache : otherServerCaches.values()) {
                member = serverCache.getMemberCache().getMember(dominion, player);
                if (member != null) {
                    return member;
                }
            }
        }
        return null;
    }

    // ******************************************************************************************************************
    // * Group Cache Methods
    // ******************************************************************************************************************

    /**
     * Retrieves a GroupDTO by the member's group ID.
     * <p>
     * This method retrieves the GroupDTO associated with the group ID of the given member.
     *
     * @param member the MemberDTO whose group ID is to be used for retrieval
     * @return the GroupDTO associated with the given member's group ID, or null if not found
     */
    public @Nullable GroupDTO getGroup(MemberDTO member) {
        return getGroup(member.getGroupId());
    }

    /**
     * Retrieves a GroupDTO by its ID.
     * <p>
     * This method retrieves the GroupDTO associated with the given ID from the cache of this server. If the GroupDTO
     * is not found, it will then attempt to retrieve the GroupDTO from the caches of other servers if multi-servers mode is enabled.
     *
     * @param id the ID of the group to retrieve
     * @return the GroupDTO associated with the given ID, or null if not found
     */
    public @Nullable GroupDTO getGroup(Integer id) {
        if (id == null) return null;
        if (id == -1) return null;
        GroupDTO group = thisServerCache.getGroupCache().getGroup(id);
        if (group != null) {
            return group;
        } else if (Configuration.multiServer.enable) {
            for (ServerCache serverCache : otherServerCaches.values()) {
                group = serverCache.getGroupCache().getGroup(id);
                if (group != null) {
                    return group;
                }
            }
        }
        return null;
    }

    // ******************************************************************************************************************
    // * Status Check Methods
    // ******************************************************************************************************************

    /**
     * Checks if a recheck of player status is needed.
     *
     * @return true if a recheck is needed, false otherwise
     */
    public boolean needRecheckPlayerStatus() {
        if (recheckPlayerStatus) {
            recheckPlayerStatus = false;
            return true;
        }
        return false;
    }

    /**
     * Sets the flag to recheck player status.
     */
    public void recheckPlayerStatus() {
        this.recheckPlayerStatus = true;
    }

    /**
     * Retrieves the current dominion of a player.
     * <p>
     * This method retrieves the current dominion of the player based on their location. It checks if the player is still
     * in the same dominion and if the dominion has no children. If the player has moved to a different dominion, it triggers
     * the appropriate events and updates the player's current dominion ID.
     *
     * @param player the Player object representing the player
     * @return the DominionDTO associated with the player's current location, or null if not found
     */
    public @Nullable DominionDTO getPlayerCurrentDominion(@NotNull Player player) {
        try (AutoTimer ignored = new AutoTimer(Configuration.timer)) {
            Integer last_in_dom_id = playerCurrentDominionId.get(player.getUniqueId());
            DominionDTO last_dominion = null;
            DominionCache cache = thisServerCache.getDominionCache();
            if (last_in_dom_id != null) {
                last_dominion = cache.getDominion(last_in_dom_id);
            }

            // if player still in the same dominion, and the dominion has no children
            // we don't need to check again and return the last dominion directly
            if (isInDominion(last_dominion, player.getLocation())
                    && cache.getChildrenOf(last_in_dom_id).isEmpty()) {
                checkPlayerStates(player, last_dominion);   // check player states
                return last_dominion;
            }

            // or get the current dominion
            DominionDTO current_dominion = cache.getDominion(player.getLocation());
            int last_dom_id = last_dominion == null ? -1 : last_dominion.getId();
            int current_dom_id = current_dominion == null ? -1 : current_dominion.getId();

            // if last and current dominion are the same, return last dominion
            if (last_dom_id == current_dom_id) {
                checkPlayerStates(player, last_dominion);   // check player states
                return last_dominion;
            }

            // if last and current dominion are different, trigger player cross dominion border event
            new PlayerCrossDominionBorderEvent(player, last_dominion, current_dominion).call();
            recheckPlayerStatus();
            checkPlayerStates(player, current_dominion);   // check player states

            // if last dominion is not null, trigger player move out dominion event
            if (last_dom_id != -1) {
                new PlayerMoveOutDominionEvent(player, last_dominion).call();
            }
            // if current dominion is not null, trigger player move in dominion event
            if (current_dom_id != -1) {
                new PlayerMoveInDominionEvent(player, current_dominion).call();
            }
            // update player current dominion id
            if (current_dominion == null) {
                playerCurrentDominionId.remove(player.getUniqueId());
                return null;
            } else {
                playerCurrentDominionId.put(player.getUniqueId(), current_dominion.getId());
                return current_dominion;
            }
        }
    }

    /**
     * Checks and updates the player's states based on the dominion.
     * <p>
     * This method checks if a recheck of player status is needed and updates the player's states such as flying and lighting
     * based on the dominion.
     *
     * @param player   the Player object representing the player
     * @param dominion the DominionDTO associated with the player's current location
     */
    private void checkPlayerStates(@NotNull Player player, @Nullable DominionDTO dominion) {
        if (!needRecheckPlayerStatus()) return;
        flyOrNot(player, dominion);
        lightOrNot(player, dominion);
    }

    /**
     * Resets the current dominion ID for a player.
     * <p>
     * This method removes the current dominion ID associated with the player from the cache.
     *
     * @param player the Player object representing the player
     */
    public void resetPlayerCurrentDominionId(@NotNull Player player) {
        playerCurrentDominionId.remove(player.getUniqueId());
    }

    // ******************************************************************************************************************
    // * Miscellaneous Methods
    // ******************************************************************************************************************

    /**
     * Retrieves the total count of dominions.
     * <p>
     * This method calculates the total number of dominions by summing the count of dominions on this server and, if
     * multi-servers mode is enabled, the counts from other servers.
     *
     * @return the total count of dominions
     */
    public Integer dominionCount() {
        int count = thisServerCache.getDominionCache().count();
        if (Configuration.multiServer.enable) {
            for (ServerCache serverCache : otherServerCaches.values()) {
                count += serverCache.getDominionCache().count();
            }
        }
        return count;
    }

    /**
     * Retrieves the total count of groups.
     * <p>
     * This method calculates the total number of groups by summing the count of groups on this server and, if
     * multi-servers mode is enabled, the counts from other servers.
     *
     * @return the total count of groups
     */
    public Integer groupCount() {
        int count = thisServerCache.getGroupCache().count();
        if (Configuration.multiServer.enable) {
            for (ServerCache serverCache : otherServerCaches.values()) {
                count += serverCache.getGroupCache().count();
            }
        }
        return count;
    }

    /**
     * Retrieves the total count of members.
     * <p>
     * This method calculates the total number of members by summing the count of members on this server and, if
     * multi-servers mode is enabled, the counts from other servers.
     *
     * @return the total count of members
     */
    public Integer memberCount() {
        int count = thisServerCache.getMemberCache().count();
        if (Configuration.multiServer.enable) {
            for (ServerCache serverCache : otherServerCaches.values()) {
                count += serverCache.getMemberCache().count();
            }
        }
        return count;
    }

}