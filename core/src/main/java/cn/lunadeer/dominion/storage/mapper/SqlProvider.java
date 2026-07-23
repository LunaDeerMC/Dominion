package cn.lunadeer.dominion.storage.mapper;

import cn.lunadeer.dominion.storage.DatabaseType;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static cn.lunadeer.dominion.storage.DatabaseSchema.CUA_LOG_ID;
import static cn.lunadeer.dominion.storage.DatabaseSchema.CUA_SERVER_ID;
import static cn.lunadeer.dominion.storage.DatabaseSchema.CUL_CREATED_AT;
import static cn.lunadeer.dominion.storage.DatabaseSchema.CUL_ID;
import static cn.lunadeer.dominion.storage.DatabaseSchema.CUL_SERVER_ID;
import static cn.lunadeer.dominion.storage.DatabaseSchema.DOM_ID;
import static cn.lunadeer.dominion.storage.DatabaseSchema.DOM_SERVER_ID;
import static cn.lunadeer.dominion.storage.DatabaseSchema.DOMINION;
import static cn.lunadeer.dominion.storage.DatabaseSchema.SERVER_ID;
import static cn.lunadeer.dominion.storage.DatabaseSchema.SERVER_INFO;
import static cn.lunadeer.dominion.storage.DatabaseSchema.TP_CACHE;
import static cn.lunadeer.dominion.storage.DatabaseSchema.TP_DOM_ID;
import static cn.lunadeer.dominion.storage.DatabaseSchema.TP_UUID;
import static cn.lunadeer.dominion.storage.DatabaseSchema.identifier;
import static cn.lunadeer.dominion.storage.DatabaseSchema.table;

public class SqlProvider {
    private static final Set<String> COMPARISON_OPERATORS = Set.of("=", ">", ">=", "<", "<=");

    public String selectAll(Map<String, Object> params) {
        return "SELECT * FROM " + tableName(params);
    }

    public String selectWhere(Map<String, Object> params) {
        return "SELECT * FROM " + tableName(params) + " WHERE " + column(params, "column") + " = #{value}";
    }

    @SuppressWarnings("unchecked")
    public String selectWhereAll(Map<String, Object> params) {
        return "SELECT * FROM " + tableName(params) + " WHERE " + whereAll((Map<String, Object>) params.get("values"));
    }

    public String selectWhereCompare(Map<String, Object> params) {
        String operator = String.valueOf(params.get("operator"));
        if (!COMPARISON_OPERATORS.contains(operator)) {
            throw new IllegalArgumentException("Unsupported SQL comparison operator: " + operator);
        }
        return "SELECT * FROM " + tableName(params) + " WHERE " + column(params, "column") + " " + operator + " #{value}";
    }

    public String selectDominionsByServer() {
        return "SELECT * FROM " + DOMINION + " WHERE " + DOM_SERVER_ID + " = #{serverId} AND " + DOM_ID + " >= 0";
    }

    public String selectValue(Map<String, Object> params) {
        return "SELECT " + column(params, "selectColumn") + " FROM " + tableName(params)
                + " WHERE " + column(params, "whereColumn") + " = #{value}";
    }

    @SuppressWarnings("unchecked")
    public String insert(Map<String, Object> params) {
        String table = tableName(params);
        Map<String, Object> values = (Map<String, Object>) params.get("values");
        return insertStatement("INSERT INTO", table, values, "");
    }

    @SuppressWarnings("unchecked")
    public String insertIgnore(Map<String, Object> params) {
        String table = tableName(params);
        Map<String, Object> values = (Map<String, Object>) params.get("values");
        DatabaseType databaseType = (DatabaseType) params.get("databaseType");
        String conflictColumn = column(params, "conflictColumn");
        if (databaseType.isMySqlFamily()) {
            return insertStatement("INSERT IGNORE INTO", table, values, "");
        }
        return insertStatement("INSERT INTO", table, values, " ON CONFLICT(" + conflictColumn + ") DO NOTHING");
    }

    @SuppressWarnings("unchecked")
    public String updateColumns(Map<String, Object> params) {
        String table = tableName(params);
        String idColumn = column(params, "idColumn");
        Map<String, Object> values = (Map<String, Object>) params.get("values");
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("No values supplied for update");
        }
        StringJoiner sets = new StringJoiner(", ");
        for (String column : values.keySet()) {
            sets.add(identifier(column) + " = #{values." + column + "}");
        }
        return "UPDATE " + table + " SET " + sets + " WHERE " + idColumn + " = #{id}";
    }

    public String deleteWhere(Map<String, Object> params) {
        return "DELETE FROM " + tableName(params) + " WHERE " + column(params, "column") + " = #{value}";
    }

    @SuppressWarnings("unchecked")
    public String deleteWhereAll(Map<String, Object> params) {
        return "DELETE FROM " + tableName(params) + " WHERE " + whereAll((Map<String, Object>) params.get("values"));
    }

    public String selectUnconsumedLogs(Map<String, Object> params) {
        String logTable = (String) params.get("logTable");
        String ackTable = (String) params.get("ackTable");
        return "SELECT l.* FROM " + identifier(logTable) + " l " +
                "WHERE l." + CUL_ID + " NOT IN (" +
                "  SELECT a." + CUA_LOG_ID + " FROM " + identifier(ackTable) + " a " +
                "  WHERE a." + CUA_SERVER_ID + " = #{serverId}" +
                ") " +
                "AND l." + CUL_SERVER_ID + " != #{serverId} " +
                "ORDER BY l." + CUL_CREATED_AT + " ASC";
    }

    public String selectFullyConsumedLogs(Map<String, Object> params) {
        String logTable = (String) params.get("logTable");
        String ackTable = (String) params.get("ackTable");
        String serverInfoTable = (String) params.get("serverInfoTable");
        int maxAgeMinutes = (int) params.get("maxAgeMinutes");
        return "SELECT l." + CUL_ID + " FROM " + identifier(logTable) + " l " +
                "WHERE l." + CUL_SERVER_ID + " = #{producerServerId} " +
                "AND (SELECT COUNT(*) FROM " + identifier(serverInfoTable) + ") = " +
                "    (SELECT COUNT(*) FROM " + identifier(ackTable) + " a " +
                "     WHERE a." + CUA_LOG_ID + " = l." + CUL_ID + ") " +
                "OR l." + CUL_CREATED_AT + " < NOW() - INTERVAL '" + maxAgeMinutes + " MINUTE'";
    }

    @SuppressWarnings("unchecked")
    public String deleteLogsAndAcks(Map<String, Object> params) {
        String ackTable = (String) params.get("ackTable");
        Collection<Long> ids = (Collection<Long>) params.get("ids");
        if (ids == null || ids.isEmpty()) {
            return "SELECT 1 WHERE 1=0";
        }
        String idList = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        return "DELETE FROM " + identifier(ackTable) + " WHERE " + CUA_LOG_ID + " IN (" + idList + ")";
    }

    @SuppressWarnings("unchecked")
    public String deleteWhereIn(Map<String, Object> params) {
        String tableName = (String) params.get("table");
        String columnName = (String) params.get("column");
        Collection<?> values = (Collection<?>) params.get("values");
        if (values == null || values.isEmpty()) {
            return "SELECT 1 WHERE 1=0";
        }
        String valueList = values.stream()
                .map(v -> v instanceof String ? "'" + v + "'" : String.valueOf(v))
                .collect(Collectors.joining(","));
        return "DELETE FROM " + identifier(tableName) + " WHERE " + identifier(columnName) + " IN (" + valueList + ")";
    }

    public String upsertTeleport(Map<String, Object> params) {
        DatabaseType databaseType = (DatabaseType) params.get("databaseType");
        String base = "INSERT INTO " + TP_CACHE + " (" + TP_UUID + ", " + TP_DOM_ID + ") VALUES (#{uuid}, #{dominionId})";
        if (databaseType.isMySqlFamily()) {
            return base + " ON DUPLICATE KEY UPDATE " + TP_DOM_ID + " = #{dominionId}";
        }
        return base + " ON CONFLICT(" + TP_UUID + ") DO UPDATE SET " + TP_DOM_ID + " = excluded." + TP_DOM_ID;
    }

    private String insertStatement(String keyword, String table, Map<String, Object> values, String suffix) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("No values supplied for insert");
        }
        StringJoiner columns = new StringJoiner(", ");
        StringJoiner parameters = new StringJoiner(", ");
        for (String column : values.keySet()) {
            columns.add(identifier(column));
            parameters.add("#{values." + column + "}");
        }
        return keyword + " " + table + " (" + columns + ") VALUES (" + parameters + ")" + suffix;
    }

    private String whereAll(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("No values supplied for WHERE clause");
        }
        StringJoiner conditions = new StringJoiner(" AND ");
        for (String column : values.keySet()) {
            conditions.add(identifier(column) + " = #{values." + column + "}");
        }
        return conditions.toString();
    }

    private String tableName(Map<String, Object> params) {
        return table(String.valueOf(params.get("table")));
    }

    private String column(Map<String, Object> params, String key) {
        return identifier(String.valueOf(params.get(key)));
    }
}