package cn.lunadeer.dominion.v1_20_1.events.environment.Explosions;

import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.misc.Others;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockExplodeEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class BlockExplodeTest {
    @BeforeAll
    static void initializeBedTagWithoutAServer() {
        Tag<Material> beds = new Tag<>() {
            @Override
            public boolean isTagged(Material material) {
                return getValues().contains(material);
            }

            @Override
            public Set<Material> getValues() {
                return Set.of(Material.RED_BED, Material.BLUE_BED);
            }

            @Override
            public NamespacedKey getKey() {
                return NamespacedKey.minecraft("beds");
            }
        };
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getTag("blocks", NamespacedKey.minecraft("beds"), Material.class))
                    .thenReturn(beds);
            assertSame(beds, Tag.BEDS);
        }
    }

    @ParameterizedTest
    @EnumSource(value = Material.class, names = {"RESPAWN_ANCHOR", "RED_BED", "BLUE_BED"})
    void filtersByCapturedSourceAndEachAffectedLocation(Material sourceType) {
        Block protectedBlock = blockAt(0);
        Block allowedBlock = blockAt(20);
        Block outsideBlock = blockAt(-2);
        BlockExplodeEvent event = explosion(sourceType, protectedBlock, allowedBlock, outsideBlock);
        EnvFlag sourceFlag = sourceType == Material.RESPAWN_ANCHOR ? Flags.ANCHOR_EXPLODE : Flags.BED_EXPLODE;
        EnvFlag otherFlag = sourceType == Material.RESPAWN_ANCHOR ? Flags.BED_EXPLODE : Flags.ANCHOR_EXPLODE;

        // The source has already disappeared when Bukkit delivers the explosion event.
        assertEquals(Material.AIR, event.getBlock().getType());
        try (MockedStatic<Others> others = mockStatic(Others.class)) {
            allow(others, protectedBlock, sourceFlag, false);
            allow(others, protectedBlock, otherFlag, true);
            allow(others, allowedBlock, sourceFlag, true);
            allow(others, allowedBlock, otherFlag, false);
            allow(others, outsideBlock, sourceFlag, true);
            allow(others, outsideBlock, otherFlag, false);

            new BlockExplode().handle(event);

            assertEquals(List.of(allowedBlock, outsideBlock), event.blockList());
            assertFalse(event.isCancelled());
            assertEquals(0.25f, event.getYield());
            others.verify(() -> Others.checkEnvironmentFlag(protectedBlock.getLocation(), sourceFlag, null));
            others.verify(() -> Others.checkEnvironmentFlag(allowedBlock.getLocation(), sourceFlag, null));
            others.verify(() -> Others.checkEnvironmentFlag(outsideBlock.getLocation(), sourceFlag, null));
            others.verifyNoMoreInteractions();
        }
    }

    @ParameterizedTest
    @CsvSource({"false, false", "false, true", "true, false", "true, true"})
    void legacyApiRequiresBothFlagsAtEachAffectedLocation(boolean bedAllowed, boolean anchorAllowed) {
        Block insideBlock = blockAt(0);
        Block outsideBlock = blockAt(-2);
        BlockExplodeEvent event = new BlockExplodeEvent(
                airSource(), new ArrayList<>(List.of(insideBlock, outsideBlock)), 0.25f, null) {
            @Override
            public BlockState getExplodedBlockState() {
                throw new NoSuchMethodError("Old Spigot has no exploded block snapshot API");
            }
        };

        try (MockedStatic<Others> others = mockStatic(Others.class)) {
            allow(others, insideBlock, Flags.BED_EXPLODE, bedAllowed);
            allow(others, insideBlock, Flags.ANCHOR_EXPLODE, anchorAllowed);
            allow(others, outsideBlock, Flags.BED_EXPLODE, true);
            allow(others, outsideBlock, Flags.ANCHOR_EXPLODE, true);

            new BlockExplode().handle(event);

            assertEquals(bedAllowed && anchorAllowed ? List.of(insideBlock, outsideBlock) : List.of(outsideBlock),
                    event.blockList());
            assertFalse(event.isCancelled());
            assertEquals(0.25f, event.getYield());
        }
    }

    @Test
    void leavesCancelledEventsUntouched() {
        Block affectedBlock = blockAt(0);
        BlockExplodeEvent event = explosion(Material.RESPAWN_ANCHOR, affectedBlock);
        event.setCancelled(true);

        try (MockedStatic<Others> others = mockStatic(Others.class)) {
            new BlockExplode().handle(event);

            assertEquals(List.of(affectedBlock), event.blockList());
            assertTrue(event.isCancelled());
            others.verifyNoInteractions();
        }
    }

    @ParameterizedTest
    @NullSource
    @EnumSource(value = Material.class, names = {"AIR", "STONE", "TNT"})
    void leavesAbsentOrUnrelatedSnapshotsUntouched(Material sourceType) {
        Block affectedBlock = blockAt(0);
        BlockExplodeEvent event = explosion(sourceType, affectedBlock);

        try (MockedStatic<Others> others = mockStatic(Others.class)) {
            new BlockExplode().handle(event);

            assertEquals(List.of(affectedBlock), event.blockList());
            assertFalse(event.isCancelled());
            others.verifyNoInteractions();
        }
    }

    private static BlockExplodeEvent explosion(Material sourceType, Block... affectedBlocks) {
        BlockState snapshot = null;
        if (sourceType != null) {
            snapshot = mock(BlockState.class);
            when(snapshot.getType()).thenReturn(sourceType);
        }
        return new BlockExplodeEvent(airSource(), new ArrayList<>(List.of(affectedBlocks)), 0.25f, snapshot);
    }

    private static Block airSource() {
        Block source = blockAt(-1);
        when(source.getType()).thenReturn(Material.AIR);
        return source;
    }

    private static Block blockAt(int x) {
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(new Location(null, x, 64, 0));
        return block;
    }

    private static void allow(MockedStatic<Others> others, Block block, EnvFlag flag, boolean allowed) {
        others.when(() -> Others.checkEnvironmentFlag(block.getLocation(), flag, null)).thenReturn(allowed);
    }
}
