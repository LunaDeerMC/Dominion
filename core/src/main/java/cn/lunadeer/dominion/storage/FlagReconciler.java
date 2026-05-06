package cn.lunadeer.dominion.storage;

import cn.lunadeer.dominion.api.dtos.flag.Flag;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.jooq.DSLContext;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

final class FlagReconciler {

    private final DSLContext dsl;
    private final DatabaseType type;

    FlagReconciler(DSLContext dsl, DatabaseType type) {
        this.dsl = dsl;
        this.type = type;
    }

    SyncResult reconcile() {
        return dsl.connectionResult(this::reconcile);
    }

    private SyncResult reconcile(Connection connection) throws SQLException {
        int changed = 0;
        changed += reconcileFlags(connection, "dominion", Flags.getAllEnvFlags());
        changed += reconcileFlags(connection, "dominion", Flags.getAllPriFlags());
        changed += reconcileFlags(connection, "dominion_member", Flags.getAllPriFlags());
        changed += reconcileFlags(connection, "dominion_group", Flags.getAllPriFlags());
        changed += reconcileFlags(connection, "privilege_template", Flags.getAllPriFlags());
        return new SyncResult(changed);
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

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String normalized = columnName.toLowerCase(Locale.ROOT);
        try (ResultSet rs = metaData.getColumns(null, null, tableName, "%")) {
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
        return type == DatabaseType.MYSQL ? "TINYINT(1)" : "BOOLEAN";
    }

    private String booleanLiteral(boolean value) {
        if (type == DatabaseType.MYSQL) {
            return value ? "1" : "0";
        }
        return value ? "true" : "false";
    }

    record SyncResult(int changedEntries) {
    }
}
