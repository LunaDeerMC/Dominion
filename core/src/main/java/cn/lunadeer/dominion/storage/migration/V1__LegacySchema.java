package cn.lunadeer.dominion.storage.migration;

import cn.lunadeer.dominion.Dominion;
import cn.lunadeer.dominion.configuration.Configuration;
import cn.lunadeer.dominion.storage.DatabaseType;
import org.bukkit.World;
import org.flywaydb.core.api.migration.Context;

import java.sql.*;
import java.util.UUID;

public class V1__LegacySchema extends AbstractJavaMigration {

    public V1__LegacySchema(DatabaseType type) {
        super(type);
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        createCoreTables(connection);
        upgradeLegacyColumns(connection);
        migratePlayerPrivilege(connection);
        seedRootRows(connection);
    }

    private void createCoreTables(Connection connection) throws SQLException {
        execute(connection, "CREATE TABLE IF NOT EXISTS player_name (" +
                "id " + autoId() + ", " +
                "uuid " + uuidText() + " NOT NULL UNIQUE, " +
                "last_known_name " + text() + " NOT NULL DEFAULT 'unknown', " +
                "last_join_at " + timestamp() + " NOT NULL DEFAULT '1970-01-01 00:00:00', " +
                "using_group_title_id INT NOT NULL DEFAULT -1, " +
                "skin_url " + longText() + " NOT NULL DEFAULT '" + DEFAULT_SKIN_URL + "', " +
                "ui_preference " + text() + " NOT NULL DEFAULT 'TUI'" +
                ")");

        execute(connection, "CREATE TABLE IF NOT EXISTS dominion (" +
                "id " + autoId() + ", " +
                "owner " + uuidText() + " NOT NULL, " +
                "name " + text() + " NOT NULL DEFAULT 'Unnamed', " +
                "world_uid " + uuidText() + " NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000', " +
                "x1 INT NOT NULL DEFAULT 0, y1 INT NOT NULL DEFAULT 0, z1 INT NOT NULL DEFAULT 0, " +
                "x2 INT NOT NULL DEFAULT 0, y2 INT NOT NULL DEFAULT 0, z2 INT NOT NULL DEFAULT 0, " +
                "parent_dom_id INT NOT NULL DEFAULT -1, " +
                "join_message " + longText() + " NOT NULL DEFAULT '&3{OWNER}: Welcome to {DOM}!', " +
                "leave_message " + longText() + " NOT NULL DEFAULT '&3{OWNER}: Leaving {DOM}...', " +
                "tp_location " + text() + " NOT NULL DEFAULT 'default', " +
                "color " + text() + " NOT NULL DEFAULT '#00BFFF', " +
                "server_id INT NOT NULL DEFAULT " + Configuration.multiServer.serverId + ", " +
                "owner_glow " + bool() + " NOT NULL DEFAULT " + falseLiteral() +
                ")");

        execute(connection, "CREATE TABLE IF NOT EXISTS privilege_template (" +
                "id " + autoId() + ", " +
                "creator " + uuidText() + " NOT NULL, " +
                "name " + text() + " NOT NULL DEFAULT 'Unnamed'" +
                ")");

        execute(connection, "CREATE TABLE IF NOT EXISTS dominion_group (" +
                "id " + autoId() + ", " +
                "dom_id INT NOT NULL, " +
                "name " + text() + " NOT NULL DEFAULT 'Unnamed', " +
                "name_colored " + text() + " NOT NULL DEFAULT 'Unnamed'" +
                ")");

        execute(connection, "CREATE TABLE IF NOT EXISTS dominion_member (" +
                "id " + autoId() + ", " +
                "player_uuid " + uuidText() + " NOT NULL, " +
                "dom_id INT NOT NULL, " +
                "group_id INT NOT NULL DEFAULT -1" +
                ")");

        execute(connection, "CREATE TABLE IF NOT EXISTS server_info (" +
                "id " + autoId() + ", " +
                "name " + text() + " NOT NULL DEFAULT 'Unnamed'" +
                ")");

        execute(connection, "CREATE TABLE IF NOT EXISTS tp_cache (" +
                "uuid " + uuidText() + " PRIMARY KEY, " +
                "dom_id INT NOT NULL" +
                ")");
    }

    private void upgradeLegacyColumns(Connection connection) throws SQLException {
        addColumnIfMissing(connection, "player_name", "last_known_name " + text() + " NOT NULL DEFAULT 'unknown'");
        boolean hadLastJoinAt = columnExists(connection, "player_name", "last_join_at");
        addColumnIfMissing(connection, "player_name", "last_join_at " + timestamp() + " NOT NULL DEFAULT '1970-01-01 00:00:00'");
        if (!hadLastJoinAt) {
            execute(connection, "UPDATE player_name SET last_join_at = CURRENT_TIMESTAMP WHERE uuid <> '00000000-0000-0000-0000-000000000000' AND last_join_at = '1970-01-01 00:00:00'");
        }
        addColumnIfMissing(connection, "player_name", "using_group_title_id INT NOT NULL DEFAULT -1");
        addColumnIfMissing(connection, "player_name", "skin_url " + longText() + " NOT NULL DEFAULT '" + DEFAULT_SKIN_URL + "'");
        addColumnIfMissing(connection, "player_name", "ui_preference " + text() + " NOT NULL DEFAULT 'TUI'");
        execute(connection, "UPDATE player_name SET ui_preference = 'CUI' WHERE uuid LIKE '00000000%' AND (ui_preference IS NULL OR ui_preference = 'TUI')");

        if (tableExists(connection, "dominion")) {
            addColumnIfMissing(connection, "dominion", "world_uid " + uuidText() + " NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000'");
            if (columnExists(connection, "dominion", "world")) {
                migrateWorldNameToUid(connection);
            }
            addColumnIfMissing(connection, "dominion", "tp_location " + text() + " NOT NULL DEFAULT 'default'");
            addColumnIfMissing(connection, "dominion", "color " + text() + " NOT NULL DEFAULT '#00BFFF'");
            addColumnIfMissing(connection, "dominion", "server_id INT NOT NULL DEFAULT " + Configuration.multiServer.serverId);
            addColumnIfMissing(connection, "dominion", "owner_glow " + bool() + " NOT NULL DEFAULT " + falseLiteral());
            execute(connection, "UPDATE dominion SET server_id = -1 WHERE id = -1");
        }

        if (tableExists(connection, "dominion_group")) {
            addColumnIfMissing(connection, "dominion_group", "name_colored " + text() + " NOT NULL DEFAULT 'Unnamed'");
            execute(connection, "UPDATE dominion_group SET name_colored = name WHERE name_colored IS NULL OR name_colored = 'Unnamed'");
        }
        if (tableExists(connection, "dominion_member")) {
            addColumnIfMissing(connection, "dominion_member", "group_id INT NOT NULL DEFAULT -1");
        }
        if (tableExists(connection, "player_privilege")) {
            addColumnIfMissing(connection, "player_privilege", "group_id INT NOT NULL DEFAULT -1");
        }
    }

    private void migrateWorldNameToUid(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT id, world FROM dominion")) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String worldName = rs.getString("world");
                String worldUid = "00000000-0000-0000-0000-000000000000";
                if (!"all".equals(worldName)) {
                    World world = Dominion.instance.getServer().getWorld(worldName);
                    if (world != null) {
                        worldUid = world.getUID().toString();
                    }
                }
                try (PreparedStatement update = connection.prepareStatement("UPDATE dominion SET world_uid = ? WHERE id = ?")) {
                    update.setString(1, worldUid);
                    update.setInt(2, id);
                    update.executeUpdate();
                }
            }
        }
    }

    private void migratePlayerPrivilege(Connection connection) throws SQLException {
        if (!tableExists(connection, "player_privilege")) {
            return;
        }
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT id, player_uuid, dom_id, group_id FROM player_privilege")) {
            while (rs.next()) {
                if (existsById(connection, "dominion_member", rs.getInt("id"))) {
                    continue;
                }
                try (PreparedStatement insert = connection.prepareStatement("INSERT INTO dominion_member (id, player_uuid, dom_id, group_id) VALUES (?, ?, ?, ?)")) {
                    insert.setInt(1, rs.getInt("id"));
                    insert.setString(2, rs.getString("player_uuid"));
                    insert.setInt(3, rs.getInt("dom_id"));
                    insert.setInt(4, rs.getInt("group_id"));
                    insert.executeUpdate();
                }
            }
        }
    }

    private void seedRootRows(Connection connection) throws SQLException {
        if (!existsById(connection, "player_name", -1)) {
            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO player_name (id, uuid, last_known_name, last_join_at, using_group_title_id, skin_url, ui_preference) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                insert.setInt(1, -1);
                insert.setString(2, "00000000-0000-0000-0000-000000000000");
                insert.setString(3, "server");
                insert.setTimestamp(4, Timestamp.valueOf("1970-01-01 00:00:00"));
                insert.setInt(5, -1);
                insert.setString(6, DEFAULT_SKIN_URL);
                insert.setString(7, "TUI");
                insert.executeUpdate();
            }
        }
        if (!existsById(connection, "dominion", -1)) {
            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO dominion (id, owner, name, world_uid, x1, y1, z1, x2, y2, z2, parent_dom_id, join_message, leave_message, tp_location, color, server_id, owner_glow) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                insert.setInt(1, -1);
                insert.setString(2, UUID.fromString("00000000-0000-0000-0000-000000000000").toString());
                insert.setString(3, "根领地");
                insert.setString(4, "00000000-0000-0000-0000-000000000000");
                insert.setInt(5, Integer.MIN_VALUE);
                insert.setInt(6, Integer.MIN_VALUE);
                insert.setInt(7, Integer.MIN_VALUE);
                insert.setInt(8, Integer.MAX_VALUE);
                insert.setInt(9, Integer.MAX_VALUE);
                insert.setInt(10, Integer.MAX_VALUE);
                insert.setInt(11, -1);
                insert.setString(12, "&3{OWNER}: Welcome to {DOM}!");
                insert.setString(13, "&3{OWNER}: Leaving {DOM}...");
                insert.setString(14, "default");
                insert.setString(15, "#00BFFF");
                insert.setInt(16, -1);
                insert.setBoolean(17, false);
                insert.executeUpdate();
            }
        }
    }

    private boolean existsById(Connection connection, String table, int id) throws SQLException {
        if (!tableExists(connection, table)) {
            return false;
        }
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM " + table + " WHERE id = ?")) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private String falseLiteral() {
        return type == DatabaseType.MYSQL ? "0" : "false";
    }
}
