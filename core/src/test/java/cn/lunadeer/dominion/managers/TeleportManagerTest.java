package cn.lunadeer.dominion.managers;

import cn.lunadeer.dominion.utils.Misc;
import cn.lunadeer.dominion.utils.SafeLocationFinder;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TeleportManagerTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void mountedPlayerCanTeleportAfterDismounting(boolean paper) {
        checkTeleport(paper, true);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void unmountedPlayerCanTeleport(boolean paper) {
        checkTeleport(paper, false);
    }

    private void checkTeleport(boolean paper, boolean mounted) {
        Player player = mock(Player.class);
        Entity vehicle = mock(Entity.class);
        Entity passenger = mock(Entity.class);
        World world = mock(World.class);
        Location target = new Location(world, 100, 65, 100);
        Location safe = new Location(world, 100.5, 65, 100.5);
        AtomicReference<Entity> currentVehicle = new AtomicReference<>(mounted ? vehicle : null);
        when(player.isInsideVehicle()).thenAnswer(invocation -> currentVehicle.get() != null);
        when(player.getVehicle()).thenAnswer(invocation -> currentVehicle.get());
        when(player.leaveVehicle()).thenAnswer(invocation -> currentVehicle.getAndSet(null) != null);
        when(player.getPassengers()).thenReturn(List.of(passenger));
        CompletableFuture<Chunk> chunk = new CompletableFuture<>();
        when(world.getChunkAtAsyncUrgently(any(Location.class))).thenReturn(chunk);
        Queue<Runnable> tasks = new ArrayDeque<>();

        try (MockedStatic<Misc> misc = mockStatic(Misc.class);
             MockedStatic<SafeLocationFinder> finder = mockStatic(SafeLocationFinder.class);
             MockedStatic<Scheduler> scheduler = mockStatic(Scheduler.class)) {
            misc.when(Misc::isPaper).thenReturn(paper);
            finder.when(() -> SafeLocationFinder.findNearestSafeLocation(target)).thenReturn(safe);
            scheduler.when(() -> Scheduler.runEntityTask(any(Runnable.class), any(Entity.class)))
                    .thenAnswer(invocation -> { tasks.add(invocation.getArgument(0)); return null; });
            scheduler.when(() -> Scheduler.runLocationTask(any(Runnable.class), any(Location.class)))
                    .thenAnswer(invocation -> { tasks.add(invocation.getArgument(0)); return null; });

            TeleportManager.doTeleportSafely(player, target);
            assertNull(currentVehicle.get(), "dismount must finish before teleportation");
            // Entity tasks execute on a later tick, after leaveVehicle clears getVehicle().
            while (!tasks.isEmpty()) assertDoesNotThrow(tasks.remove()::run);
            if (paper) {
                verify(player, never()).teleportAsync(any(Location.class), any(TeleportCause.class));
                chunk.complete(mock(Chunk.class));
                while (!tasks.isEmpty()) assertDoesNotThrow(tasks.remove()::run);
                verify(player).teleportAsync(safe, TeleportCause.PLUGIN);
            } else {
                verify(player).teleport(safe, TeleportCause.PLUGIN);
            }
            verify(player).removePassenger(passenger);
            verify(player, times(mounted ? 1 : 0)).leaveVehicle();
        }
    }
}
