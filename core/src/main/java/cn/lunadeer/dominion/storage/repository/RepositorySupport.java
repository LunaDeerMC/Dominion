package cn.lunadeer.dominion.storage.repository;

import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.Flag;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.storage.DatabaseManager;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.exception.DataAccessException;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import static cn.lunadeer.dominion.storage.DatabaseSchema.boolField;

abstract class RepositorySupport {

    @FunctionalInterface
    protected interface SqlSupplier<T> {
        T get() throws Exception;
    }

    protected static DSLContext db() {
        return DatabaseManager.instance.dsl();
    }

    protected static <T> T sql(SqlSupplier<T> supplier) throws SQLException {
        try {
            return supplier.get();
        } catch (SQLException e) {
            throw e;
        } catch (DataAccessException e) {
            throw new SQLException(e.getMessage(), e);
        } catch (Exception e) {
            throw new SQLException(e.getMessage(), e);
        }
    }

    protected static LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value != null) {
            return Timestamp.valueOf(value.toString()).toLocalDateTime();
        }
        return LocalDateTime.of(1970, 1, 1, 0, 0);
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

    protected static Field<Boolean> flagField(String flagName) {
        return boolField(flagName);
    }

    protected static Field<Boolean> flagField(Flag flag) {
        return flagField(flag.getFlagName());
    }

    protected static Map<PriFlag, Boolean> readPriFlags(Record record) {
        Map<PriFlag, Boolean> flags = defaultPriFlags();
        for (PriFlag flag : Flags.getAllPriFlagsEnable()) {
            flags.put(flag, readFlag(record, flag));
        }
        return flags;
    }

    protected static Map<EnvFlag, Boolean> readEnvFlags(Record record) {
        Map<EnvFlag, Boolean> flags = defaultEnvFlags();
        for (EnvFlag flag : Flags.getAllEnvFlagsEnable()) {
            flags.put(flag, readFlag(record, flag));
        }
        return flags;
    }

    protected static void putPriFlags(Map<Field<?>, Object> values, Map<PriFlag, Boolean> flags) {
        Map<PriFlag, Boolean> source = flags == null ? Collections.emptyMap() : flags;
        for (PriFlag flag : Flags.getAllPriFlagsEnable()) {
            values.put(flagField(flag), normalizeValue(flag, source.get(flag)));
        }
    }

    protected static void putEnvFlags(Map<Field<?>, Object> values, Map<EnvFlag, Boolean> flags) {
        Map<EnvFlag, Boolean> source = flags == null ? Collections.emptyMap() : flags;
        for (EnvFlag flag : Flags.getAllEnvFlagsEnable()) {
            values.put(flagField(flag), normalizeValue(flag, source.get(flag)));
        }
    }

    protected static void updateFlag(Table<Record> table, Field<Integer> ownerIdField, int ownerId, Flag flag, Boolean value) {
        db().update(table)
                .set(flagField(flag), normalizeValue(flag, value))
                .where(ownerIdField.eq(ownerId))
                .execute();
    }

    protected static void updatePriFlags(Table<Record> table, Field<Integer> ownerIdField, int ownerId, Map<PriFlag, Boolean> flags) {
        Map<Field<?>, Object> values = new LinkedHashMap<>();
        putPriFlags(values, flags);
        updateFields(table, ownerIdField, ownerId, values);
    }

    protected static void updateEnvFlags(Table<Record> table, Field<Integer> ownerIdField, int ownerId, Map<EnvFlag, Boolean> flags) {
        Map<Field<?>, Object> values = new LinkedHashMap<>();
        putEnvFlags(values, flags);
        updateFields(table, ownerIdField, ownerId, values);
    }

    private static <T extends Flag> boolean readFlag(Record record, T flag) {
        return toBoolean(record.get(flagField(flag)), flag.getDefaultValue());
    }

    private static boolean normalizeValue(Flag flag, Boolean value) {
        return value != null ? value : flag.getDefaultValue();
    }

    private static void updateFields(Table<Record> table, Field<Integer> ownerIdField, int ownerId, Map<Field<?>, Object> values) {
        if (values.isEmpty()) {
            return;
        }
        db().update(table)
                .set(values)
                .where(ownerIdField.eq(ownerId))
                .execute();
    }
}
