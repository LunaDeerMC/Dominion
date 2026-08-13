package cn.lunadeer.dominion.managers;

import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.utils.scheduler.CancellableTask;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;
import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerTextures;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerSkinRefreshManagerTest {
    @Test
    void duplicateJoinOnlyStartsOneRequestAndQueuedPlayersAreRateLimited() throws Exception {
        CacheManager cacheManager = mock(CacheManager.class);
        CacheManager.instance = cacheManager;

        UUID firstUuid = UUID.randomUUID();
        UUID secondUuid = UUID.randomUUID();
        CompletableFuture<PlayerProfile> firstUpdate = new CompletableFuture<>();
        CompletableFuture<PlayerProfile> secondUpdate = new CompletableFuture<>();
        PlayerProfile firstProfile = profile(firstUpdate, "http://textures.minecraft.net/texture/first");
        PlayerProfile secondProfile = profile(secondUpdate, "http://textures.minecraft.net/texture/second");
        Player firstPlayer = player(firstUuid, firstProfile);
        Player secondPlayer = player(secondUuid, secondProfile);
        List<Runnable> scheduled = new ArrayList<>();
        List<Long> delays = new ArrayList<>();

        try (MockedStatic<Scheduler> scheduler = mockScheduler(scheduled, delays)) {
            PlayerSkinRefreshManager manager = new PlayerSkinRefreshManager();
            try {
                manager.enqueue(firstPlayer);
                manager.enqueue(firstPlayer);
                manager.enqueue(secondPlayer);

                assertEquals(1, scheduled.size());
                scheduled.remove(0).run();
                firstUpdate.complete(firstProfile);

                verify(firstProfile, times(1)).update();
                verify(cacheManager).updatePlayerSkin(firstUuid, new URL("http://textures.minecraft.net/texture/first"));
                assertEquals(1, scheduled.size());
                assertTrue(delays.get(1) >= 1L, "the second request must not start in the same tick");
            } finally {
                manager.shutdown();
            }
        } finally {
            CacheManager.instance = null;
        }
    }

    @Test
    void failedRefreshKeepsCacheUntouchedAndSchedulesBackoff() throws Exception {
        CacheManager cacheManager = mock(CacheManager.class);
        CacheManager.instance = cacheManager;

        UUID uuid = UUID.randomUUID();
        CompletableFuture<PlayerProfile> update = new CompletableFuture<>();
        PlayerProfile profile = profile(update, "http://textures.minecraft.net/texture/unused");
        Player player = player(uuid, profile);
        List<Runnable> scheduled = new ArrayList<>();
        List<Long> delays = new ArrayList<>();

        try (MockedStatic<Scheduler> scheduler = mockScheduler(scheduled, delays)) {
            PlayerSkinRefreshManager manager = new PlayerSkinRefreshManager();
            try {
                manager.enqueue(player);
                scheduled.remove(0).run();
                update.completeExceptionally(new IllegalStateException("429"));

                verify(cacheManager, times(0)).updatePlayerSkin(any(), any());
                assertEquals(1, scheduled.size());
                assertTrue(delays.get(1) >= 500L, "the first retry should be delayed by about 30 seconds");
            } finally {
                manager.shutdown();
            }
        } finally {
            CacheManager.instance = null;
        }
    }

    @Test
    void shutdownPreventsAnAlreadyStartedFutureFromWritingTheDatabase() throws Exception {
        CacheManager cacheManager = mock(CacheManager.class);
        CacheManager.instance = cacheManager;

        UUID uuid = UUID.randomUUID();
        CompletableFuture<PlayerProfile> update = new CompletableFuture<>();
        PlayerProfile profile = profile(update, "http://textures.minecraft.net/texture/unused");
        Player player = player(uuid, profile);
        List<Runnable> scheduled = new ArrayList<>();
        List<Long> delays = new ArrayList<>();

        try (MockedStatic<Scheduler> scheduler = mockScheduler(scheduled, delays)) {
            PlayerSkinRefreshManager manager = new PlayerSkinRefreshManager();
            manager.enqueue(player);
            scheduled.remove(0).run();
            manager.shutdown();
            update.complete(profile);
            verify(cacheManager, times(0)).updatePlayerSkin(any(), any());
        } finally {
            CacheManager.instance = null;
        }
    }

    private static PlayerProfile profile(CompletableFuture<PlayerProfile> update, String skin) {
        PlayerProfile profile = mock(PlayerProfile.class);
        PlayerTextures textures = mock(PlayerTextures.class);
        doReturn(profile).when(profile).clone();
        when(profile.update()).thenReturn(update);
        when(profile.getTextures()).thenReturn(textures);
        try {
            when(textures.getSkin()).thenReturn(new URL(skin));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
        return profile;
    }

    private static Player player(UUID uuid, PlayerProfile profile) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getPlayerProfile()).thenReturn(profile);
        return player;
    }

    private static MockedStatic<Scheduler> mockScheduler(List<Runnable> scheduled, List<Long> delays) {
        MockedStatic<Scheduler> scheduler = org.mockito.Mockito.mockStatic(Scheduler.class);
        scheduler.when(() -> Scheduler.runTaskLaterAsync(any(Runnable.class), anyLong()))
                .thenAnswer(invocation -> {
                    scheduled.add(invocation.getArgument(0));
                    delays.add(invocation.getArgument(1));
                    return (CancellableTask) () -> {
                    };
                });
        return scheduler;
    }
}
