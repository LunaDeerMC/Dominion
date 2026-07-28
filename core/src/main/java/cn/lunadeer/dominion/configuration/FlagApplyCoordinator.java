package cn.lunadeer.dominion.configuration;

import cn.lunadeer.dominion.Dominion;
import cn.lunadeer.dominion.api.dtos.flag.FlagGroups;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.storage.DatabaseManager;
import cn.lunadeer.dominion.uis.chest.DominionChestUi;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Coalesces flag registry updates and applies only the affected subsystems.
 */
public final class FlagApplyCoordinator {
    private record Request(long flagRevision, long groupRevision, CompletableFuture<Void> future) {
    }

    private final List<Request> requests = new ArrayList<>();
    private long appliedFlagRevision = Flags.getRevision();
    private long appliedGroupRevision = FlagGroups.getRevision();
    private boolean scheduled;
    private boolean running;

    public synchronized CompletableFuture<Void> requestApply() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        requests.add(new Request(Flags.getRevision(), FlagGroups.getRevision(), future));
        scheduleIfNeeded();
        return future;
    }

    private synchronized void scheduleIfNeeded() {
        if (scheduled || running) return;
        scheduled = true;
        Scheduler.runTaskLaterAsync(this::runPass, 1L);
    }

    private void runPass() {
        FlagConfiguration.resolveAvailableGroupFlags();
        final long targetFlags;
        final long targetGroups;
        final boolean flagChanges;
        final boolean groupChanges;
        synchronized (this) {
            scheduled = false;
            running = true;
            targetFlags = Flags.getRevision();
            targetGroups = FlagGroups.getRevision();
            flagChanges = targetFlags > appliedFlagRevision;
            groupChanges = targetGroups > appliedGroupRevision;
        }

        Throwable failure = null;
        try {
            FlagConfiguration.saveRuntimeConfiguration();
            if (groupChanges) {
                Language.saveFlagGroupTexts();
            }
            if (flagChanges) {
                Language.reconcileFlagTexts();
                DatabaseManager.instance.reconcileFlags();
                WorldWide.load(Bukkit.getConsoleSender(), Dominion.instance);
                CacheManager.instance.reloadCache();
            }
            CompletableFuture<Void> uiReload = new CompletableFuture<>();
            Scheduler.runTask(() -> {
                try {
                    DominionChestUi.reload();
                    uiReload.complete(null);
                } catch (Throwable throwable) {
                    uiReload.completeExceptionally(throwable);
                }
            });
            uiReload.join();
        } catch (Throwable throwable) {
            failure = throwable;
        }

        synchronized (this) {
            if (failure == null) {
                appliedFlagRevision = Math.max(appliedFlagRevision, targetFlags);
                appliedGroupRevision = Math.max(appliedGroupRevision, targetGroups);
            }
            Iterator<Request> iterator = requests.iterator();
            while (iterator.hasNext()) {
                Request request = iterator.next();
                if (request.flagRevision() <= targetFlags && request.groupRevision() <= targetGroups) {
                    if (failure == null) request.future().complete(null);
                    else request.future().completeExceptionally(failure);
                    iterator.remove();
                }
            }
            running = false;
            if (!requests.isEmpty()
                    || Flags.getRevision() > targetFlags
                    || FlagGroups.getRevision() > targetGroups) {
                scheduleIfNeeded();
            }
        }
    }
}
