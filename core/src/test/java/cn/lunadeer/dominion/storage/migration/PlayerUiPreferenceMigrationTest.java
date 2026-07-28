package cn.lunadeer.dominion.storage.migration;

import cn.lunadeer.dominion.storage.DatabaseType;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerUiPreferenceMigrationTest {
    @TempDir
    Path tempDir;

    @Test
    void addsDefaultedPreferenceWithoutChangingExistingPlayerData() throws Exception {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("ui-preference.db"));
        try (Connection connection = dataSource.getConnection()) {
            connection.createStatement().execute(
                    "CREATE TABLE player_name (id INTEGER PRIMARY KEY, uuid TEXT NOT NULL, last_known_name TEXT)");
            connection.createStatement().execute(
                    "INSERT INTO player_name (id, uuid, last_known_name) VALUES (1, 'player', 'Luna')");

            Context context = new Context() {
                @Override
                public Configuration getConfiguration() {
                    return null;
                }

                @Override
                public Connection getConnection() {
                    return connection;
                }
            };
            V3__PlayerUiPreference migration = new V3__PlayerUiPreference(DatabaseType.SQLITE);
            migration.migrate(context);
            migration.migrate(context);

            try (ResultSet result = connection.createStatement().executeQuery(
                    "SELECT last_known_name, ui_preference FROM player_name WHERE id = 1")) {
                assertTrue(result.next());
                assertEquals("Luna", result.getString("last_known_name"));
                assertEquals("DEFAULT", result.getString("ui_preference"));
            }
        }
    }
}
