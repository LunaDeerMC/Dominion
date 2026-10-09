package cn.lunadeer.dominion.storage;

import cn.lunadeer.dominion.api.dtos.flag.Flag;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

class FlagReconcilerMigrationTest {

    @TempDir
    Path tempDir;

    @Test
    void customDefaultsApplyToNewColumnsAndNullBackfills() throws Exception {
        EnvFlag added = customDefaultFlag("custom_added");
        EnvFlag nullable = customDefaultFlag("custom_nullable");
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("custom-defaults.db"));
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE dominion (id INTEGER PRIMARY KEY, custom_nullable BOOLEAN)");
            statement.execute("INSERT INTO dominion (id) VALUES (1)");
        }

        try (var registry = mockStatic(Flags.class, CALLS_REAL_METHODS)) {
            registry.when(Flags::getActiveFlags).thenReturn(List.of(added, nullable));
            registry.when(Flags::getActiveEnvFlags).thenReturn(List.of(added, nullable));
            registry.when(Flags::getActivePriFlags).thenReturn(List.of());
            new FlagReconciler(dataSource, DatabaseType.SQLITE).reconcile();
        }

        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            try (ResultSet result = statement.executeQuery("SELECT custom_added, custom_nullable FROM dominion WHERE id = 1")) {
                assertTrue(result.next());
                assertTrue(result.getBoolean("custom_added"));
                assertTrue(result.getBoolean("custom_nullable"));
            }
            statement.execute("INSERT INTO dominion (id) VALUES (2)");
            try (ResultSet result = statement.executeQuery("SELECT custom_added FROM dominion WHERE id = 2")) {
                assertTrue(result.next());
                assertTrue(result.getBoolean("custom_added"));
            }
        }
    }

    private static EnvFlag customDefaultFlag(String name) {
        return new EnvFlag(name, name, name, false, true, Material.STONE) {
            @Override
            public Boolean getDefaultValue() {
                return true;
            }
        };
    }

    @Test
    void copiesSplitColumnsAcrossEveryTableAndIsIdempotent() throws Exception {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("flags.db"));
        createLegacyTables(dataSource);

        FlagReconciler reconciler = new FlagReconciler(dataSource, DatabaseType.SQLITE);
        assertTrue(reconciler.reconcile().changedEntries() > 0);

        try (Connection connection = dataSource.getConnection()) {
            assertMigrated(connection, "dominion", Flags.getActiveFlags());
            assertMigrated(connection, "dominion_member", Flags.getActivePriFlags());
            assertMigrated(connection, "dominion_group", Flags.getActivePriFlags());
            assertMigrated(connection, "privilege_template", Flags.getActivePriFlags());
        }
        assertEquals(0, reconciler.reconcile().changedEntries());
    }

    @Test
    void newFireworkFlagDefaultsToFalseAndPreservesExplicitChanges() throws Exception {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("firework-flags.db"));
        try (Connection connection = dataSource.getConnection()) {
            createTableWithoutFlag(connection, "dominion", Flags.getActiveFlags(), Flags.FIREWORK_DAMAGE_ENTITY);
            createTableWithoutFlag(connection, "dominion_member", Flags.getActivePriFlags(), Flags.FIREWORK_DAMAGE_ENTITY);
            createTableWithoutFlag(connection, "dominion_group", Flags.getActivePriFlags(), Flags.FIREWORK_DAMAGE_ENTITY);
            createTableWithoutFlag(connection, "privilege_template", Flags.getActivePriFlags(), Flags.FIREWORK_DAMAGE_ENTITY);
            connection.createStatement().execute("INSERT INTO dominion (id) VALUES (1)");
        }

        FlagReconciler reconciler = new FlagReconciler(dataSource, DatabaseType.SQLITE);
        assertEquals(1, reconciler.reconcile().changedEntries());
        try (Connection connection = dataSource.getConnection()) {
            assertFireworkFlag(connection, 1, false);
            connection.createStatement().execute("INSERT INTO dominion (id) VALUES (2)");
            assertFireworkFlag(connection, 2, false);
            connection.createStatement().execute(
                    "UPDATE dominion SET firework_damage_entity = true WHERE id = 1");
        }

        assertEquals(0, reconciler.reconcile().changedEntries());
        try (Connection connection = dataSource.getConnection()) {
            assertFireworkFlag(connection, 1, true);
            assertFireworkFlag(connection, 2, false);
        }
        assertEquals(0, reconciler.reconcile().changedEntries());
    }

    @Test
    void newNetherPortalFlagProtectsExistingAndNewRowsAndPreservesExplicitChanges() throws Exception {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("nether-portal-flags.db"));
        try (Connection connection = dataSource.getConnection()) {
            createTableWithoutFlag(connection, "dominion", Flags.getActiveFlags(), Flags.NETHER_PORTAL_CREATE);
            createTableWithoutFlag(connection, "dominion_member", Flags.getActivePriFlags(), Flags.NETHER_PORTAL_CREATE);
            createTableWithoutFlag(connection, "dominion_group", Flags.getActivePriFlags(), Flags.NETHER_PORTAL_CREATE);
            createTableWithoutFlag(connection, "privilege_template", Flags.getActivePriFlags(), Flags.NETHER_PORTAL_CREATE);
            connection.createStatement().execute("INSERT INTO dominion (id) VALUES (1)");
        }

        FlagReconciler reconciler = new FlagReconciler(dataSource, DatabaseType.SQLITE);
        assertEquals(1, reconciler.reconcile().changedEntries());
        try (Connection connection = dataSource.getConnection()) {
            assertNetherPortalFlag(connection, 1, false);
            connection.createStatement().execute("INSERT INTO dominion (id) VALUES (2)");
            assertNetherPortalFlag(connection, 2, false);
            connection.createStatement().execute(
                    "UPDATE dominion SET nether_portal_create = true WHERE id = 1");
        }

        assertEquals(0, reconciler.reconcile().changedEntries());
        try (Connection connection = dataSource.getConnection()) {
            assertNetherPortalFlag(connection, 1, true);
            assertNetherPortalFlag(connection, 2, false);
        }
        assertEquals(0, reconciler.reconcile().changedEntries());
    }

    private static void createTableWithoutFlag(Connection connection,
                                               String table,
                                               List<? extends Flag> flags,
                                               Flag missingFlag) throws Exception {
        StringBuilder sql = new StringBuilder("CREATE TABLE ").append(table).append(" (id INTEGER PRIMARY KEY");
        for (Flag flag : flags) {
            if (flag == missingFlag) continue;
            // Existing allowances must not enable the new protection flag.
            sql.append(", ").append(flag.getFlagName()).append(" BOOLEAN NOT NULL DEFAULT true");
        }
        sql.append(')');
        connection.createStatement().execute(sql.toString());
    }

    private static void assertNetherPortalFlag(Connection connection, int id, boolean expected) throws Exception {
        try (var statement = connection.prepareStatement(
                "SELECT nether_portal_create, place, ignite, teleport FROM dominion WHERE id = ?")) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(expected, result.getBoolean("nether_portal_create"));
                assertTrue(result.getBoolean("place"));
                assertTrue(result.getBoolean("ignite"));
                assertTrue(result.getBoolean("teleport"));
                assertFalse(result.next());
            }
        }
    }

    private static void assertFireworkFlag(Connection connection, int id, boolean expected) throws Exception {
        try (var statement = connection.prepareStatement(
                "SELECT firework_damage_entity, tnt_damage_entity, creeper_damage_entity, fireball_damage_entity "
                        + "FROM dominion WHERE id = ?")) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(expected, result.getBoolean("firework_damage_entity"));
                assertTrue(result.getBoolean("tnt_damage_entity"));
                assertTrue(result.getBoolean("creeper_damage_entity"));
                assertTrue(result.getBoolean("fireball_damage_entity"));
                assertFalse(result.next());
            }
        }
    }

    private static void createLegacyTables(SQLiteDataSource dataSource) throws Exception {
        Set<Flag> environmentAndPrivilegeSources = legacySources(Flags.getActiveFlags());
        Set<Flag> privilegeSources = legacySources(Flags.getActivePriFlags());
        try (Connection connection = dataSource.getConnection()) {
            createTable(connection, "dominion", environmentAndPrivilegeSources);
            createTable(connection, "dominion_member", privilegeSources);
            createTable(connection, "dominion_group", privilegeSources);
            createTable(connection, "privilege_template", privilegeSources);
        }
    }

    private static Set<Flag> legacySources(List<? extends Flag> flags) {
        Set<Flag> sources = new LinkedHashSet<>();
        for (Flag flag : flags) {
            sources.addAll(Flags.getLegacySources(flag));
        }
        return sources;
    }

    private static void createTable(Connection connection, String table, Set<Flag> sources) throws Exception {
        StringBuilder sql = new StringBuilder("CREATE TABLE ").append(table).append(" (id INTEGER PRIMARY KEY");
        for (Flag source : sources) {
            sql.append(", ").append(source.getFlagName()).append(" BOOLEAN NOT NULL");
        }
        sql.append(')');
        connection.createStatement().execute(sql.toString());

        StringBuilder columns = new StringBuilder("id");
        StringBuilder values = new StringBuilder("1");
        int index = 0;
        for (Flag source : sources) {
            columns.append(", ").append(source.getFlagName());
            values.append(", ").append(index++ % 2 == 0 ? "true" : "false");
        }
        connection.createStatement().execute(
                "INSERT INTO " + table + " (" + columns + ") VALUES (" + values + ")"
        );
    }

    private static void assertMigrated(Connection connection,
                                       String table,
                                       List<? extends Flag> flags) throws Exception {
        for (Flag target : flags) {
            // These flags are themselves part of the legacy schema and are
            // already present in the synthetic legacy tables.
            if (legacySources(Flags.getActiveFlags()).contains(target)) continue;
            List<Flag> sources = Flags.getLegacySources(target);
            if (sources.isEmpty()) {
                continue;
            }
            String columns = sources.stream().map(Flag::getFlagName).collect(java.util.stream.Collectors.joining(", "));
            try (ResultSet result = connection.createStatement().executeQuery(
                    "SELECT " + columns + ", " + target.getFlagName() + " FROM " + table + " WHERE id = 1")) {
                assertTrue(result.next());
                boolean expected = true;
                for (int index = 1; index <= sources.size(); index++) {
                    expected &= result.getBoolean(index);
                }
                if (Flags.preserveAllowedSpawnEggValue(target)) expected = true;
                assertEquals(expected, result.getBoolean(sources.size() + 1),
                        table + "." + target.getFlagName());
            }
        }
    }
}
