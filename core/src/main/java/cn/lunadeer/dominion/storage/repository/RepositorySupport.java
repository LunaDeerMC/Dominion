package cn.lunadeer.dominion.storage.repository;

import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.Flag;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.storage.DatabaseManager;
import cn.lunadeer.dominion.storage.DatabaseType;
import cn.lunadeer.dominion.storage.mapper.GenericMapper;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSession;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

abstract class RepositorySupport {

    @FunctionalInterface
    protected interface SqlSupplier<T> {
        T get(SqlSession session, GenericMapper mapper) throws Exception;
    }

    protected static <T> T sql(SqlSupplier<T> supplier) throws SQLException {
        SqlSession session = null;
        try {
            session = DatabaseManager.instance.openSession();
            T result = supplier.get(session, session.getMapper(GenericMapper.class));
            session.commit();
            return result;
        } catch (SQLException exception) {
            if (session != null) session.rollback();
            throw exception;
        } catch (PersistenceException exception) {
            if (session != null) session.rollback();
            throw new SQLException(exception.getMessage(), exception);
        } catch (Exception exception) {
            if (session != null) session.rollback();
            throw new SQLException(exception.getMessage(), exception);
        } finally {
            if (session != null) session.close();
        }
    }

    protected static DatabaseType databaseType() {
        return DatabaseManager.instance.getType();
    }

    protected static LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Number number) {
            return new Timestamp(number.longValue()).toLocalDateTime();
        }
        if (value != null) {
            return Timestamp.valueOf(value.toString()).toLocalDateTime();
        }
        return LocalDateTime.of(1970, 1, 1, 0, 0);
    }

    protected static Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Integer integer) return integer;
        if (value instanceof Number number) return number.intValue();
        return Integer.parseInt(value.toString());
    }

    protected static boolean toBoolean(Object value, boolean defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Boolean b) return b;
        if (value instanceof Number number) return number.intValue() != 0;
        String text = value.toString();
        return "1".equals(text) || "true".equalsIgnoreCase(text) || "t".equalsIgnoreCase(text);
    }

    protected static Map<PriFlag, Boolean> defaultPriFlags() {
        Map<PriFlag, Boolean> flags = new HashMap<>();
        for (PriFlag flag : Flags.getAllPriFlagsEnable()) {
            flags.put(flag, flag.getDefaultValue());
        }
        return flags;
    }

    protected static Map<EnvFlag, Boolean> defaultEnvFlags() {
        Map<EnvFlag, Boolean> flags = new HashMap<>();
        for (EnvFlag flag : Flags.getAllEnvFlagsEnable()) {
            flags.put(flag, flag.getDefaultValue());
        }
        return flags;
    }

    protected static Map<PriFlag, Boolean> readPriFlags(Map<String, Object> row) {
        Map<PriFlag, Boolean> flags = defaultPriFlags();
        for (PriFlag flag : Flags.getAllPriFlagsEnable()) {
            flags.put(flag, readFlag(row, flag));
        }
        return flags;
    }

    protected static Map<EnvFlag, Boolean> readEnvFlags(Map<String, Object> row) {
        Map<EnvFlag, Boolean> flags = defaultEnvFlags();
        for (EnvFlag flag : Flags.getAllEnvFlagsEnable()) {
            flags.put(flag, readFlag(row, flag));
        }
        return flags;
    }

    protected static void putPriFlags(Map<String, Object> values, Map<PriFlag, Boolean> flags) {
        Map<PriFlag, Boolean> source = flags == null ? Collections.emptyMap() : flags;
        for (PriFlag flag : Flags.getAllPriFlagsEnable()) {
            values.put(flag.getFlagName(), normalizeValue(flag, source.get(flag)));
        }
    }

    protected static void putEnvFlags(Map<String, Object> values, Map<EnvFlag, Boolean> flags) {
        Map<EnvFlag, Boolean> source = flags == null ? Collections.emptyMap() : flags;
        for (EnvFlag flag : Flags.getAllEnvFlagsEnable()) {
            values.put(flag.getFlagName(), normalizeValue(flag, source.get(flag)));
        }
    }

    protected static void updateFlag(GenericMapper mapper, String table, String ownerIdColumn, int ownerId, Flag flag, Boolean value) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(flag.getFlagName(), normalizeValue(flag, value));
        mapper.updateColumns(table, ownerIdColumn, ownerId, values);
    }

    protected static void updatePriFlags(GenericMapper mapper, String table, String ownerIdColumn, int ownerId, Map<PriFlag, Boolean> flags) {
        Map<String, Object> values = new LinkedHashMap<>();
        putPriFlags(values, flags);
        updateFields(mapper, table, ownerIdColumn, ownerId, values);
    }

    protected static void updateEnvFlags(GenericMapper mapper, String table, String ownerIdColumn, int ownerId, Map<EnvFlag, Boolean> flags) {
        Map<String, Object> values = new LinkedHashMap<>();
        putEnvFlags(values, flags);
        updateFields(mapper, table, ownerIdColumn, ownerId, values);
    }

    protected static Object value(Map<String, Object> row, String column) {
        if (row == null) return null;
        if (row.containsKey(column)) return row.get(column);
        String lowerColumn = column.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey() != null && entry.getKey().toLowerCase(Locale.ROOT).equals(lowerColumn)) {
                return entry.getValue();
            }
        }
        return null;
    }

    protected static Integer integer(Map<String, Object> row, String column) {
        return toInteger(value(row, column));
    }

    protected static String string(Map<String, Object> row, String column) {
        Object value = value(row, column);
        return value == null ? null : value.toString();
    }

    private static <T extends Flag> boolean readFlag(Map<String, Object> row, T flag) {
        return toBoolean(value(row, flag.getFlagName()), flag.getDefaultValue());
    }

    private static boolean normalizeValue(Flag flag, Boolean value) {
        return value != null ? value : flag.getDefaultValue();
    }

    private static void updateFields(GenericMapper mapper, String table, String ownerIdColumn, int ownerId, Map<String, Object> values) {
        if (values.isEmpty()) {
            return;
        }
        mapper.updateColumns(table, ownerIdColumn, ownerId, values);
    }
}
