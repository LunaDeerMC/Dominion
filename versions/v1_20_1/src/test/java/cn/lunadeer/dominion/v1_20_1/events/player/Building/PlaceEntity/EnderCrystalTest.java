package cn.lunadeer.dominion.v1_20_1.events.player.Building.PlaceEntity;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.misc.Others;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EnderCrystalTest {
    private final EnderCrystal listener = new EnderCrystal();
    private MockedStatic<Others> others;
    private Player player;
    private Block base;
    private org.bukkit.entity.EnderCrystal crystal;
    private World world;
    private Location crystalLocation;

    @BeforeEach
    void setUp() {
        others = mockStatic(Others.class);
        player = mock(Player.class);
        base = mock(Block.class);
        crystal = mock(org.bukkit.entity.EnderCrystal.class);
        world = mock(World.class);
        crystalLocation = new Location(world, 0.5, 65, 3.5);
        when(crystal.getLocation()).thenReturn(crystalLocation);
        // At a vertical claim boundary the crystal and its supporting block can have different owners.
        when(base.getLocation()).thenReturn(new Location(world, 0, 64, 3));
        when(player.getLocation()).thenReturn(new Location(world, 0, 65, 0));
    }

    @AfterEach
    void tearDown() {
        others.close();
    }

    @ParameterizedTest
    @CsvSource({
            "BEDROCK, HAND, false", "BEDROCK, OFF_HAND, false",
            "OBSIDIAN, HAND, false", "OBSIDIAN, OFF_HAND, false",
            "BEDROCK, HAND, true", "BEDROCK, OFF_HAND, true",
            "OBSIDIAN, HAND, true", "OBSIDIAN, OFF_HAND, true"
    })
    void checksCrystalPermissionAtTheEntityPositionForEitherHand(Material material, EquipmentSlot hand, boolean allowed) {
        when(base.getType()).thenReturn(material);
        EntityPlaceEvent event = placement(crystal, player, hand);
        permission(event, allowed);

        listener.handler(event);

        assertEquals(!allowed, event.isCancelled());
        verifyPermission(event);
        // Placement permission must not depend on the player's location, inventory, or the base block's claim.
        verifyNoInteractions(player, base);
    }

    @ParameterizedTest
    @EnumSource(value = World.Environment.class, names = {"NORMAL", "NETHER", "THE_END"})
    void protectsCrystalsInEveryDimension(World.Environment environment) {
        when(world.getEnvironment()).thenReturn(environment);
        EntityPlaceEvent event = placement(crystal, player, EquipmentSlot.HAND);
        permission(event, false);

        listener.handler(event);

        assertTrue(event.isCancelled());
        verifyPermission(event);
    }

    @Test
    void preservesCancellationWithoutRecheckingPermission() {
        EntityPlaceEvent event = placement(crystal, player, EquipmentSlot.OFF_HAND);
        event.setCancelled(true);

        listener.handler(event);

        assertTrue(event.isCancelled());
        others.verifyNoInteractions();
    }

    @Test
    void ignoresPlacementsWithoutAPlayer() {
        EntityPlaceEvent event = placement(crystal, null, EquipmentSlot.HAND);

        listener.handler(event);

        assertFalse(event.isCancelled());
        others.verifyNoInteractions();
    }

    @Test
    void leavesOtherEntitiesToTheirOwnPlacementPermissions() {
        for (Entity entity : new Entity[]{mock(ArmorStand.class), mock(Vehicle.class), mock(Entity.class)}) {
            EntityPlaceEvent event = placement(entity, player, EquipmentSlot.HAND);

            listener.handler(event);

            assertFalse(event.isCancelled());
        }
        others.verifyNoInteractions();
    }

    @Test
    void checksPlacementAtTheEarliestPriority() throws Exception {
        EventHandler handler = EnderCrystal.class.getMethod("handler", EntityPlaceEvent.class)
                .getAnnotation(EventHandler.class);
        assertEquals(EventPriority.LOWEST, handler.priority());
    }

    private EntityPlaceEvent placement(Entity entity, Player actor, EquipmentSlot hand) {
        return new EntityPlaceEvent(entity, actor, base, BlockFace.UP, hand);
    }

    private void permission(EntityPlaceEvent event, boolean allowed) {
        others.when(() -> Others.checkPrivilegeFlag(crystalLocation, Flags.PLACE_ENDER_CRYSTAL, player, event))
                .thenAnswer(invocation -> {
                    if (!allowed) event.setCancelled(true);
                    return allowed;
                });
    }

    private void verifyPermission(EntityPlaceEvent event) {
        others.verify(() -> Others.checkPrivilegeFlag(crystalLocation, Flags.PLACE_ENDER_CRYSTAL, player, event));
        others.verifyNoMoreInteractions();
    }
}
