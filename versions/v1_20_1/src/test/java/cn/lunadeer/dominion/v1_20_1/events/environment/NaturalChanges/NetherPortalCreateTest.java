package cn.lunadeer.dominion.v1_20_1.events.environment.NaturalChanges;

import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.WorldWide;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.world.PortalCreateEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class NetherPortalCreateTest {
    private final NetherPortalCreate listener = new NetherPortalCreate();
    private CacheManager previousCache;
    private CacheManager cache;
    private World destination;
    private MockedStatic<WorldWide> worldWide;
    private boolean previousEnable;

    @BeforeEach
    void setUp() {
        previousCache = CacheManager.instance;
        cache = mock(CacheManager.class);
        CacheManager.instance = cache;
        destination = mock(World.class);
        worldWide = mockStatic(WorldWide.class);
        previousEnable = Flags.NETHER_PORTAL_CREATE.getEnable();
        Flags.NETHER_PORTAL_CREATE.setEnable(true);
    }

    @AfterEach
    void tearDown() {
        Flags.NETHER_PORTAL_CREATE.setEnable(previousEnable);
        CacheManager.instance = previousCache;
        worldWide.close();
    }

    @Test
    void cancelsWholePortalWhenALaterDestinationBlockCrossesIntoAProtectedDominion() {
        Player player = mock(Player.class);
        World origin = mock(World.class);
        when(player.getLocation()).thenReturn(new Location(origin, 128, 64, 128));
        BlockState outside = blockAt(0, Material.NETHER_PORTAL);
        BlockState protectedFrame = blockAt(1, Material.OBSIDIAN);
        BlockState remaining = blockAt(2, Material.NETHER_PORTAL);
        allowInDominion(protectedFrame, false);
        PortalCreateEvent event = portal(PortalCreateEvent.CreateReason.NETHER_PAIR,
                player, outside, protectedFrame, remaining);

        listener.handler(event);

        assertTrue(event.isCancelled());
        assertEquals(List.of(outside, protectedFrame, remaining), event.getBlocks());
        verify(cache).getDominion(outside.getLocation());
        verify(cache).getDominion(protectedFrame.getLocation());
        verifyNoMoreInteractions(cache);
        verifyNoInteractions(player);
        worldWide.verify(() -> WorldWide.isWorldWideEnabled(destination));
        worldWide.verifyNoMoreInteractions();
    }

    @Test
    void protectsBlocksThatWouldBeClearedToAir() {
        BlockState frame = blockAt(0, Material.OBSIDIAN);
        BlockState cleared = blockAt(1, Material.AIR);
        allowInDominion(frame, true);
        allowInDominion(cleared, false);
        PortalCreateEvent event = portal(PortalCreateEvent.CreateReason.NETHER_PAIR, null, frame, cleared);

        listener.handler(event);

        assertTrue(event.isCancelled());
        assertEquals(List.of(frame, cleared), event.getBlocks());
        verify(cache).getDominion(cleared.getLocation());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void appliesTheDestinationDominionSettingToTheEntirePortal(boolean allowed) {
        BlockState frame = blockAt(0, Material.OBSIDIAN);
        BlockState interior = blockAt(1, Material.NETHER_PORTAL);
        allowInDominion(frame, allowed);
        allowInDominion(interior, allowed);
        PortalCreateEvent event = portal(PortalCreateEvent.CreateReason.NETHER_PAIR, null, frame, interior);

        listener.handler(event);

        assertEquals(!allowed, event.isCancelled());
        assertEquals(List.of(frame, interior), event.getBlocks());
        if (allowed) {
            verify(cache).getDominion(frame.getLocation());
            verify(cache).getDominion(interior.getLocation());
            verifyNoMoreInteractions(cache);
        }
        worldWide.verifyNoInteractions();
    }

    @ParameterizedTest
    @EnumSource(value = PortalCreateEvent.CreateReason.class, names = {"FIRE", "END_PLATFORM"})
    void leavesIgnitionAndEndPlatformsUnchanged(PortalCreateEvent.CreateReason reason) {
        BlockState block = blockAt(0, Material.OBSIDIAN);
        PortalCreateEvent event = portal(reason, null, block);

        listener.handler(event);

        assertFalse(event.isCancelled());
        assertEquals(List.of(block), event.getBlocks());
        verifyNoInteractions(cache);
        worldWide.verifyNoInteractions();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void protectsTheDestinationForNullAndNonPlayerActors(boolean hasEntity) {
        Entity entity = hasEntity ? mock(Zombie.class) : null;
        BlockState block = blockAt(0, Material.OBSIDIAN);
        allowInDominion(block, false);
        PortalCreateEvent event = portal(PortalCreateEvent.CreateReason.NETHER_PAIR, entity, block);

        listener.handler(event);

        assertTrue(event.isCancelled());
        verify(cache).getDominion(block.getLocation());
        if (entity != null) verifyNoInteractions(entity);
    }

    @Test
    void leavesAlreadyCancelledEventsUntouched() {
        BlockState block = blockAt(0, Material.OBSIDIAN);
        PortalCreateEvent event = portal(PortalCreateEvent.CreateReason.NETHER_PAIR, null, block);
        event.setCancelled(true);

        listener.handler(event);

        assertTrue(event.isCancelled());
        assertEquals(List.of(block), event.getBlocks());
        verifyNoInteractions(cache);
        worldWide.verifyNoInteractions();
    }

    @ParameterizedTest
    @CsvSource({"true, false, true", "true, true, false", "false, false, false"})
    void usesWorldWideSettingsOutsideDominions(boolean enabled, boolean allowed, boolean cancelled) {
        BlockState block = blockAt(0, Material.OBSIDIAN);
        worldWide.when(() -> WorldWide.isWorldWideEnabled(destination)).thenReturn(enabled);
        worldWide.when(() -> WorldWide.getEnvFlagValue(destination, Flags.NETHER_PORTAL_CREATE))
                .thenReturn(allowed);
        PortalCreateEvent event = portal(PortalCreateEvent.CreateReason.NETHER_PAIR, null, block);

        listener.handler(event);

        assertEquals(cancelled, event.isCancelled());
        verify(cache).getDominion(block.getLocation());
        worldWide.verify(() -> WorldWide.isWorldWideEnabled(destination));
        if (enabled) {
            worldWide.verify(() -> WorldWide.getEnvFlagValue(destination, Flags.NETHER_PORTAL_CREATE));
        }
        worldWide.verifyNoMoreInteractions();
    }

    @Test
    void allowsCreationWhenTheFlagIsGloballyDisabled() {
        Flags.NETHER_PORTAL_CREATE.setEnable(false);
        BlockState block = blockAt(0, Material.OBSIDIAN);
        PortalCreateEvent event = portal(PortalCreateEvent.CreateReason.NETHER_PAIR, null, block);

        listener.handler(event);

        assertFalse(event.isCancelled());
        verifyNoInteractions(cache);
        worldWide.verifyNoInteractions();
    }

    private BlockState blockAt(int x, Material material) {
        BlockState block = mock(BlockState.class);
        when(block.getLocation()).thenReturn(new Location(destination, x, 64, 0));
        when(block.getType()).thenReturn(material);
        return block;
    }

    private void allowInDominion(BlockState block, boolean allowed) {
        DominionDTO dominion = mock(DominionDTO.class);
        when(dominion.getEnvironmentFlagValue()).thenReturn(Map.of(Flags.NETHER_PORTAL_CREATE, allowed));
        when(cache.getDominion(block.getLocation())).thenReturn(dominion);
    }

    private PortalCreateEvent portal(PortalCreateEvent.CreateReason reason, Entity entity, BlockState... blocks) {
        return new PortalCreateEvent(new ArrayList<>(List.of(blocks)), destination, entity, reason);
    }
}
