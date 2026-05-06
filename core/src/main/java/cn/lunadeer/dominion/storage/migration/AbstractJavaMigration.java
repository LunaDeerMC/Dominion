package cn.lunadeer.dominion.storage.migration;

import cn.lunadeer.dominion.storage.DatabaseType;
import org.flywaydb.core.api.migration.BaseJavaMigration;

import java.sql.*;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

abstract class AbstractJavaMigration extends BaseJavaMigration {
    protected static final String DEFAULT_SKIN_URL = "http://textures.minecraft.net/texture/613ba1403f98221fab6f4ae0f9e5298068262258966e8f9e53cdedd97aa45ef1";

    protected final DatabaseType type;

    protected AbstractJavaMigration(DatabaseType type) {
        this.type = type;
    }

    protected void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    protected boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String normalized = tableName.toLowerCase(Locale.ROOT);
        try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                if (normalized.equals(rs.getString("TABLE_NAME").toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
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
            try (Statement statement = connection.createStatement();
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

    protected Set<String> columns(Connection connection, String tableName) throws SQLException {
        Set<String> columns = new HashSet<>();
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getColumns(null, null, tableName, "%")) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
            }
        }
        if (columns.isEmpty() && type == DatabaseType.SQLITE) {
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
                while (rs.next()) {
                    columns.add(rs.getString("name").toLowerCase(Locale.ROOT));
                }
            }
        }
        return columns;
    }

    protected boolean indexExists(Connection connection, String tableName, String indexName) throws SQLException {
        String normalized = indexName.toLowerCase(Locale.ROOT);
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getIndexInfo(null, null, tableName, false, false)) {
            while (rs.next()) {
                String name = rs.getString("INDEX_NAME");
                if (name != null && normalized.equals(name.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void addColumnIfMissing(Connection connection, String tableName, String columnDefinition) throws SQLException {
        String columnName = columnDefinition.trim().split("\\s+")[0];
        if (!columnExists(connection, tableName, columnName)) {
            execute(connection, "ALTER TABLE " + tableName + " ADD COLUMN " + columnDefinition);
        }
    }

    protected String autoId() {
        return switch (type) {
            case MYSQL -> "INT NOT NULL AUTO_INCREMENT PRIMARY KEY";
            case PGSQL -> "SERIAL PRIMARY KEY";
            case SQLITE -> "INTEGER PRIMARY KEY AUTOINCREMENT";
        };
    }

    protected String text() {
        return type == DatabaseType.MYSQL ? "VARCHAR(255)" : "TEXT";
    }

    protected String longText() {
        return type == DatabaseType.MYSQL ? "VARCHAR(512)" : "TEXT";
    }

    protected String uuidText() {
        return "VARCHAR(36)";
    }

    protected String bool() {
        return type == DatabaseType.MYSQL ? "TINYINT(1)" : "BOOLEAN";
    }

    protected String timestamp() {
        return type == DatabaseType.MYSQL ? "DATETIME" : "TIMESTAMP";
    }

    protected boolean toBoolean(Object value, boolean defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Boolean b) return b;
        if (value instanceof Number number) return number.intValue() != 0;
        String text = value.toString();
        return "1".equals(text) || "true".equalsIgnoreCase(text) || "t".equalsIgnoreCase(text);
    }
}
