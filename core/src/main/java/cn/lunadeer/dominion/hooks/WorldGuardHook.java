package cn.lunadeer.dominion.hooks;

import cn.lunadeer.dominion.api.dtos.CuboidDTO;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class WorldGuardHook {
    public final static StateFlag CLAIMING = new StateFlag("dominion-claim", false);

    public static boolean isConflict(@NotNull CuboidDTO cuboid, @NotNull World world) {
        final Optional<RegionManager> regionManager = getRegionManager(world);
        if (regionManager.isEmpty()) {
            return false;
        }
        final ApplicableRegionSet set = getOverlappingRegions(cuboid, regionManager.get(), world);
        return set.queryState(null, CLAIMING) == StateFlag.State.DENY;
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
