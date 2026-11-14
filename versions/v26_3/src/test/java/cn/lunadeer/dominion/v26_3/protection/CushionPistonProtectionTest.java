package cn.lunadeer.dominion.v26_3.protection;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.misc.Others;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Cushion;
import org.bukkit.entity.Entity;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.VoxelShape;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CushionPistonProtectionTest {
    private static final int X = 100008, Y = 80, Z = 100008;
    private static final List<BlockFace> FACES = List.of(BlockFace.EAST, BlockFace.WEST,
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.UP, BlockFace.DOWN);

    static Stream<Arguments> emptyShapes() {
        return FACES.stream().flatMap(face -> Stream.of(true, false).flatMap(extending ->
                Stream.of(Material.AIR, Material.WATER, Material.LAVA)
                        .map(material -> Arguments.of(face, extending, material))));
    }

    static Stream<Arguments> directions() {
        return FACES.stream().flatMap(face -> Stream.of(true, false)
                .map(extending -> Arguments.of(face, extending)));
    }

    @ParameterizedTest
    @MethodSource("emptyShapes")
    void emptyItemsNeverQueryOriginAndDoNotSkipLaterBlocks(BlockFace facing, boolean extending,
                                                         Material material) {
        Fixture f = new Fixture(facing, extending);
        Block empty = f.block(material, X + 3, Y, Z);
        emptyShape(empty);
        Block solid = f.block(Material.STONE, X + 4, Y, Z);

        f.check(List.of(empty, solid));

        assertEquals(2, f.queries.size(), "Only the piston head and the later solid block are queried");
        assertTrue(f.queries.stream().allMatch(box -> box.getMinX() > X - 2 && box.getMinZ() > Z - 2),
                "No empty shape may be expanded into a query at the world origin");
        assertFalse(f.event.isCancelled());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void emptyItemDoesNotPreventProtectingLaterCushion(boolean allow) {
        Fixture f = new Fixture(BlockFace.EAST, true);
        Block empty = f.block(Material.WATER, X + 3, Y, Z);
        emptyShape(empty);
        Block solid = f.block(Material.STONE, X + 4, Y, Z);
        Block later = f.block(Material.STONE, X + 8, Y, Z);
        Cushion cushion = f.cushion(new BoundingBox(X + 5.1, Y, Z + .1, X + 5.9, Y + .25, Z + .9));
        Location location = cushion.getLocation();

        try (MockedStatic<Others> others = mockStatic(Others.class)) {
            others.when(() -> Others.checkEnvironmentFlag(location, Flags.CUSHION_ENVIRONMENT_BREAK, f.event))
                    .thenAnswer(invocation -> {
                        if (!allow) f.event.setCancelled(true);
                        return allow;
                    });

            f.check(List.of(empty, solid, later));

            assertEquals(!allow, f.event.isCancelled());
            assertEquals(allow ? 3 : 2, f.queries.size(), "A denied cushion stops later checks only after cancellation");
            others.verify(() -> Others.checkEnvironmentFlag(location, Flags.CUSHION_ENVIRONMENT_BREAK, f.event));
            others.verifyNoMoreInteractions();
        }
    }

    @Test
    void destroyedWaterDoesNotSweepIntoUnaffectedCushionBehindIt() {
        Fixture f = new Fixture(BlockFace.EAST, true);
        Block water = f.block(Material.WATER, X + 1, Y, Z);
        emptyShape(water);
        f.cushion(new BoundingBox(X + 2.1, Y, Z + .1, X + 2.9, Y + .25, Z + .9));

        try (MockedStatic<Others> others = mockStatic(Others.class)) {
            f.check(List.of(water));

            assertEquals(1, f.queries.size(), "Water must not acquire a fictitious unit-block sweep");
            assertFalse(f.event.isCancelled());
            others.verifyNoInteractions();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void pistonHeadStillProtectsCushionWithNoMovedBlocks(boolean extending) {
        Fixture f = new Fixture(BlockFace.EAST, extending);
        Cushion cushion = f.cushion(new BoundingBox(X + 1.1, Y, Z + .1, X + 1.9, Y + .25, Z + .9));
        Location location = cushion.getLocation();

        try (MockedStatic<Others> others = mockStatic(Others.class)) {
            others.when(() -> Others.checkEnvironmentFlag(location, Flags.CUSHION_ENVIRONMENT_BREAK, f.event))
                    .thenAnswer(invocation -> {
                        f.event.setCancelled(true);
                        return false;
                    });

            f.check(List.of());

            assertTrue(f.event.isCancelled());
            assertEquals(1, f.queries.size());
            assertBox(f.queries.getFirst(), X, Y, Z, X + 2, Y + 1, Z + 1);
            others.verify(() -> Others.checkEnvironmentFlag(location, Flags.CUSHION_ENVIRONMENT_BREAK, f.event));
        }
    }

    @Test
    void nonemptySlabKeepsItsOriginalHeightAndDoesNotReadCollisionFallback() {
        Fixture f = new Fixture(BlockFace.EAST, true);
        Block slab = f.block(Material.OAK_SLAB, X + 4, Y, Z);
        BoundingBox original = new BoundingBox(X + 4, Y, Z, X + 5, Y + .5, Z + 1);
        when(slab.getBoundingBox()).thenReturn(original);

        f.check(List.of(slab));

        assertEquals(2, f.queries.size());
        assertBox(f.queries.getLast(), X + 3.99, Y - .01, Z - .01, X + 6.01, Y + .51, Z + 1.01);
        assertBox(original, X + 4, Y, Z, X + 5, Y + .5, Z + 1);
        verify(slab, never()).getCollisionShape();
    }

    @ParameterizedTest
    @MethodSource("directions")
    void honeyOnlyAddsCarryHeightDuringHorizontalMovement(BlockFace facing, boolean extending) {
        Fixture f = new Fixture(facing, extending);
        Block honey = f.block(Material.HONEY_BLOCK, X + 4, Y, Z);
        BoundingBox original = new BoundingBox(X + 4.0625, Y, Z + .0625, X + 4.9375, Y + .9375, Z + .9375);
        when(honey.getBoundingBox()).thenReturn(original);
        BoundingBox before = original.clone();
        BlockFace movement = extending ? facing : facing.getOppositeFace();

        f.check(List.of(honey));

        int dx = movement.getModX(), dy = movement.getModY(), dz = movement.getModZ();
        assertBox(f.queries.getLast(),
                X + 4.0625 + Math.min(0, dx) - .01, Y + Math.min(0, dy) - .01,
                Z + .0625 + Math.min(0, dz) - .01,
                X + 4.9375 + Math.max(0, dx) + .01,
                Y + .9375 + Math.max(0, dy) + (dy == 0 ? .5 : 0) + .01,
                Z + .9375 + Math.max(0, dz) + .01);
        assertEquals(before, original, "Sweep calculation must not mutate the block's bounding box");
    }

    @Test
    void emptyOutlineWithCollisionUsesWorldCoordinatesWithoutMutatingLocalBoxes() {
        Fixture f = new Fixture(BlockFace.EAST, true);
        Block block = f.block(Material.STONE, -1234, 67, -5678);
        when(block.getBoundingBox()).thenReturn(new BoundingBox());
        BoundingBox first = new BoundingBox(.1, .2, .3, .4, .5, .6);
        BoundingBox second = new BoundingBox(.6, .4, .5, .9, .8, .95);
        BoundingBox firstBefore = first.clone(), secondBefore = second.clone();
        VoxelShape collision = mock(VoxelShape.class);
        when(collision.getBoundingBoxes()).thenReturn(List.of(new BoundingBox(), first, second,
                new BoundingBox(20, 20, 20, 20, 21, 21)));
        when(block.getCollisionShape()).thenReturn(collision);

        f.check(List.of(block));

        assertEquals(2, f.queries.size());
        assertBox(f.queries.getLast(), -1233.91, 67.19, -5677.71, -1232.09, 67.81, -5677.04);
        assertEquals(firstBefore, first);
        assertEquals(secondBefore, second);
        verify(block).getCollisionShape();
    }

    @Test
    void degenerateCollisionBoxesDoNotProduceAnEntityQuery() {
        Fixture f = new Fixture(BlockFace.EAST, true);
        Block block = f.block(Material.AIR, X + 4, Y, Z);
        when(block.getBoundingBox()).thenReturn(new BoundingBox());
        VoxelShape collision = mock(VoxelShape.class);
        when(collision.getBoundingBoxes()).thenReturn(List.of(new BoundingBox(),
                new BoundingBox(.1, .2, .3, .9, .2, .8)));
        when(block.getCollisionShape()).thenReturn(collision);

        f.check(List.of(block));

        assertEquals(1, f.queries.size(), "Degenerate fallback boxes must not be expanded into physical volume");
    }

    private static void emptyShape(Block block) {
        when(block.getBoundingBox()).thenReturn(new BoundingBox());
        VoxelShape collision = mock(VoxelShape.class);
        when(collision.getBoundingBoxes()).thenReturn(List.of());
        when(block.getCollisionShape()).thenReturn(collision);
    }

    private static void assertBox(BoundingBox actual, double minX, double minY, double minZ,
                                  double maxX, double maxY, double maxZ) {
        assertAll(() -> assertEquals(minX, actual.getMinX(), 1e-8),
                () -> assertEquals(minY, actual.getMinY(), 1e-8),
                () -> assertEquals(minZ, actual.getMinZ(), 1e-8),
                () -> assertEquals(maxX, actual.getMaxX(), 1e-8),
                () -> assertEquals(maxY, actual.getMaxY(), 1e-8),
                () -> assertEquals(maxZ, actual.getMaxZ(), 1e-8));
    }

    private static final class Fixture {
        final World world = mock(World.class);
        final List<BoundingBox> queries = new ArrayList<>();
        final List<Entity> entities = new ArrayList<>();
        final boolean extending;
        final BlockPistonEvent event;

        Fixture(BlockFace facing, boolean extending) {
            this.extending = extending;
            Block piston = block(Material.STICKY_PISTON, X, Y, Z);
            Directional data = mock(Directional.class);
            when(data.getFacing()).thenReturn(facing);
            when(piston.getBlockData()).thenReturn(data);
            Block head = block(Material.AIR,
                    X + facing.getModX(), Y + facing.getModY(), Z + facing.getModZ());
            when(piston.getRelative(facing)).thenReturn(head);
            event = extending ? new BlockPistonExtendEvent(piston, List.of(), facing)
                    : new BlockPistonRetractEvent(piston, List.of(), facing);
            when(world.getNearbyEntities(any(BoundingBox.class), any())).thenAnswer(invocation -> {
                BoundingBox area = invocation.getArgument(0);
                Predicate<Entity> filter = invocation.getArgument(1);
                queries.add(area.clone());
                return entities.stream().filter(entity -> filter.test(entity)
                        && area.overlaps(entity.getBoundingBox())).toList();
            });
        }

        Block block(Material type, int x, int y, int z) {
            Block block = mock(Block.class);
            when(block.getWorld()).thenReturn(world);
            when(block.getX()).thenReturn(x);
            when(block.getY()).thenReturn(y);
            when(block.getZ()).thenReturn(z);
            when(block.getType()).thenReturn(type);
            when(block.getBoundingBox()).thenReturn(new BoundingBox(x, y, z, x + 1, y + 1, z + 1));
            return block;
        }

        Cushion cushion(BoundingBox box) {
            Cushion cushion = mock(Cushion.class);
            when(cushion.getBoundingBox()).thenReturn(box);
            when(cushion.getLocation()).thenReturn(new Location(world, box.getCenterX(), box.getMinY(), box.getCenterZ()));
            entities.add(cushion);
            return cushion;
        }

        void check(List<Block> blocks) {
            CushionPistonProtection.check(event, blocks, extending);
        }
    }
}
