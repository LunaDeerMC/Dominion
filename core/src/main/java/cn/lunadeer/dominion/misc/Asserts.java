package cn.lunadeer.dominion.misc;

import cn.lunadeer.dominion.api.dtos.CuboidDTO;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.GroupDTO;
import cn.lunadeer.dominion.api.dtos.MemberDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.configuration.Limitation;
import cn.lunadeer.dominion.doos.DominionDOO;
import cn.lunadeer.dominion.managers.HooksManager;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.VaultConnect.VaultConnect;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static cn.lunadeer.dominion.misc.Converts.toWorld;
import static cn.lunadeer.dominion.misc.Others.bypassLimit;

/**
 * Asserts class.
 * <p>
 * This class provides some static methods to assert some conditions.
 */
public class Asserts {

    public static class AssertsText extends ConfigurationPart {
        public String domNameShouldNotEmpty = "Dominion name should not be empty.";
        public String domNameInvalid = "Dominion name should not contain space, dot, §, &, ~ or ,.";
        public String domNameExist = "Dominion name: {0} already exists.";

        public String groupNameShouldNotEmpty = "Group name should not be empty.";
        public String groupNameInvalid = "Group name should not contain space or dot.";
        public String groupNameExist = "Group name: {0} already exists.";

        public String notAllowDomInWorld = "Player {0} is not allowed to create dominions in world {1}.";
        public String exceedMaxAmount = "Player {0} can not create more dominions (max: {1}).";
        public String exceedMaxAmountOfWorld = "Player {0} can not create more dominions in world {1} (max: {2}).";

        public String xLengthTooShort = "West-East(X) length {0} should more than {1}.";
        public String yLengthTooShort = "Down-Up(Y) length {0} should more than {1}.";
        public String zLengthTooShort = "South-North(Z) length {0} should more than {1}.";
        public String xLengthTooLong = "West-East(X) length {0} should less than {1}.";
        public String yLengthTooLong = "Down-Up(Y) length {0} should less than {1}.";
        public String zLengthTooLong = "South-North(Z) length {0} should less than {1}.";
        public String yTooHigh = "The highest point of the dominion {0} should less than {1}.";
        public String yTooLow = "The lowest point of the dominion {0} should more than {1}.";

        public String notOwner = "Only {0}'s owner can perform this command/operation.";
        public String notAdmin = "Only {0}'s admin can perform this command/operation.";

        public String intersectWithDom = "Dominion {0} intersects with dominion {1}.";
        public String intersectWithSpawn = "Dominion {0} intersects with the spawn protection area.";

        public String outsideOfParentDom = "Dominion {0} is outside of it's parent dominion {1}.";
        public String cantContainChild = "Dominion {0} can not contain it's child dominion {1}.";
        public String missingParentDom = "Parent dominion of {0} is missing.";
        public String subDomTooDeep = "Player {0} can't create sub-dominion of depth more than {1}.";

        public String withDrawMoney = "Successfully paid {0} for dominion.";
        public String depositMoney = "Successfully refunded {0} from dominion.";

        public String groupNotBelongDominion = "Group {0} does not belong to dominion {1}.";
    }

    /**
     * Asserts that the given dominion name is valid.
     *
     * @param dominionName the name of the dominion to check
     * @throws DominionException if the dominion name is empty, contains spaces or dots, or already exists
     */
    public static void assertDominionName(String dominionName) throws DominionException, SQLException {
        if (dominionName.isEmpty()) {
            throw new DominionException(Language.assertsText.domNameShouldNotEmpty);
        }
        if (dominionName.contains(" ") || dominionName.contains(".")
                || dominionName.contains("§") || dominionName.contains("&")
                || dominionName.contains("~") || dominionName.contains(",")) {
            throw new DominionException(Language.assertsText.domNameInvalid);
        }
        if (DominionDOO.select(dominionName) != null) {
            throw new DominionException(Language.assertsText.domNameExist, dominionName);
        }
    }

    public static void assertGroupName(@NotNull DominionDTO dominion, String groupNamePlain) throws DominionException, SQLException {
        if (groupNamePlain.isEmpty()) {
            throw new DominionException(Language.assertsText.groupNameShouldNotEmpty);
        }
        if (groupNamePlain.contains(" ") || groupNamePlain.contains(".")) {
            throw new DominionException(Language.assertsText.groupNameInvalid);
        }
        if (dominion.getGroups().stream().anyMatch(group -> group.getNamePlain().equals(groupNamePlain))) {
            throw new DominionException(Language.assertsText.groupNameExist, groupNamePlain);
        }
    }

    /**
     * Asserts that the player has not exceeded the maximum number of dominions they can create.
     *
     * @param operator           the command sender (usually a player)
     * @param associatedWorldUid the world in which the dominion is located
     * @throws DominionException if the player has exceeded the maximum number of dominions they can create
     */
    public static void assertPlayerDominionAmount(@NotNull CommandSender operator, @NotNull UUID associatedWorldUid) throws DominionException {
        if (!(operator instanceof Player associatedPlayer)) {
            return;
        }
        if (bypassLimit(associatedPlayer)) {
            return;
        }
        List<DominionDTO> dominions = CacheManager.instance.getCache().getDominionCache().getPlayerOwnDominionDTOs(associatedPlayer.getUniqueId());
        int allOverTheWorld = Configuration.getPlayerLimitation(associatedPlayer).amountAllOverTheWorld;
        if (dominions.size() >= allOverTheWorld && allOverTheWorld >= 0) {
            throw new DominionException(Language.assertsText.exceedMaxAmount, associatedPlayer.getName(), allOverTheWorld);
        }
        World associatedWorld = toWorld(associatedWorldUid);
        int amountOfWorld = Configuration.getPlayerLimitation(associatedPlayer).getWorldSettings(associatedWorld.getName()).amount;
        if (amountOfWorld == 0) {
            throw new DominionException(Language.assertsText.notAllowDomInWorld, associatedPlayer.getName(), associatedWorld.getName());
        }
        if (dominions.stream().filter(dom -> dom.getWorldUid().equals(associatedWorld.getUID())).count() >= amountOfWorld
                && amountOfWorld >= 0) {
            throw new DominionException(Language.assertsText.exceedMaxAmountOfWorld, associatedPlayer.getName(), associatedWorld.getName(), amountOfWorld);
        }
    }

    /**
     * Asserts that the size of the given dominion is within the allowed limits.
     *
     * @param operator           the command operator (usually a player)
     * @param associatedWorldUid the world in which the dominion is located
     * @param cuboid             the cuboid representing the dominion's size
     * @throws DominionException if the dominion size is outside the allowed limits
     */
    public static void assertDominionSize(@NotNull CommandSender operator, @NotNull UUID associatedWorldUid, CuboidDTO cuboid) throws DominionException {
        if (!(operator instanceof Player associatedPlayer)) {
            return;
        }
        if (bypassLimit(associatedPlayer)) {
            return;
        }

        String associatedWorld = toWorld(associatedWorldUid).getName();
        int sizeMinX = Configuration.getPlayerLimitation(associatedPlayer).getWorldSettings(associatedWorld).sizeMinX;
        if (cuboid.xLength() < sizeMinX) {
            throw new DominionException(Language.assertsText.xLengthTooShort, cuboid.xLength(), sizeMinX);
        }
        int sizeMinY = Configuration.getPlayerLimitation(associatedPlayer).getWorldSettings(associatedWorld).sizeMinY;
        if (cuboid.yLength() < sizeMinY) {
            throw new DominionException(Language.assertsText.yLengthTooShort, cuboid.yLength(), sizeMinY);
        }
        int sizeMinZ = Configuration.getPlayerLimitation(associatedPlayer).getWorldSettings(associatedWorld).sizeMinZ;
        if (cuboid.zLength() < sizeMinZ) {
            throw new DominionException(Language.assertsText.zLengthTooShort, cuboid.zLength(), sizeMinZ);
        }
        int sizeMaxX = Configuration.getPlayerLimitation(associatedPlayer).getWorldSettings(associatedWorld).sizeMaxX;
        if (sizeMaxX > 0 && cuboid.xLength() > sizeMaxX) {
            throw new DominionException(Language.assertsText.xLengthTooLong, cuboid.xLength(), sizeMaxX);
        }
        int sizeMaxY = Configuration.getPlayerLimitation(associatedPlayer).getWorldSettings(associatedWorld).sizeMaxY;
        if (sizeMaxY > 0 && cuboid.yLength() > sizeMaxY) {
            throw new DominionException(Language.assertsText.yLengthTooLong, cuboid.yLength(), sizeMaxY);
        }
        int sizeMaxZ = Configuration.getPlayerLimitation(associatedPlayer).getWorldSettings(associatedWorld).sizeMaxZ;
        if (sizeMaxZ > 0 && cuboid.zLength() > sizeMaxZ) {
            throw new DominionException(Language.assertsText.zLengthTooLong, cuboid.zLength(), sizeMaxZ);
        }

        int yMax = Configuration.getPlayerLimitation(associatedPlayer).getWorldSettings(associatedWorld).noHigherThan;
        if (cuboid.y2() > yMax) {
            throw new DominionException(Language.assertsText.yTooHigh, cuboid.y2(), yMax);
        }
        int yMin = Configuration.getPlayerLimitation(associatedPlayer).getWorldSettings(associatedWorld).noLowerThan;
        if (cuboid.y1() < yMin) {
            throw new DominionException(Language.assertsText.yTooLow, cuboid.y1(), yMin);
        }
    }

    /**
     * Asserts that the given player is the owner of the specified dominion.
     * Server operators are allowed to bypass this check.
     *
     * @param associatedPlayer the player to check
     * @param dominion         the dominion to check ownership of
     * @throws DominionException if the player is not the owner of the dominion
     */
    public static void assertDominionOwner(@NotNull Player associatedPlayer, @NotNull DominionDTO dominion) throws DominionException {
        if (bypassLimit(associatedPlayer)) {
            return;
        }
        if (!dominion.getOwner().equals(associatedPlayer.getUniqueId())) {
            throw new DominionException(Language.assertsText.notOwner, dominion.getName());
        }
    }

    /**
     * Asserts that the given command sender is the owner of the specified dominion.
     * If the sender is a player, it delegates the check to the player-specific method.
     * Server operators are allowed to bypass this check.
     *
     * @param sender   the command sender to check
     * @param dominion the dominion to check ownership of
     * @throws DominionException if the sender is a player and is not the owner of the dominion
     */
    public static void assertDominionOwner(@NotNull CommandSender sender, @NotNull DominionDTO dominion) throws DominionException {
        if (sender instanceof Player player) {
            assertDominionOwner(player, dominion);
        }
    }

    /**
     * Checks if the given player is an admin of the specified dominion.
     * <p>
     * Returns true if the player is a server operator (bypass limit), the owner of the dominion,
     * or a member with the admin flag in their group or as an individual.
     * Returns false if the player is not a member or does not have admin privileges.
     *
     * @param associatedPlayer the player to check
     * @param dominion         the dominion to check admin status of
     * @return true if the player is an admin of the dominion, false otherwise
     */
    public static boolean checkDominionAdmin(@NotNull Player associatedPlayer, @NotNull DominionDTO dominion) {
        if (bypassLimit(associatedPlayer)) {
            return true;
        }
        if (dominion.getOwner().equals(associatedPlayer.getUniqueId())) {
            return true;
        }
        MemberDTO member = CacheManager.instance.getMember(dominion, associatedPlayer);
        if (member == null) {
            return false; // not a member of the dominion
        }
        GroupDTO group = CacheManager.instance.getGroup(member.getGroupId());
        if (group != null && group.getFlagValue(Flags.ADMIN)) {
            return true;
        }
        return member.getFlagValue(Flags.ADMIN);
    }

    /**
     * Asserts that the given player is an admin of the specified dominion.
     * Server operators are allowed to bypass this check.
     *
     * @param associatedPlayer the player to check
     * @param dominion         the dominion to check admin status of
     * @throws DominionException if the player is not an admin of the dominion
     */
    public static void assertDominionAdmin(@NotNull Player associatedPlayer, @NotNull DominionDTO dominion) throws DominionException {
        if (checkDominionAdmin(associatedPlayer, dominion)) {
            return;
        }
        throw new DominionException(Language.assertsText.notAdmin, dominion.getName());
    }

    public static void assertDominionAdmin(@NotNull CommandSender sender, @NotNull DominionDTO dominion) throws DominionException {
        if (sender instanceof Player) {
            assertDominionAdmin((Player) sender, dominion);
        }
    }

    /**
     * Asserts that the parent dominion can contain the specified dominion.
     *
     * @param operator the command operator (usually a player)
     * @param dominion the dominion to check
     * @param cuboid   the cuboid representing the dominion's size
     * @throws DominionException if the parent dominion cannot contain the specified dominion,
     *                           if the dominion cannot contain its children, or if the sub-dominion recursion depth is invalid
     */
    public static void assertWithinParent(@NotNull CommandSender operator, @NotNull DominionDTO dominion, @NotNull CuboidDTO cuboid) throws Exception {
        if (!(operator instanceof Player associatedPlayer)) {
            return;
        }
        // check if parent dominion can contain this dominion
        if (dominion.getParentDomId() != -1) {
            DominionDTO parent = DominionDOO.select(dominion.getParentDomId()); // get from db instead of cache to avoid cache miss
            if (parent == null) {
                throw new DominionException(Language.assertsText.missingParentDom, dominion.getName());
            }
            if (!parent.getCuboid().contain(cuboid,
                    Configuration.getPlayerLimitation(associatedPlayer).getWorldSettings(dominion.getWorldUid()).autoIncludeVertical)) {
                throw new DominionException(Language.assertsText.outsideOfParentDom, dominion.getName(), parent.getName());
            }
        }
    }

    /**
     * Asserts that the sub-dominion depth is within the allowed limit.
     * This method checks if the recursion depth of sub-dominions is valid
     * based on the player's limitations and the configuration settings.
     *
     * @param operator the command operator (usually a player)
     * @param dominion the dominion to check
     * @throws DominionException if the sub-dominion depth exceeds the allowed limit
     */
    public static void assertSubDepth(@NotNull CommandSender operator, @NotNull DominionDTO dominion) throws DominionException {
        if (!(operator instanceof Player associatedPlayer)) {
            return;
        }
        // check if sub recursion is valid
        if (bypassLimit(associatedPlayer)) {
            return;
        }
        int limitDepth = Configuration.getPlayerLimitation(associatedPlayer).getWorldSettings(dominion.getWorldUid()).maxSubDominionDepth;
        if (limitDepth == -1) {
            return;
        }
        int level = 0;
        DominionDTO parent = dominion;
        while (parent.getParentDomId() != -1) {
            parent = CacheManager.instance.getDominion(parent.getParentDomId());
            if (parent == null) {
                throw new DominionException(Language.assertsText.missingParentDom, dominion.getName());
            }
            level++;
        }
        if (level > limitDepth) {
            throw new DominionException(Language.assertsText.subDomTooDeep, associatedPlayer.getName(), limitDepth);
        }
    }

    /**
     * Asserts that the given dominion can contain its child dominions.
     * This method checks if the cuboid representing the dominion's size
     * can contain the cuboids of all its child dominions.
     *
     * @param operator the command operator (usually a player)
     * @param dominion the dominion to check
     * @param cuboid   the cuboid representing the dominion's size
     * @throws DominionException if the dominion cannot contain its child dominions
     */
    public static void assertContainSubs(@NotNull CommandSender operator, @NotNull DominionDTO dominion, @NotNull CuboidDTO cuboid) throws DominionException {
        if (!(operator instanceof Player associatedPlayer)) {
            return;
        }
        // check if dominion can contain children
        List<DominionDTO> children = CacheManager.instance.getCache().getDominionCache().getChildrenOf(dominion.getId());
        for (DominionDTO child : children) {
            if (!cuboid.contain(child.getCuboid(),
                    Configuration.getPlayerLimitation(associatedPlayer).getWorldSettings(dominion.getWorldUid()).autoIncludeVertical)) {
                throw new DominionException(Language.assertsText.cantContainChild, dominion.getName(), child.getName());
            }
        }
    }

    /**
     * Asserts that the specified dominion does not intersect with other dominions or the spawn protection area.
     *
     * @param operator the command operator (usually a player)
     * @param dominion the dominion to check for intersections
     * @throws DominionException if the dominion intersects with another dominion or the spawn protection area
     */
    public static void assertDominionIntersect(@NotNull CommandSender operator, @NotNull DominionDTO dominion, @NotNull CuboidDTO cuboid) throws DominionException {
        List<DominionDTO> dominions = CacheManager.instance.getCache().getDominionCache().getChildrenOf(dominion.getParentDomId());
        for (DominionDTO dom : dominions) {
            if (dom.getId().equals(dominion.getId())) {
                continue;
            }
            if (!dom.getWorldUid().equals(dominion.getWorldUid())) {
                continue;
            }
            if (cuboid.intersectWith(dom.getCuboid())) {
                throw new DominionException(Language.assertsText.intersectWithDom, dominion.getName(), dom.getName());
            }
        }
        if (!(operator instanceof Player associatedPlayer)) {
            return;
        }
        if (bypassLimit(associatedPlayer)) {
            return;
        }
        int spawnProtection = Configuration.serverSpawnProtectionRadius;
        if (spawnProtection == -1) {
            return;
        }
        World world = dominion.getWorld();
        if (world == null) {
            return;
        }
        Location spawn = world.getSpawnLocation();
        CuboidDTO spawnCuboid = new CuboidDTO(spawn.getBlockX() - spawnProtection, spawn.getBlockX() + spawnProtection,
                spawn.getBlockY() - spawnProtection, spawn.getBlockY() + spawnProtection,
                spawn.getBlockZ() - spawnProtection, spawn.getBlockZ() + spawnProtection);
        if (cuboid.intersectWith(spawnCuboid)) {
            throw new DominionException(Language.assertsText.intersectWithSpawn, dominion.getName());
        }
        if (HooksManager.isConflictWithWorldGuard(cuboid, world)) {
            throw new DominionException(Language.assertsText.intersectWithDom, dominion.getName(), "WorldGuard");
        }
    }

    /**
     * Asserts the economic conditions for the given player when modifying a dominion.
     * This method checks if the economy feature is enabled, calculates the cost or refund
     * based on the difference between the before and after cuboid sizes, and updates the player's balance accordingly.
     *
     * @param operator the command sender (usually a player)
     * @param before   the cuboid representing the dominion's size before modification
     * @param after    the cuboid representing the dominion's size after modification
     * @throws Exception if there is an error during the economic transaction
     */
    public static void assertEconomy(@NotNull CommandSender operator, CuboidDTO before, CuboidDTO after) throws Exception {
        if (!(operator instanceof Player associatedPlayer)) {
            // do nothing if command sender is not a player
            return;
        }
        Limitation.Economy ecoConf = Configuration.getPlayerLimitation(associatedPlayer).economy;
        if (!ecoConf.enable) {
            return;
        }
        if (bypassLimit(associatedPlayer)) {
            return;
        }
        long amount;
        if (ecoConf.squareOnly) {
            amount = after.minusSquareWith(before);
        } else {
            amount = after.minusVolumeWith(before);
        }
        if (amount == 0) {
            return;
        }
        double price = amount * ecoConf.pricePerBlock;
        if (price > 0) {
            VaultConnect.instance.withdrawPlayer(associatedPlayer, price);
            Notification.info(associatedPlayer, Language.assertsText.withDrawMoney, price);
        } else {
            price = price * ecoConf.refundRate * -1;
            VaultConnect.instance.depositPlayer(associatedPlayer, price);
            Notification.info(associatedPlayer, Language.assertsText.depositMoney, price);
        }
    }

    public static void assertGroupBelongDominion(@NotNull GroupDTO group, @NotNull DominionDTO dominion) throws DominionException {
        if (!group.getDomID().equals(dominion.getId())) {
            throw new DominionException(Language.assertsText.groupNotBelongDominion, group.getNamePlain(), dominion.getName());
        }
    }

    public static void assertMemberBelongDominion(@NotNull MemberDTO member, @NotNull DominionDTO dominion) throws DominionException {
        if (!member.getDomID().equals(dominion.getId())) {
            throw new DominionException(Language.assertsText.groupNotBelongDominion, member.getPlayer().getLastKnownName(), dominion.getName());
        }
    }
}
