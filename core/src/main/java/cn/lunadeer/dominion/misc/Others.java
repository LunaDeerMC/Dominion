package cn.lunadeer.dominion.misc;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.configuration.WorldWide;
import cn.lunadeer.dominion.doos.PlayerDOO;
import cn.lunadeer.dominion.utils.MessageDisplay;
import cn.lunadeer.dominion.utils.XLogger;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static cn.lunadeer.dominion.Dominion.adminPermission;
import static cn.lunadeer.dominion.misc.Asserts.checkDominionAdmin;
import static cn.lunadeer.dominion.utils.Misc.formatString;

public class Others {

    public static class OthersText extends ConfigurationPart {
        public String autoCleanStart = "Start auto clean players who have not logged in for {0} days.";
        public String autoCleaningPlayer = "Cleaned {0}'s data.";
        public String autoCleanEnd = "Auto clean finished.";
        public String noPermissionForFlag = "&4&lYou do not have {0}({1}) permission.";
    }

    public static boolean bypassLimit(Player player) {
        return player.isOp() || player.hasPermission(adminPermission);
    }

    /**
     * Recursively retrieves all sub-dominions of the given dominion.
     * This method fetches the direct sub-dominions from the cache and then recursively
     * retrieves their sub-dominions, building a complete list of all nested sub-dominions.
     *
     * @param dominion the dominion for which to retrieve sub-dominions
     * @return a list of all sub-dominions of the given dominion
     */
    public static List<DominionDTO> getSubDominionsRecursive(DominionDTO dominion) {
        List<DominionDTO> res = new ArrayList<>();
        List<DominionDTO> sub_dominions = CacheManager.instance.getCache().getDominionCache().getChildrenOf(dominion.getId());
        for (DominionDTO sub_dominion : sub_dominions) {
            res.add(sub_dominion);
            res.addAll(getSubDominionsRecursive(sub_dominion));
        }
        return res;
    }

    /**
     * Calculates the automatic points for a player based on their current location and the configured radius.
     * This method considers the player's current location and expands it by the configured radius to determine
     * two diagonal points of a cuboid area. If vertical inclusion is enabled, it adjusts the Y coordinates
     * based on the player's world settings.
     *
     * @param player The player for whom to calculate the automatic points.
     * @return An array of two Location objects representing the diagonal points of the cuboid area.
     */
    public static Location[] autoPoints(Player player) {
        int size = Configuration.autoCreateRadius;
        Location location = player.getLocation();
        Location location1 = new Location(location.getWorld(), location.getX() - size, location.getY() - size, location.getZ() - size);
        Location location2 = new Location(location.getWorld(), location.getX() + size, location.getY() + size, location.getZ() + size);
        if (Configuration.getPlayerLimitation(player).getWorldSettings(player.getWorld()).autoIncludeVertical) {
            location1.setY(Configuration.getPlayerLimitation(player).getWorldSettings(player.getWorld()).noLowerThan);
            location2.setY(Configuration.getPlayerLimitation(player).getWorldSettings(player.getWorld()).noHigherThan - 1);
        }
        return new Location[]{location1, location2};
    }

    /**
     * Automatically cleans up player data for players who have not logged in for a configured number of days.
     * This method checks the last login time of each player and deletes their data if they have not logged in
     * within the configured number of days. It logs the start, progress, and end of the cleaning process.
     */
    public static void autoClean() {
        if (Configuration.autoCleanAfterDays == -1) {
            return;
        }
        XLogger.info(Language.othersText.autoCleanStart, Configuration.autoCleanAfterDays);
        int auto_clean_after_days = Configuration.autoCleanAfterDays;
        try {
            List<PlayerDTO> players = PlayerDOO.all();
            for (PlayerDTO p : players) {
                if (((PlayerDOO) p).getLastJoinAt() + (long) auto_clean_after_days * 24 * 60 * 60 * 1000 < System.currentTimeMillis()) {
                    try {
                        PlayerDOO.delete((PlayerDOO) p);
                    } catch (Exception e) {
                        XLogger.error(e);
                    }
                    XLogger.info(Language.othersText.autoCleaningPlayer, p.getLastKnownName());
                }
            }
        } catch (Exception e) {
            XLogger.error(e);
        }
        XLogger.info(Language.othersText.autoCleanEnd);
    }

    /**
     * Checks if the player has the specified privilege flag at the given location.
     * <p>
     * If the player does not have the required privilege, a formatted no-permission message is shown
     * to the player at the configured display place, and the method returns false.
     * If the player has the privilege, returns true.
     *
     * @param location the location to check the privilege at
     * @param flag     the privilege flag to check
     * @param player   the player to check
     * @param event    the cancellable event to cancel if the player lacks the privilege
     * @return true if the player has the privilege, false otherwise
     */
    public static boolean checkPrivilegeFlag(@NotNull Location location, @NotNull PriFlag flag, @NotNull Player player, @Nullable Cancellable event) {
        if (checkPrivilegeFlagSilence(location, flag, player, event)) {
            return true;
        } else {
            String msg = formatString(Language.othersText.noPermissionForFlag, flag.getDisplayName(), flag.getDescription());
            msg = "&4" + "&l" + msg;
            MessageDisplay.show(player, MessageDisplay.Place.valueOf(Configuration.pluginMessage.noPermissionDisplayPlace.toUpperCase()), msg);
            return false;
        }
    }

    /**
     * Checks if the player has the specified privilege flag at the given location, without displaying any message.
     * <p>
     * If the player does not have the required privilege, the provided event (if not null) will be cancelled.
     *
     * @param location the location to check the privilege at
     * @param flag     the privilege flag to check
     * @param player   the player to check
     * @param event    the cancellable event to cancel if the player lacks the privilege
     * @return true if the player has the privilege, false otherwise
     */
    public static boolean checkPrivilegeFlagSilence(@NotNull Location location, @NotNull PriFlag flag, @NotNull Player player, @Nullable Cancellable event) {
        if (!flag.getEnable()) {
            return true;
        }
        boolean re;
        DominionDTO dom = CacheManager.instance.getDominion(location);
        if (dom == null) {
            if (!WorldWide.isWorldWideEnabled(location.getWorld())) {
                return true;
            } else {
                if (bypassLimit(player)) return true;
                re = WorldWide.getGuestFlagValue(location.getWorld(), flag);
            }
        } else {
            if (checkDominionAdmin(player, dom)) {
                return true;
            }
            MemberDTO member = CacheManager.instance.getMember(dom, player);
            if (member != null) {
                GroupDTO group = CacheManager.instance.getGroup(member.getGroupId());
                if (member.getGroupId() != -1 && group != null) {
                    re = group.getFlagValue(flag);
                } else {
                    re = member.getFlagValue(flag);
                }
            } else {
                re = dom.getGuestPrivilegeFlagValue().get(flag);
            }
        }
        if (re) {
            return true;
        }
        if (event != null) {
            event.setCancelled(true);
        }
        return false;
    }


    /**
     * Checks if the specified environment flag is enabled at the given location.
     * <p>
     * This method determines whether the provided {@link EnvFlag} is enabled for the given {@link Location}.
     * If the flag is not enabled, the method returns true. If the location is not within a dominion,
     * it checks if the world-wide environment flag is enabled for the world. If the flag is not enabled
     * in either the dominion or world-wide, the provided {@link Cancellable} event (if not null) will be cancelled.
     *
     * @param location the location to check the environment flag at
     * @param flag     the environment flag to check
     * @param event    the cancellable event to cancel if the flag is not enabled
     * @return true if the environment flag is enabled at the location, false otherwise
     */
    public static boolean checkEnvironmentFlag(@NotNull Location location, @NotNull EnvFlag flag, @Nullable Cancellable event) {
        if (!flag.getEnable()) {
            return true;
        }
        boolean re;
        DominionDTO dom = CacheManager.instance.getDominion(location);
        if (dom == null) {
            if (!WorldWide.isWorldWideEnabled(location.getWorld())) {
                return true;
            } else {
                re = WorldWide.getEnvFlagValue(location.getWorld(), flag);
            }
        } else {
            re = dom.getEnvironmentFlagValue().get(flag);
        }
        if (re) {
            return true;
        }
        if (event != null) {
            event.setCancelled(true);
        }
        return false;
    }

    public static boolean isInDominion(@Nullable DominionDTO dominion, @NotNull Location location) {
        if (dominion == null) return false;
        if (!Objects.equals(dominion.getWorldUid(), location.getWorld().getUID())) return false;
        return dominion.getCuboid().contain(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static void lightOrNot(@NotNull Player player, @Nullable DominionDTO dominion) {
        if (!Flags.GLOW.getEnable()) {
            return;
        }
        if (dominion == null) {
            player.setGlowing(false);
            return;
        }
        MemberDTO member = CacheManager.instance.getCache().getMemberCache().getMember(dominion, player);
        if (member != null) {
            if (member.getGroupId() == -1) {
                player.setGlowing(member.getFlagValue(Flags.GLOW));
            } else {
                GroupDTO group = CacheManager.instance.getCache().getGroupCache().getGroup(member.getGroupId());
                if (group != null) {
                    player.setGlowing(group.getFlagValue(Flags.GLOW));
                } else {
                    player.setGlowing(dominion.getGuestPrivilegeFlagValue().get(Flags.GLOW));
                }
            }
        } else {
            player.setGlowing(dominion.getGuestPrivilegeFlagValue().get(Flags.GLOW));
        }
    }

    public static void flyOrNot(@NotNull Player player, @Nullable DominionDTO dominion) {
        for (String flyPN : Configuration.flyPermissionNodes) {
            if (player.hasPermission(flyPN)) {
                return;
            }
        }
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (player.isOp() && Configuration.adminBypass) {
            return;
        }
        if (!Flags.FLY.getEnable()) {
            player.setAllowFlight(false);
            return;
        }
        if (dominion == null) {
            player.setAllowFlight(false);
            return;
        }
        MemberDTO member = CacheManager.instance.getCache().getMemberCache().getMember(dominion, player);
        if (member != null) {
            if (member.getGroupId() == -1) {
                player.setAllowFlight(member.getFlagValue(Flags.FLY));
            } else {
                GroupDTO group = CacheManager.instance.getCache().getGroupCache().getGroup(member.getGroupId());
                if (group != null) {
                    player.setAllowFlight(group.getFlagValue(Flags.FLY));
                } else {
                    player.setAllowFlight(dominion.getGuestPrivilegeFlagValue().get(Flags.FLY));
                }
            }
        } else {
            player.setAllowFlight(dominion.getGuestPrivilegeFlagValue().get(Flags.FLY));
        }
    }

    public static boolean isCrop(@NotNull Material material) {
        return material == Material.COCOA ||
                material == Material.WHEAT ||
                material == Material.CARROTS ||
                material == Material.POTATOES ||
                material == Material.BEETROOTS ||
                material == Material.NETHER_WART ||
                material == Material.SWEET_BERRY_BUSH ||
                material == Material.MELON ||
                material == Material.PUMPKIN ||
                material == Material.SUGAR_CANE ||
                material == Material.BAMBOO ||
                material == Material.CACTUS ||
                material == Material.CHORUS_PLANT ||
                material == Material.CHORUS_FLOWER ||
                material == Material.KELP ||
                material == Material.KELP_PLANT;
    }

    public static boolean isExplodeEntity(@NotNull Entity entity) {
        return entity.getType() == EntityType.CREEPER
                || entity.getType() == EntityType.WITHER_SKULL
                || entity.getType() == EntityType.FIREBALL
                || entity.getType() == EntityType.ENDER_CRYSTAL
                || entity.getType() == EntityType.SMALL_FIREBALL
                || entity.getType() == EntityType.DRAGON_FIREBALL;
    }

    public static Location[] sortLocations(@NotNull Location location1, @NotNull Location location2) {
        int minX = Math.min(location1.getBlockX(), location2.getBlockX());
        int minY = Math.min(location1.getBlockY(), location2.getBlockY());
        int minZ = Math.min(location1.getBlockZ(), location2.getBlockZ());
        int maxX = Math.max(location1.getBlockX(), location2.getBlockX()) + 1;
        int maxY = Math.max(location1.getBlockY(), location2.getBlockY()) + 1;
        int maxZ = Math.max(location1.getBlockZ(), location2.getBlockZ()) + 1;
        return new Location[]{
                new Location(location1.getWorld(), minX, minY, minZ),
                new Location(location1.getWorld(), maxX, maxY, maxZ)
        };
    }
}