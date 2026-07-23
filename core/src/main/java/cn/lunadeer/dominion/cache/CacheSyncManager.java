package cn.lunadeer.dominion.cache;

import cn.lunadeer.dominion.cache.server.ServerCache;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.storage.repository.UpdateLogRepository;
import cn.lunadeer.dominion.utils.XLogger;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.lunadeer.dominion.storage.DatabaseSchema.*;

/**
 * Manages cross-server cache synchronization using a database-backed producer-consumer model.
 * <p>
 * When a server mutates a dominion, member, or group, it writes a notification into the
 * {@code cache_update_log} table and immediately self-consumes it. Other servers periodically
 * poll for unconsumed logs, apply the corresponding cache updates, and mark them as consumed.
 * Once all servers have consumed a log (or the log has expired), it is cleaned up.
 */
public class CacheSyncManager {

    public static CacheSyncManager instance;

    static final long POLL_INTERVAL_TICKS = 100L;  // 5 seconds
    static final int LOG_MAX_AGE_MINUTES = 60;      // clean up logs older than 1 hour

    private final int myServerId;

    public CacheSyncManager() {
        instance = this;
        this.myServerId = Configuration.multiServer.serverId;
    }

    // ============================================================
    // Producer: notify of changes
    // ============================================================

    public void notifyDominion(int domId) {
        notify("dominion", domId, "UPSERT");
    }

    public void notifyDominionDelete(int domId) {
        notify("dominion", domId, "DELETE");
    }

    public void notifyMember(int memberId) {
        notify("member", memberId, "UPSERT");
    }

    public void notifyMemberDelete(int memberId) {
        notify("member", memberId, "DELETE");
    }

    public void notifyGroup(int groupId) {
        notify("group", groupId, "UPSERT");
    }

    public void notifyGroupDelete(int groupId) {
        notify("group", groupId, "DELETE");
    }

    private void notify(String entityType, int targetId, String action) {
        Scheduler.runTaskAsync(() -> {
            try {
                UpdateLogRepository.insertLog(entityType, targetId, myServerId, action);
            } catch (Exception e) {
                XLogger.error("Failed to write cache update log: type={0} id={1} action={2}", entityType, targetId, action);
                XLogger.error(e);
            }
        });
    }

    // ============================================================
    // Consumer: poll & process
    // ============================================================

    /**
     * Start the background polling task. Called once at startup.
     */
    public void startPolling() {
        XLogger.info("Starting cross-server cache sync polling (interval: {0}s)", POLL_INTERVAL_TICKS / 20);
        Scheduler.runTaskRepeatAsync(this::pollCycle, POLL_INTERVAL_TICKS, POLL_INTERVAL_TICKS);
    }

    void pollCycle() {
        try {
            List<Map<String, Object>> logs = UpdateLogRepository.selectUnconsumed(myServerId);
            if (logs.isEmpty()) {
                cleanupOwnLogs();
                return;
            }

            XLogger.debug("Cache sync: processing {0} unconsumed log(s)", logs.size());
            for (Map<String, Object> log : logs) {
                processLog(log);
            }

            cleanupOwnLogs();
        } catch (Exception e) {
            XLogger.error("Cache sync poll cycle failed");
            XLogger.error(e);
        }
    }

    @SuppressWarnings("unchecked")
    private void processLog(Map<String, Object> log) {
        Long logId = toLong(log.get(CUL_ID));
        String entityType = String.valueOf(log.get(CUL_ENTITY_TYPE));
        int targetId = toInt(log.get(CUL_TARGET_ID));
        int producerServerId = toInt(log.get(CUL_SERVER_ID));
        String action = String.valueOf(log.getOrDefault(CUL_ACTION, "UPSERT"));

        if (logId == null) return;

        ServerCache serverCache = CacheManager.instance.getCache(producerServerId);
        if (serverCache == null) {
            // Server cache not yet initialized — skip and try next cycle
            return;
        }

        try {
            applyUpdate(serverCache, entityType, targetId, action);
        } catch (Exception e) {
            XLogger.error("Failed to apply cache update: logId={0} type={1} id={2}", logId, entityType, targetId);
            XLogger.error(e);
        }

        // Mark as consumed
        try {
            ackLog(logId);
        } catch (Exception e) {
            XLogger.error("Failed to ack cache update log: {0}", logId);
            XLogger.error(e);
        }
    }

    private void applyUpdate(ServerCache cache, String entityType, int targetId, String action) {
        switch (entityType) {
            case "dominion" -> {
                if ("DELETE".equals(action)) {
                    cache.getDominionCache().delete(targetId);
                } else {
                    cache.getDominionCache().load(targetId);
                }
            }
            case "member" -> {
                if ("DELETE".equals(action)) {
                    cache.getMemberCache().delete(targetId);
                } else {
                    cache.getMemberCache().load(targetId);
                }
            }
            case "group" -> {
                if ("DELETE".equals(action)) {
                    cache.getGroupCache().delete(targetId);
                } else {
                    cache.getGroupCache().load(targetId);
                }
            }
        }
    }

    private void ackLog(long logId) throws Exception {
        UpdateLogRepository.insertAck(logId, myServerId);
    }

    // ============================================================
    // Cleanup
    // ============================================================

    private void cleanupOwnLogs() {
        try {
            List<Long> toClean = UpdateLogRepository.selectFullyConsumedLogs(myServerId, LOG_MAX_AGE_MINUTES);
            if (!toClean.isEmpty()) {
                XLogger.debug("Cache sync: cleaning up {0} fully-consumed log(s)", toClean.size());
                UpdateLogRepository.deleteLogsAndAcks(new HashSet<>(toClean));
            }
        } catch (Exception e) {
            XLogger.error("Cache sync cleanup failed");
            XLogger.error(e);
        }
    }

    // ============================================================
    // Helpers
    // ============================================================

    private static Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long l) return l;
        if (value instanceof Number n) return n.longValue();
        return Long.parseLong(value.toString());
    }

    private static int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Integer i) return i;
        if (value instanceof Number n) return n.intValue();
        return Integer.parseInt(value.toString());
    }
}
