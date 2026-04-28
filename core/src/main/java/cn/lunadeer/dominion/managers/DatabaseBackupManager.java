package cn.lunadeer.dominion.managers;

import cn.lunadeer.dominion.Dominion;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.configuration.Language;
import cn.lunadeer.dominion.storage.DatabaseManager;
import cn.lunadeer.dominion.utils.Notification;
import cn.lunadeer.dominion.utils.XLogger;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import cn.lunadeer.dominion.utils.scheduler.Scheduler;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.sql.*;
import java.util.*;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

public class DatabaseBackupManager {
    public static class DatabaseManagerText extends ConfigurationPart {
        public String exportingDatabaseTables = "Exporting database tables...";
        public String exportTableFail = "Export table failed, reason: {0}";
        public String exportWorldMappingFail = "Export world uid mapping failed, reason: {0}";
        public String exportDatabaseSuccess = "Export database to {0} successfully.";

        public String fileNotFound = "Database table file path {0} not found.";
        public String importingDatabase = "Importing database...";
        public String fileCorrupted = "Some database table file is missing, please re-export the database tables.";
        public String convertWorldFailed = "The old world {0}({1}) is unable to find in current save, please make sure the world name is exist in current save.";
        public String importDatabaseFail = "Import database failed, reason: {0}";
        public String importDatabaseSuccess = "Import database successfully.";
    }

    private static final File export_path = new File(Dominion.instance.getDataFolder(), "backup");
    private static final List<String> REQUIRED_IMPORT_TABLES = List.of("player_name", "privilege_template", "dominion", "dominion_group", "dominion_member");
    private static final List<String> EXPORT_TABLES = List.of(
            "player_name",
            "privilege_template",
            "dominion",
            "dominion_group",
            "dominion_member",
            "server_info",
            "tp_cache"
    );

    public static void exportTables(CommandSender sender) {
        Scheduler.runTaskAsync(() -> {
            Notification.info(sender, Language.databaseManagerText.exportingDatabaseTables);
            if (!export_path.exists()) {
                boolean re = export_path.mkdirs();
            }
            try {
                for (String table : EXPORT_TABLES) {
                    exportCsv(table, new File(export_path, table + ".csv"), defaultOrderKey(table));
                }
            } catch (Exception e) {
                Notification.error(sender, Language.databaseManagerText.exportTableFail, e.getMessage());
                return;
            }
            try {
                Map<String, String> world_uid_map = Dominion.instance.getServer().getWorlds().stream().collect(HashMap::new, (m, w) -> m.put(w.getName(), w.getUID().toString()), HashMap::putAll);
                YamlConfiguration world_uid = new YamlConfiguration();
                for (Map.Entry<String, String> entry : world_uid_map.entrySet()) {
                    world_uid.set(entry.getKey(), entry.getValue());
                }
                world_uid.save(new File(export_path, "world_uid_mapping.yml"));
            } catch (Exception e) {
                Notification.error(sender, Language.databaseManagerText.exportWorldMappingFail, e.getMessage());
                return;
            }
            Notification.info(sender, Language.databaseManagerText.exportDatabaseSuccess, export_path.getAbsolutePath());
        });
    }

    public static void importTables(CommandSender sender) {
        Scheduler.runTaskAsync(() -> {
            if (!export_path.exists()) {
                Notification.error(sender, Language.databaseManagerText.fileNotFound, export_path.getAbsolutePath());
                return;
            }
            Notification.info(sender, Language.databaseManagerText.importingDatabase);
            Map<String, String> world_uid_map = Dominion.instance.getServer().getWorlds().stream().collect(HashMap::new, (m, w) -> m.put(w.getName(), w.getUID().toString()), HashMap::putAll);
            for (String table : REQUIRED_IMPORT_TABLES) {
                if (!new File(export_path, table + ".csv").exists()) {
                    Notification.error(sender, Language.databaseManagerText.fileCorrupted);
                    return;
                }
            }
            try {
                for (String table : EXPORT_TABLES) {
                    File csv = new File(export_path, table + ".csv");
                    if (csv.exists()) {
                        importCsv(table, csv);
                    }
                }

                File world_uid_mapping = new File(export_path, "world_uid_mapping.yml");
                if (world_uid_mapping.exists()) {
                    YamlConfiguration world_uid = YamlConfiguration.loadConfiguration(world_uid_mapping);
                    for (String key : world_uid.getKeys(false)) {
                        if (world_uid_map.containsKey(key)) {
                            String old_uid = world_uid.getString(key);
                            World newWorld = Dominion.instance.getServer().getWorld(key);
                            if (newWorld == null || old_uid == null) {
                                Notification.warn(sender, Language.databaseManagerText.convertWorldFailed, key, old_uid);
                                continue;
                            }
                            String new_uid = newWorld.getUID().toString();
                            DatabaseManager.instance.dsl()
                                    .update(table(name("dominion")))
                                    .set(field(name("world_uid"), String.class), new_uid)
                                    .where(field(name("world_uid"), String.class).eq(old_uid))
                                    .execute();
                        }
                    }
                }
            } catch (Exception e) {
                Notification.error(sender, Language.databaseManagerText.importDatabaseFail, e.getMessage());
                XLogger.error(e);
                return;
            }
            Notification.info(sender, Language.databaseManagerText.importDatabaseSuccess);
            CacheManager.instance.reloadCache();
        });
    }

    private static void exportCsv(String tableName, File file, String orderKey) throws SQLException, IOException {
        StringBuilder builder = new StringBuilder();
        try (Connection connection = DatabaseManager.instance.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT * FROM " + tableName + " ORDER BY " + orderKey)) {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                builder.append(metaData.getColumnName(i)).append(",");
            }
            builder.deleteCharAt(builder.length() - 1).append("\n");
            for (int i = 1; i <= columnCount; i++) {
                builder.append(unifiedType(metaData.getColumnType(i))).append(",");
            }
            builder.deleteCharAt(builder.length() - 1).append("\n");
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    Object raw = rs.getObject(i);
                    String value = raw == null ? "" : raw.toString();
                    if (value.contains(",") || value.contains("\"")) {
                        value = "\"" + value.replace("\"", "\"\"") + "\"";
                    }
                    builder.append(value).append(",");
                }
                builder.deleteCharAt(builder.length() - 1).append("\n");
            }
        }
        Files.write(file.toPath(), builder.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void importCsv(String tableName, File file) throws IOException, SQLException {
        XLogger.warn("Importing " + tableName + " from " + file.getAbsolutePath());
        String content = Files.readString(file.toPath());
        String[] lines = content.split("\n");
        if (lines.length < 2) {
            return;
        }
        String[] columns = splitCsvLine(lines[0]).toArray(new String[0]);
        String[] types = splitCsvLine(lines[1]).toArray(new String[0]);
        Set<String> existingColumns = tableColumns(tableName);
        for (int i = 2; i < lines.length; i++) {
            if (lines[i].isBlank()) continue;
            List<String> values = splitCsvLine(lines[i]);
            Map<String, Object> row = new LinkedHashMap<>();
            for (int j = 0; j < columns.length && j < values.size(); j++) {
                row.put(columns[j].trim(), parseValue(types[j].trim(), values.get(j).trim()));
            }
            importRow(tableName, row, existingColumns);
            if ((i - 2) % 100 == 1 || i == lines.length - 1) {
                XLogger.warn("Importing " + tableName + " " + (i - 2) + "/" + (lines.length - 2));
            }
        }
    }

    private static void importRow(String tableName, Map<String, Object> row, Set<String> existingColumns) {
        Table<Record> table = table(name(tableName));
        Map<Field<?>, Object> values = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (!existingColumns.contains(entry.getKey().toLowerCase(Locale.ROOT))) {
                continue;
            }
            Field<Object> field = field(name(entry.getKey()));
            values.put(field, entry.getValue());
        }
        if (!values.isEmpty()) {
            DatabaseManager.instance.dsl().insertInto(table).set(values).onDuplicateKeyIgnore().execute();
        }
    }

    private static Set<String> tableColumns(String tableName) throws SQLException {
        Set<String> columns = new HashSet<>();
        try (Connection connection = DatabaseManager.instance.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet rs = metaData.getColumns(null, null, tableName, "%")) {
                while (rs.next()) {
                    columns.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
                }
            }
            if (columns.isEmpty() && DatabaseManager.instance.getType() == cn.lunadeer.dominion.storage.DatabaseType.SQLITE) {
                try (Statement statement = connection.createStatement();
                     ResultSet rs = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
                    while (rs.next()) {
                        columns.add(rs.getString("name").toLowerCase(Locale.ROOT));
                    }
                }
            }
        }
        return columns;
    }

    private static List<String> splitCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());
        return values;
    }

    private static Object parseValue(String type, String value) {
        if (value.isEmpty()) return null;
        return switch (type) {
            case "BOOLEAN" -> Boolean.parseBoolean(value) || "1".equals(value);
            case "INTEGER" -> Integer.parseInt(value);
            case "LONG" -> Long.parseLong(value);
            case "FLOAT" -> Float.parseFloat(value);
            case "TIMESTAMP" -> Timestamp.valueOf(value);
            default -> value;
        };
    }

    private static String unifiedType(int sqlType) {
        return switch (sqlType) {
            case Types.BOOLEAN, Types.BIT, Types.TINYINT -> "BOOLEAN";
            case Types.INTEGER, Types.SMALLINT -> "INTEGER";
            case Types.BIGINT -> "LONG";
            case Types.FLOAT, Types.REAL, Types.DOUBLE, Types.DECIMAL, Types.NUMERIC -> "FLOAT";
            case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE, Types.DATE, Types.TIME -> "TIMESTAMP";
            default -> "STRING";
        };
    }

    private static String defaultOrderKey(String table) {
        return switch (table) {
            case "tp_cache" -> "uuid";
            default -> "id";
        };
    }
}
