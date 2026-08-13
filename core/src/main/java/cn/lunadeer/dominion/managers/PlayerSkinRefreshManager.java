package cn.lunadeer.dominion.managers;

import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.utils.XLogger;
import cn.lunadeer.dominion.utils.scheduler.CancellableTask;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerProfile;

import java.net.URL;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Serializes remote player profile refreshes so a mass join cannot create a
 * burst of Mojang requests. The queue only retains profile snapshots, never
 * Player instances, so pending work does not keep players alive.
 */
public final class PlayerSkinRefreshManager {
    public static final long REQUEST_INTERVAL_MILLIS = 2_000L;
    public static final long[] RETRY_DELAYS_MILLIS = {30_000L, 60_000L, 120_000L};

    private static final int MAX_RETRIES = RETRY_DELAYS_MILLIS.length;

    public static PlayerSkinRefreshManager instance;

    private final Object lock = new Object();
    private final PriorityQueue<RefreshRequest> queue = new PriorityQueue<>(
            Comparator.comparingLong((RefreshRequest request) -> request.readyAt)
                    .thenComparingLong(request -> request.sequence)
    );
    private final Set<UUID> queuedOrInFlight = new HashSet<>();

    private long sequence;
    private long nextRequestAt;
    private boolean requestInFlight;
    private boolean drainScheduled;
    private boolean closed;
    private CancellableTask scheduledDrain;

    public PlayerSkinRefreshManager() {
        instance = this;
    }

    /**
     * Queues a profile snapshot captured from the joining player. Capturing
     * the snapshot itself does not call the remote profile update operation.
     */
    public void enqueue(Player player) {
        if (player == null) return;

        final UUID uuid = player.getUniqueId();
        synchronized (lock) {
            if (closed || queuedOrInFlight.contains(uuid)) return;
        }

        final PlayerProfile profile;
        try {
            profile = player.getPlayerProfile().clone();
        } catch (NoSuchMethodError | RuntimeException exception) {
            logFailure(uuid, exception);
            return;
        }

        synchronized (lock) {
            if (closed || !queuedOrInFlight.add(uuid)) return;
            queue.add(new RefreshRequest(uuid, profile, ++sequence, System.currentTimeMillis()));
            scheduleDrainLocked(System.currentTimeMillis());
        }
    }

    private void drain() {
        RefreshRequest request;
        synchronized (lock) {
            drainScheduled = false;
            scheduledDrain = null;
            if (closed || requestInFlight || queue.isEmpty()) return;

            long now = System.currentTimeMillis();
            RefreshRequest next = queue.peek();
            long readyAt = Math.max(nextRequestAt, next.readyAt);
            if (readyAt > now) {
                scheduleDrainLocked(now);
                return;
            }

            request = queue.poll();
            requestInFlight = true;
            nextRequestAt = now + REQUEST_INTERVAL_MILLIS;
        }

        final CompletableFuture<? extends PlayerProfile> update;
        try {
            update = request.profile.update();
        } catch (NoSuchMethodError | RuntimeException exception) {
            handleFailure(request, exception);
            return;
        }

        if (update == null) {
            handleFailure(request, new IllegalStateException("PlayerProfile.update() returned null"));
            return;
        }

        update.whenComplete((updated, exception) -> {
            if (exception != null) {
                handleFailure(request, exception);
                return;
            }
            handleSuccess(request, updated);
        });
    }

    private void handleSuccess(RefreshRequest request, PlayerProfile updated) {
        URL skin = null;
        Throwable failure = null;
        try {
            if (updated == null || updated.getTextures() == null) {
                failure = new IllegalStateException("Updated player profile has no textures");
            } else {
                skin = updated.getTextures().getSkin();
                if (skin == null) {
                    failure = new IllegalStateException("Updated player profile has no skin");
                }
            }
        } catch (RuntimeException exception) {
            failure = exception;
        }

        if (failure != null) {
            handleFailure(request, failure);
            return;
        }

        synchronized (lock) {
            if (closed) {
                requestInFlight = false;
                queuedOrInFlight.remove(request.uuid);
                return;
            }

            try {
                CacheManager cacheManager = CacheManager.instance;
                if (cacheManager == null) {
                    throw new IllegalStateException("CacheManager is not available");
                }
                cacheManager.updatePlayerSkin(request.uuid, skin);
            } catch (Exception exception) {
                requestInFlight = false;
                handleFailureLocked(request, exception, System.currentTimeMillis());
                return;
            }

            requestInFlight = false;
            queuedOrInFlight.remove(request.uuid);
            scheduleDrainLocked(System.currentTimeMillis());
        }
    }

    private void handleFailure(RefreshRequest request, Throwable failure) {
        synchronized (lock) {
            requestInFlight = false;
            if (closed) {
                queuedOrInFlight.remove(request.uuid);
                return;
            }
            handleFailureLocked(request, failure, System.currentTimeMillis());
        }
        logFailure(request.uuid, failure);
    }

    private void handleFailureLocked(RefreshRequest request, Throwable failure, long now) {
        if (request.retries < MAX_RETRIES) {
            request.readyAt = now + RETRY_DELAYS_MILLIS[request.retries];
            request.retries++;
            queue.add(request);
        } else {
            queuedOrInFlight.remove(request.uuid);
        }
        scheduleDrainLocked(now);
    }

    private void scheduleDrainLocked(long now) {
        if (closed || drainScheduled || requestInFlight || queue.isEmpty()) return;

        RefreshRequest next = queue.peek();
        long runAt = Math.max(nextRequestAt, next.readyAt);
        long delayMillis = Math.max(0L, runAt - now);
        long delayTicks = (delayMillis + 49L) / 50L;

        drainScheduled = true;
        try {
            scheduledDrain = Scheduler.runTaskLaterAsync(this::drain, delayTicks);
        } catch (RuntimeException exception) {
            drainScheduled = false;
            scheduledDrain = null;
            logFailure(next.uuid, exception);
        }
    }

    public void shutdown() {
        CancellableTask task;
        synchronized (lock) {
            closed = true;
            queue.clear();
            queuedOrInFlight.clear();
            task = scheduledDrain;
            scheduledDrain = null;
            drainScheduled = false;
        }
        if (task != null) task.cancel();
        if (instance == this) instance = null;
    }

    public static void shutdownInstance() {
        PlayerSkinRefreshManager manager = instance;
        if (manager != null) manager.shutdown();
    }

    private static void logFailure(UUID uuid, Throwable failure) {
        if (XLogger.instance == null) return;
        String message = failure == null ? "unknown error" : failure.getMessage();
        XLogger.debug("Player skin refresh failed for {0}: {1}", uuid, message == null ? failure.getClass().getSimpleName() : message);
    }

    private static final class RefreshRequest {
        private final UUID uuid;
        private final PlayerProfile profile;
        private final long sequence;
        private int retries;
        private long readyAt;

        private RefreshRequest(UUID uuid, PlayerProfile profile, long sequence, long readyAt) {
            this.uuid = uuid;
            this.profile = profile;
            this.sequence = sequence;
            this.readyAt = readyAt;
        }
    }
}
