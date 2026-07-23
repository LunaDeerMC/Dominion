package cn.lunadeer.dominion.storage.repository;

import cn.lunadeer.dominion.storage.mapper.GenericMapper;

import java.sql.SQLException;
import java.util.*;

import static cn.lunadeer.dominion.storage.DatabaseSchema.*;

/**
 * Repository for the cache update log and acknowledgement tables used in cross-server cache synchronization.
 */
public class UpdateLogRepository extends RepositorySupport {

    /**
     * Insert a new cache update log and immediately self-consume it.
     *
     * @param entityType dominion, member, or group
     * @param targetId   the ID of the entity that changed
     * @param serverId   the server that produced this change
     * @param action     UPSERT or DELETE
     */
    public static void insertLog(String entityType, int targetId, int serverId, String action) throws SQLException {
        sql((session, mapper) -> {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put(CUL_ENTITY_TYPE, entityType);
            values.put(CUL_TARGET_ID, targetId);
            values.put(CUL_SERVER_ID, serverId);
            values.put(CUL_ACTION, action);
            mapper.insert(identifier(CACHE_UPDATE_LOG), values);
            Integer logId = toInteger(values.get(CUL_ID));
            if (logId != null) {
                insertAckInternal(mapper, logId, serverId);
            }
            return null;
        });
    }

    private static void insertAckInternal(GenericMapper mapper, long logId, int serverId) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(CUA_LOG_ID, logId);
        values.put(CUA_SERVER_ID, serverId);
        mapper.insert(identifier(CACHE_UPDATE_ACK), values);
    }

    /**
     * Insert an acknowledgement for a log.
     */
    public static void insertAck(long logId, int serverId) throws SQLException {
        sql((session, mapper) -> {
            insertAckInternal(mapper, logId, serverId);
            return null;
        });
    }

    /**
     * Get all unconsumed update logs for a server (excluding logs produced by itself).
     */
    public static List<Map<String, Object>> selectUnconsumed(int serverId) throws SQLException {
        return sql((session, mapper) -> mapper.selectUnconsumedLogs(
                CACHE_UPDATE_LOG, CACHE_UPDATE_ACK, serverId));
    }

    /**
     * Get log IDs produced by the given server that have been consumed by ALL known servers,
     * or are older than maxAgeMinutes.
     */
    public static List<Long> selectFullyConsumedLogs(int producerServerId, int maxAgeMinutes) throws SQLException {
        return sql((session, mapper) -> {
            List<Map<String, Object>> rows = mapper.selectFullyConsumedLogs(
                    CACHE_UPDATE_LOG, CACHE_UPDATE_ACK, SERVER_INFO, producerServerId, maxAgeMinutes);
            List<Long> ids = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                ids.add(toLong(row.get(CUL_ID)));
            }
            return ids;
        });
    }

    /**
     * Delete logs and their acknowledgements by log IDs.
     */
    public static void deleteLogsAndAcks(Set<Long> ids) throws SQLException {
        if (ids.isEmpty()) return;
        sql((session, mapper) -> {
            // delete acks first, then logs (in a single transaction)
            mapper.deleteLogsAndAcks(CACHE_UPDATE_LOG, CACHE_UPDATE_ACK, ids);
            mapper.deleteWhereIn(CACHE_UPDATE_LOG, CUL_ID, ids);
            return null;
        });
    }

    private static Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long l) return l;
        if (value instanceof Number n) return n.longValue();
        return Long.parseLong(value.toString());
    }
}
