package cn.lunadeer.dominion.misc;

import cn.lunadeer.dominion.Dominion;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.PlayerDTO;
import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.events.dominion.modify.DominionReSizeEvent;
import cn.lunadeer.dominion.events.dominion.modify.DominionSetMessageEvent;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

import static cn.lunadeer.dominion.misc.Others.sortLocations;

public class Converts {

    public static class ConvertsText extends ConfigurationPart {
        public String mustBePlayer = "Must be a player for this command/operation.";
        public String mustBeOnline = "The player ({0}) must be online.";
        public String worldNotExist = "The world ({0}) does not exist.";

        public String unknownEnvFlag = "Unknown environment flag: {0}.";
        public String unknownPreFlag = "Unknown privilege flag: {0}.";

        public String unknownDominion = "Unknown dominion: {0}.";

        public String invalidNumberFormat = "Invalid number of arguments: {0}.";
        public String invalidBooleanFormat = "Invalid boolean value of arguments: {0}, should be true or false.";

        public String invalidPoints = "You need to select two points in the same world to create a dominion.";
        public String unknownBlockFace = "Unknown direction: {0}.";
        public String unknownResizeType = "Unknown resize type: {0}.";
        public String unknownMessageType = "Unknown message type: {0}.";

        public String invalidColorFormat = "Invalid color value of arguments: {0}, should be a hex color value (0xRRGGBB).";

        public String unknownPlayer = "Player {0} have not been recorded (after the plugin is installed).";

        public String notMember = "Player {0} is not a member of dominion {1}.";

        public String noGroupFound = "No group found with the name {0} in dominion {1}.";
    }

    /**
     * Converts a CommandSender to a Player object.
     *
     * @param sender The CommandSender to be converted.
     * @return The corresponding Player object.
     * @throws DominionException If the sender is not a player.
     */
    public static @NotNull Player toPlayer(@NotNull CommandSender sender) throws DominionException {
        if (sender instanceof Player) {
            return (Player) sender;
        } else {
            throw new DominionException(Language.convertsText.mustBePlayer);
        }
    }

    /**
     * Converts a UUID to a Player object.
     *
     * @param uuid The UUID of the player.
     * @return The corresponding Player object.
     * @throws DominionException If the player is not online.
     */
    public static @NotNull Player toPlayer(UUID uuid) throws DominionException {
        Player player = Dominion.instance.getServer().getPlayer(uuid);
        if (player != null) {
            return player;
        } else {
            throw new DominionException(Language.convertsText.mustBeOnline, uuid.toString());
        }
    }

    /**
     * Converts a player name to a Player object.
     *
     * @param name The name of the player.
     * @return The corresponding Player object.
     * @throws DominionException If the player is not online.
     */
    public static @NotNull Player toPlayer(@NotNull String name) throws DominionException {
        Player player = Dominion.instance.getServer().getPlayer(name);
        if (player != null) {
            return player;
        } else {
            throw new DominionException(Language.convertsText.mustBeOnline, name);
        }
    }

    /**
     * Converts a world name to a World object.
     *
     * @param name The name of the world.
     * @return The corresponding World object.
     * @throws DominionException If the world does not exist.
     */
    public static @NotNull World toWorld(@NotNull String name) throws DominionException {
        World world = Dominion.instance.getServer().getWorld(name);
        if (world != null) {
            return world;
        } else {
            throw new DominionException(Language.convertsText.worldNotExist, name);
        }
    }

    /**
     * Converts a UUID to a World object.
     *
     * @param uuid The UUID of the world.
     * @return The corresponding World object.
     * @throws DominionException If the world does not exist.
     */
    public static @NotNull World toWorld(@NotNull UUID uuid) throws DominionException {
        World world = Dominion.instance.getServer().getWorld(uuid);
        if (world != null) {
            return world;
        } else {
            throw new DominionException(Language.convertsText.worldNotExist, uuid.toString());
        }
    }

    /**
     * Converts a string representation of an environment flag to an EnvFlag object.
     *
     * @param flagName The name of the environment flag.
     * @return The corresponding EnvFlag object.
     * @throws DominionException If the environment flag is unknown.
     */
    public static @NotNull EnvFlag toEnvFlag(String flagName) throws DominionException {
        EnvFlag flag = Flags.getActiveEnvFlag(flagName);
        if (flag == null) {
            throw new DominionException(Language.convertsText.unknownEnvFlag, flagName);
        } else {
            return flag;
        }
    }

    /**
     * Converts a string representation of a privilege flag to a PriFlag object.
     *
     * @param flagName The name of the privilege flag.
     * @return The corresponding PriFlag object.
     * @throws DominionException If the privilege flag is unknown.
     */
    public static @NotNull PriFlag toPriFlag(String flagName) throws DominionException {
        PriFlag flag = Flags.getActivePriFlag(flagName);
        if (flag == null) {
            throw new DominionException(Language.convertsText.unknownPreFlag, flagName);
        } else {
            return flag;
        }
    }

    /**
     * Converts a string representation of a dominion name to a DominionDTO object.
     *
     * @param name The name of the dominion.
     * @return The corresponding DominionDTO object.
     * @throws DominionException If the dominion is unknown.
     */
    public static @NotNull DominionDTO toDominionDTO(@NotNull String name) throws DominionException {
        DominionDTO dominion = CacheManager.instance.getDominion(name);
        if (dominion != null)
            return dominion;
        else
            throw new DominionException(Language.convertsText.unknownDominion, name);
    }

    public static @NotNull DominionDTO toDominionDTO(@NotNull Integer id) throws DominionException {
        DominionDTO dominion = CacheManager.instance.getDominion(id);
        if (dominion == null) {
            throw new DominionException(Language.convertsText.unknownDominion, id.toString());
        } else {
            return dominion;
        }
    }

    /**
     * Converts a string representation of an integer to an int.
     *
     * @param arg The string representation of the integer.
     * @return The corresponding int value.
     * @throws DominionException If the string cannot be parsed as an integer.
     */
    public static int toIntegrity(String arg) throws DominionException {
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            throw new DominionException(Language.convertsText.invalidNumberFormat, arg);
        }
    }

    /**
     * Converts a string representation of an integer to an int, with a default value.
     *
     * @param arg          The string representation of the integer.
     * @param defaultValue The default value to return if the string cannot be parsed as an integer.
     * @return The corresponding int value, or the default value if parsing fails.
     */
    public static int toIntegrity(String arg, int defaultValue) {
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Converts a string representation of a boolean to a boolean.
     *
     * @param arg The string representation of the boolean (e.g., "T" for true, "F" for false).
     * @return The corresponding boolean value.
     * @throws DominionException If the string cannot be parsed as a boolean.
     */
    public static boolean toBoolean(String arg) throws DominionException {
        if (arg.toUpperCase().startsWith("T")) {
            return true;
        } else if (arg.toUpperCase().startsWith("F")) {
            return false;
        } else {
            throw new DominionException(Language.convertsText.invalidBooleanFormat, arg);
        }
    }

    /**
     * Retrieves the selected points for a player.
     *
     * @param player The player whose selected points are to be retrieved.
     * @return An array containing the two selected points.
     * @throws DominionException If the points are not selected or are in different worlds.
     */
    public static Location[] getSelectedPoints(@NotNull Player player) throws DominionException {
        Map<Integer, Location> points = Dominion.pointsSelect.get(player.getUniqueId());
        if (points == null || points.size() != 2) {
            throw new DominionException(Language.convertsText.invalidPoints);
        }
        if (!points.get(0).getWorld().getUID().equals(points.get(1).getWorld().getUID())) {
            throw new DominionException(Language.convertsText.invalidPoints);
        }
        return sortLocations(points.get(0), points.get(1));
    }

    /**
     * Converts a string representation of a message type to a {@link DominionSetMessageEvent.TYPE} enum.
     *
     * @param arg The string representation of the message type (e.g., "E" for ENTER).
     * @return The corresponding {@link DominionSetMessageEvent.TYPE} enum.
     * @throws DominionException If the message type is unknown.
     */
    public static DominionSetMessageEvent.TYPE toMessageType(String arg) {
        if (arg.toUpperCase().startsWith("E")) {
            return DominionSetMessageEvent.TYPE.ENTER;
        } else if (arg.toUpperCase().startsWith("L")) {
            return DominionSetMessageEvent.TYPE.LEAVE;
        } else {
            throw new DominionException(Language.convertsText.unknownMessageType, arg);
        }
    }

    /**
     * Converts a string representation of a resize type to a {@link DominionReSizeEvent.TYPE} enum.
     *
     * @param arg The string representation of the resize type (e.g., "E" for EXPAND).
     * @return The corresponding {@link DominionReSizeEvent.TYPE} enum.
     * @throws DominionException If the resize type is unknown.
     */
    public static DominionReSizeEvent.TYPE toResizeType(String arg) throws DominionException {
        if (arg.toUpperCase().startsWith("E")) {
            return DominionReSizeEvent.TYPE.EXPAND;
        } else if (arg.toUpperCase().startsWith("C")) {
            return DominionReSizeEvent.TYPE.CONTRACT;
        } else {
            throw new DominionException(Language.convertsText.unknownResizeType, arg);
        }
    }

    /**
     * Converts a string representation of a direction to a {@link DominionReSizeEvent.DIRECTION} enum.
     *
     * @param arg The string representation of the direction (e.g., "N" for NORTH).
     * @return The corresponding {@link DominionReSizeEvent.DIRECTION} enum.
     * @throws DominionException If the direction is unknown.
     */
    public static DominionReSizeEvent.DIRECTION toDirection(String arg) throws DominionException {
        if (arg.toUpperCase().startsWith("N")) {
            return DominionReSizeEvent.DIRECTION.NORTH;
        } else if (arg.toUpperCase().startsWith("E")) {
            return DominionReSizeEvent.DIRECTION.EAST;
        } else if (arg.toUpperCase().startsWith("S")) {
            return DominionReSizeEvent.DIRECTION.SOUTH;
        } else if (arg.toUpperCase().startsWith("W")) {
            return DominionReSizeEvent.DIRECTION.WEST;
        } else if (arg.toUpperCase().startsWith("U")) {
            return DominionReSizeEvent.DIRECTION.UP;
        } else if (arg.toUpperCase().startsWith("D")) {
            return DominionReSizeEvent.DIRECTION.DOWN;
        } else {
            throw new DominionException(Language.convertsText.unknownBlockFace, arg);
        }
    }

    /**
     * Determines the direction a player is facing based on their yaw and pitch.
     *
     * @param player The player whose direction is to be determined.
     * @return The corresponding {@link DominionReSizeEvent.DIRECTION} enum.
     */
    public static DominionReSizeEvent.DIRECTION toDirection(Player player) {
        float yaw = player.getLocation().getYaw();
        float pitch = player.getLocation().getPitch();
        if (pitch > -45 && pitch < 45) {
            if (yaw > -45 && yaw < 45) {
                return DominionReSizeEvent.DIRECTION.SOUTH;
            } else if (yaw > 135 || yaw < -135) {
                return DominionReSizeEvent.DIRECTION.NORTH;
            } else if (yaw > 45 && yaw < 135) {
                return DominionReSizeEvent.DIRECTION.WEST;
            } else {
                return DominionReSizeEvent.DIRECTION.EAST;
            }
        } else if (pitch > 45) {
            return DominionReSizeEvent.DIRECTION.DOWN;
        } else {
            return DominionReSizeEvent.DIRECTION.UP;
        }
    }

    /**
     * Converts a hexadecimal color string to a Bukkit Color object.
     *
     * @param arg The hexadecimal color string (e.g., "0xRRGGBB").
     * @return The corresponding Bukkit Color object.
     * @throws DominionException If the color format is invalid.
     */
    public static Color toColor(String arg) throws DominionException {
        if (arg.startsWith("0x")) {
            try {
                int color = Integer.parseInt(arg.substring(2), 16);
                return Color.fromRGB(color);
            } catch (NumberFormatException e) {
                throw new DominionException(Language.convertsText.invalidColorFormat, arg);
            }
        } else {
            throw new DominionException(Language.convertsText.invalidColorFormat, arg);
        }
    }

    /**
     * Converts a player name to a PlayerDTO object.
     *
     * @param name The name of the player.
     * @return The corresponding PlayerDTO object.
     * @throws DominionException If the player is not recorded.
     */
    public static PlayerDTO toPlayerDTO(String name) throws DominionException {
        PlayerDTO playerDTO = CacheManager.instance.getPlayer(name);
        if (playerDTO == null) {
            throw new DominionException(Language.convertsText.unknownPlayer, name);
        } else {
            return playerDTO;
        }
    }

    public static PlayerDTO toPlayerDTO(UUID uuid) {
        PlayerDTO playerDTO = CacheManager.instance.getPlayer(uuid);
        if (playerDTO == null) {
            throw new DominionException(Language.convertsText.unknownPlayer, uuid.toString());
        } else {
            return playerDTO;
        }
    }

    /**
     * Converts a player name to a MemberDTO object.
     *
     * @param dominion   The dominion to which the player belongs.
     * @param playerName The name of the player.
     * @return The corresponding MemberDTO object.
     * @throws DominionException If the player is not a member of the dominion.
     */
    public static @NotNull MemberDTO toMemberDTO(@NotNull DominionDTO dominion, String playerName) {
        PlayerDTO player = toPlayerDTO(playerName);
        MemberDTO member = CacheManager.instance.getMember(dominion, player.getUuid());
        if (member != null) {
            return member;
        }
        throw new DominionException(Language.convertsText.notMember, playerName, dominion.getName());
    }

    public static @NotNull GroupDTO toGroupDTO(@NotNull DominionDTO dominion, String groupName) {
        GroupDTO group = dominion.getGroups().stream().filter(g -> g.getNamePlain().equals(groupName)).findFirst().orElse(null);
        if (group != null) {
            return group;
        }
        throw new DominionException(Language.convertsText.noGroupFound, groupName, dominion.getName());
    }

    public static @NotNull GroupDTO toGroupDTO(@NotNull Integer groupId) {
        GroupDTO group = CacheManager.instance.getGroup(groupId);
        if (group != null) {
            return group;
        }
        throw new DominionException(Language.convertsText.noGroupFound, groupId.toString());
    }

}
