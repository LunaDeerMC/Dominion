package cn.lunadeer.dominion.storage;

import cn.lunadeer.dominion.api.dtos.flag.Flag;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

final class FlagReconciler {

    private final DataSource dataSource;
    private final DatabaseType type;

    FlagReconciler(DataSource dataSource, DatabaseType type) {
        this.dataSource = dataSource;
        this.type = type;
    }

    SyncResult reconcile() {
        try (Connection connection = dataSource.getConnection()) {
            return reconcile(connection);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to reconcile flag columns", exception);
        }
    }

    private SyncResult reconcile(Connection connection) throws SQLException {
        int changed = 0;
        changed += reconcileSplitBurnFlag(connection);
        changed += reconcileSplitFlags(connection);
        changed += reconcileFlags(connection, "dominion", Flags.getAllEnvFlags());
        changed += reconcileFlags(connection, "dominion", Flags.getAllPriFlags());
        changed += reconcileFlags(connection, "dominion_member", Flags.getAllPriFlags());
        changed += reconcileFlags(connection, "dominion_group", Flags.getAllPriFlags());
        changed += reconcileFlags(connection, "privilege_template", Flags.getAllPriFlags());
        return new SyncResult(changed);
    }

    private int reconcileSplitFlags(Connection connection) throws SQLException {
        int changed = 0;
        for (Flag flag : Flags.getAllFlags()) {
            List<Flag> sources = Flags.getLegacySources(flag);
            if (sources.isEmpty()) continue;
            if (flag instanceof EnvFlag) {
                changed += reconcileSplitFlagColumn(connection, "dominion", sources, flag);
            } else if (flag instanceof PriFlag) {
                changed += reconcileSplitFlagColumn(connection, "dominion", sources, flag);
                changed += reconcileSplitFlagColumn(connection, "dominion_member", sources, flag);
                changed += reconcileSplitFlagColumn(connection, "dominion_group", sources, flag);
                changed += reconcileSplitFlagColumn(connection, "privilege_template", sources, flag);
            }
        }
        return changed;
    }

    private int reconcileSplitFlagColumn(Connection connection, String tableName, List<Flag> sources, Flag target)
            throws SQLException {
        if (columnExists(connection, tableName, target.getFlagName())) return 0;
        addFlagColumn(connection, tableName, target);
        if (Flags.preserveAllowedSpawnEggValue(target)) {
            setFlagColumn(connection, tableName, target.getFlagName(), true);
        } else {
            List<String> sourceColumns = sources.stream()
                    .map(Flag::getFlagName)
                    .filter(source -> {
                        try {
                            return columnExists(connection, tableName, source);
                        } catch (SQLException exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();
            if (!sourceColumns.isEmpty()) {
                if ((target == Flags.BURN_ENTITY_FIRE || target == Flags.BURN_ENTITY_LAVA)
                        && sourceColumns.contains(Flags.BURN_ENTITY.getFlagName())) {
                    sourceColumns = List.of(Flags.BURN_ENTITY.getFlagName());
                }
                copyFlagColumns(connection, tableName, sourceColumns, target.getFlagName());
            }
        }
        return 1;
    }

    private int reconcileSplitBurnFlag(Connection connection) throws SQLException {
        boolean oldBurnExists = columnExists(connection, "dominion", "burn");
        if (!oldBurnExists || columnExists(connection, "dominion", Flags.BURN_ENTITY.getFlagName())) {
            return 0;
        }
        // Keep the intermediate column available as a migration source for
        // installations that still have only the historical `burn` column.
        addFlagColumn(connection, "dominion", Flags.BURN_ENTITY);
        copyFlagColumn(connection, "dominion", "burn", Flags.BURN_ENTITY.getFlagName());
        return 1;
    }

    private int reconcileFlags(Connection connection, String tableName, List<? extends Flag> flags) throws SQLException {
        int changed = 0;
        for (Flag flag : flags) {
            if (!columnExists(connection, tableName, flag.getFlagName())) {
                addFlagColumn(connection, tableName, flag);
                changed++;
            }
            changed += backfillNullValues(connection, tableName, flag);
        }
        return changed;
    }

    private void addFlagColumn(Connection connection, String tableName, Flag flag) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + tableName + " ADD COLUMN " + flag.getFlagName() + " "
                    + boolType() + " NOT NULL DEFAULT " + booleanLiteral(flag.getDefaultValue()));
        }
    }

    private int backfillNullValues(Connection connection, String tableName, Flag flag) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE " + tableName + " SET " + flag.getFlagName() + " = ? WHERE " + flag.getFlagName() + " IS NULL")) {
            statement.setBoolean(1, flag.getDefaultValue());
            return statement.executeUpdate();
        }
    }

    private void copyFlagColumn(Connection connection, String tableName, String sourceColumn, String targetColumn) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE " + tableName + " SET " + targetColumn + " = " + sourceColumn);
        }
    }

    private void copyFlagColumns(Connection connection, String tableName, List<String> sourceColumns, String targetColumn)
            throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE " + tableName + " SET " + targetColumn + " = "
                    + String.join(" AND ", sourceColumns));
        }
    }

    private void setFlagColumn(Connection connection, String tableName, String targetColumn, boolean value)
            throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE " + tableName + " SET " + targetColumn + " = "
                    + booleanLiteral(value));
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String normalized = columnName.toLowerCase(Locale.ROOT);
        try (ResultSet rs = metaData.getColumns(
                DatabaseMetadataScope.catalog(connection),
                DatabaseMetadataScope.schema(connection, type),
                tableName,
                "%")) {
            while (rs.next()) {
                if (normalized.equals(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }
        if (type == DatabaseType.SQLITE) {
            try (var statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
                while (rs.next()) {
                    if (normalized.equals(rs.getString("name").toLowerCase(Locale.ROOT))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private String boolType() {
        return type.isMySqlFamily() ? "TINYINT(1)" : "BOOLEAN";
    }

    private String booleanLiteral(boolean value) {
        if (type.isMySqlFamily()) {
            return value ? "1" : "0";
        }
        return value ? "true" : "false";
    }

    record SyncResult(int changedEntries) {
    }
}
