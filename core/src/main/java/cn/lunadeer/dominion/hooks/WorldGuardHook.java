package cn.lunadeer.dominion.hooks;

import cn.lunadeer.dominion.api.dtos.CuboidDTO;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WorldGuardHook {
    static final String CLAIMING_FLAG_NAME = "dominion-claim";
    private static StateFlag claimingFlag;

    record FlagRegistration(StateFlag flag, boolean newlyRegistered) {
    }

    /**
     * Registers Dominion's custom flag before WorldGuard enables and locks its registry.
     *
     * @param logger logger available during the plugin load phase
     */
    public static void registerFlag(@NotNull Logger logger) {
        claimingFlag = null;
        try {
            FlagRegistration registration = registerFlag(WorldGuard.getInstance().getFlagRegistry());
            claimingFlag = registration.flag();
            if (registration.newlyRegistered()) {
                logger.info("Registered WorldGuard custom flag: " + CLAIMING_FLAG_NAME);
            } else {
                logger.info("Reusing existing WorldGuard custom flag: " + CLAIMING_FLAG_NAME);
            }
        } catch (RuntimeException e) {
            logger.log(Level.WARNING,
                    "Failed to register WorldGuard custom flag '" + CLAIMING_FLAG_NAME
                            + "'; WorldGuard claim checks will be disabled.",
                    e);
        }
    }

    static FlagRegistration registerFlag(@NotNull FlagRegistry registry) {
        Flag<?> existing = registry.get(CLAIMING_FLAG_NAME);
        if (existing != null) {
            return reuseExistingFlag(existing);
        }

        StateFlag flag = new StateFlag(CLAIMING_FLAG_NAME, false);
        try {
            registry.register(flag);
            return new FlagRegistration(flag, true);
        } catch (FlagConflictException e) {
            // Another plugin may have registered the flag between get() and register().
            Flag<?> conflicting = registry.get(CLAIMING_FLAG_NAME);
            if (conflicting == null) {
                throw new IllegalStateException("WorldGuard reported a flag conflict but no flag was registered", e);
            }
            return reuseExistingFlag(conflicting);
        }
    }

    private static FlagRegistration reuseExistingFlag(@NotNull Flag<?> existing) {
        if (existing instanceof StateFlag stateFlag) {
            return new FlagRegistration(stateFlag, false);
        }
        throw new IllegalStateException(
                "WorldGuard flag '" + CLAIMING_FLAG_NAME + "' is already registered as "
                        + existing.getClass().getSimpleName() + " instead of StateFlag"
        );
    }

    public static boolean isConflict(@NotNull CuboidDTO cuboid, @NotNull World world) {
        StateFlag flag = claimingFlag;
        if (flag == null) {
            return false;
        }
        final Optional<RegionManager> regionManager = getRegionManager(world);
        if (regionManager.isEmpty()) {
            return false;
        }
        final ApplicableRegionSet set = getOverlappingRegions(cuboid, regionManager.get(), world);
        return set.queryState(null, flag) == StateFlag.State.DENY;
    }

    @NotNull
    private static ApplicableRegionSet getOverlappingRegions(@NotNull CuboidDTO cuboid, @NotNull RegionManager manager,
                                                             @NotNull org.bukkit.World bukkitWorld) {
        return manager.getApplicableRegions(new ProtectedCuboidRegion(
                "dummy",
                BlockVector3.at(cuboid.x1(), bukkitWorld.getMinHeight(), cuboid.z1()),
                BlockVector3.at(cuboid.x2(), bukkitWorld.getMaxHeight(), cuboid.z2())
        ));
    }

    private static Optional<RegionManager> getRegionManager(@NotNull org.bukkit.World world) {
        return Optional.ofNullable(
                WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world))
        );
    }
}
