package cn.lunadeer.dominion.managers;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.cache.CacheManager;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaceHolderApi extends PlaceholderExpansion {

    private final JavaPlugin plugin;

    public static PlaceHolderApi instance = null;

    public PlaceHolderApi(JavaPlugin plugin) {
        this.plugin = plugin;
        this.register();
        instance = this;
    }

    public static String setPlaceholders(Player player, String text) {
        if (instance == null) {
            return text;
        }
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    @Override
    public String onPlaceholderRequest(Player bukkitPlayer, @NotNull String params) {
        // %dominion_group_title%: Get the title of the group the player is currently using
        // @Returns the title of the group, empty if the player is not using a group title
        if (params.equalsIgnoreCase("group_title")) {
            Integer usingId = CacheManager.instance.getPlayerCache().getPlayerUsingTitleId(bukkitPlayer.getUniqueId());
            GroupDTO group = CacheManager.instance.getGroup(usingId);
            if (group == null) {
                return null;
            }
            return group.getNameColoredBukkit();
        }
        // %dominion_current_dominion%: Get the name of the dominion the player is currently in
        // @Returns the name of the dominion, empty if the player is not in a dominion
        if (params.equalsIgnoreCase("current_dominion")) {
            DominionDTO dominion = CacheManager.instance.getDominion(bukkitPlayer.getLocation());
            if (dominion == null) {
                return null;
            }
            return dominion.getName();
        }
        // %dominion_tp_loc_x_<dominion_name>%: Get the x coordinate of the teleport location of a dominion
        // %dominion_tp_loc_y_<dominion_name>%: Get the y coordinate of the teleport location of a dominion
        // %dominion_tp_loc_z_<dominion_name>%: Get the z coordinate of the teleport location of a dominion
        // @Returns the coordinate of the teleport location of the dominion, empty if the dominion does not exist
        if (params.startsWith("tp_loc_")) {
            String coordinate = params.substring(8, 9); // x, y, or z
            String dominionName = params.substring(10); // Get the dominion name after the coordinate

            DominionDTO dominion = CacheManager.instance.getDominion(dominionName);
            if (dominion == null) return null; // Dominion not found

            return switch (coordinate) {
                case "x" -> String.valueOf(dominion.getTpLocation().getBlockX());
                case "y" -> String.valueOf(dominion.getTpLocation().getBlockY());
                case "z" -> String.valueOf(dominion.getTpLocation().getBlockZ());
                default -> null; // Invalid coordinate
            };
        }
        // %dominion_is_member%: Check if the player is a member of the dominion they are currently in
        // @Returns "true" or "false", empty if not in a dominion
        if (params.equalsIgnoreCase("is_member")) {
            DominionDTO dominion = CacheManager.instance.getDominion(bukkitPlayer.getLocation());
            if (dominion == null) return null; // Dominion not found
            return dominion.getMembers().stream().anyMatch(member ->
                    member.getPlayer().getUuid().equals(bukkitPlayer.getUniqueId()))
                    ? "true" : "false";
        }
        // %dominion_is_member_<dominion_name>%: Check if the player is a member of a specific dominion
        // @Returns "true" or "false", empty if the dominion does not exist
        if (params.startsWith("is_member_")) {
            String dominionName = params.substring(17); // Get the dominion name after "is_member_of_"

            DominionDTO dominion = CacheManager.instance.getDominion(dominionName);
            if (dominion == null) return null; // Dominion not found

            return dominion.getMembers().stream().anyMatch(member ->
                    member.getPlayer().getUuid().equals(bukkitPlayer.getUniqueId()))
                    ? "true" : "false";
        }
        // %dominion_members%: Get the list of members in the dominion the player is currently in
        // @Returns a comma-separated list of member names, empty if not in a dominion
        if (params.equalsIgnoreCase("members")) {
            DominionDTO dominion = CacheManager.instance.getDominion(bukkitPlayer.getLocation());
            if (dominion == null) return null; // Dominion not found
            return dominion.getMembers().stream()
                    .map(member -> member.getPlayer().getLastKnownName())
                    .reduce((a, b) -> a + ", " + b).orElse("");
        }
        // %dominion_members_<dominion_name>%: Get the list of members in a specific dominion
        // @Returns a comma-separated list of member names, empty if the dominion does not
        if (params.startsWith("members_")) {
            String dominionName = params.substring(9); // Get the dominion name after "members_"

            DominionDTO dominion = CacheManager.instance.getDominion(dominionName);
            if (dominion == null) return null; // Dominion not found
            return dominion.getMembers().stream()
                    .map(member -> member.getPlayer().getLastKnownName())
                    .reduce((a, b) -> a + ", " + b).orElse("");
        }
        // %dominion_member_count%: Get the number of members in the dominion the player is currently in
        // @Returns the number of members, empty if not in a dominion
        if (params.equalsIgnoreCase("member_count")) {
            DominionDTO dominion = CacheManager.instance.getDominion(bukkitPlayer.getLocation());
            if (dominion == null) return null; // Dominion not found
            return String.valueOf(dominion.getMembers().size());
        }
        // %dominion_member_count_<dominion_name>%: Get the number of members in a specific dominion
        // @Returns the number of members, empty if the dominion does not exist
        if (params.startsWith("member_count_")) {
            String dominionName = params.substring(14); // Get the dominion name after "member_count_"

            DominionDTO dominion = CacheManager.instance.getDominion(dominionName);
            if (dominion == null) return null; // Dominion not found
            return String.valueOf(dominion.getMembers().size());
        }
        // %dominion_group%: Get the name of the group of player in the dominion they are currently in
        // @Returns the name of the group, empty if not in a dominion or not in a group
        if (params.equalsIgnoreCase("group")) {
            DominionDTO dominion = CacheManager.instance.getDominion(bukkitPlayer.getLocation());
            if (dominion == null) return null; // Dominion not found
            return getGroupName(bukkitPlayer, dominion);
        }
        // %dominion_group_<dominion_name>%: Get the name of the group of player in a specific dominion
        // @Returns the name of the group, empty if the dominion does not exist or not in a group
        if (params.startsWith("group_")) {
            String dominionName = params.substring(6); // Get the dominion name after "group_"
            DominionDTO dominion = CacheManager.instance.getDominion(dominionName);
            if (dominion == null) return null; // Dominion not found
            return getGroupName(bukkitPlayer, dominion);
        }
        // %dominion_groups%: Get the list of groups in the dominion the player is currently in
        // @Returns a comma-separated list of group names, empty if not in a dominion
        if (params.equalsIgnoreCase("groups")) {
            DominionDTO dominion = CacheManager.instance.getDominion(bukkitPlayer.getLocation());
            if (dominion == null) return null; // Dominion not found
            return dominion.getGroups().stream()
                    .map(GroupDTO::getNameColoredBukkit)
                    .reduce((a, b) -> a + ", " + b).orElse("");
        }
        // %dominion_groups_<dominion_name>%: Get the list of groups in a specific dominion
        // @Returns a comma-separated list of group names, empty if the dominion does not exist
        if (params.startsWith("groups_")) {
            String dominionName = params.substring(7); // Get the dominion name after "groups_"
            DominionDTO dominion = CacheManager.instance.getDominion(dominionName);
            if (dominion == null) return null; // Dominion not found
            return dominion.getGroups().stream()
                    .map(GroupDTO::getNameColoredBukkit)
                    .reduce((a, b) -> a + ", " + b).orElse("");
        }
        // %dominion_group_count%: Get the number of groups in the dominion the player is currently in
        // @Returns the number of groups, empty if not in a dominion
        if (params.equalsIgnoreCase("group_count")) {
            DominionDTO dominion = CacheManager.instance.getDominion(bukkitPlayer.getLocation());
            if (dominion == null) return null; // Dominion not found
            return String.valueOf(dominion.getGroups().size());
        }
        return null; //
    }

    private static @Nullable String getGroupName(Player bukkitPlayer, DominionDTO dominion) {
        MemberDTO member = CacheManager.instance.getMember(dominion, bukkitPlayer);
        if (member == null || member.getGroupId() == -1) return null; // Not in a group
        GroupDTO group = CacheManager.instance.getGroup(member);
        if (group == null) return null; // Group not found
        return group.getNameColoredBukkit();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "dominion";
    }

    @Override
    public @NotNull String getAuthor() {
        return "zhangyuheng";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

}
