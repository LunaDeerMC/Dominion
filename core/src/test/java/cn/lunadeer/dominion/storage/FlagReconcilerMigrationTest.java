package cn.lunadeer.dominion.storage;

import cn.lunadeer.dominion.api.dtos.flag.Flag;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlagReconcilerMigrationTest {

    @TempDir
    Path tempDir;

    @Test
    void copiesSplitColumnsAcrossEveryTableAndIsIdempotent() throws Exception {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("flags.db"));
        createLegacyTables(dataSource);

        FlagReconciler reconciler = new FlagReconciler(dataSource, DatabaseType.SQLITE);
        assertTrue(reconciler.reconcile().changedEntries() > 0);

        try (Connection connection = dataSource.getConnection()) {
            assertMigrated(connection, "dominion", Flags.getAllFlags());
            assertMigrated(connection, "dominion_member", Flags.getAllPriFlags());
            assertMigrated(connection, "dominion_group", Flags.getAllPriFlags());
            assertMigrated(connection, "privilege_template", Flags.getAllPriFlags());
        }
        assertEquals(0, reconciler.reconcile().changedEntries());
    }

    private static void createLegacyTables(SQLiteDataSource dataSource) throws Exception {
        Set<Flag> environmentAndPrivilegeSources = legacySources(Flags.getAllFlags());
        Set<Flag> privilegeSources = legacySources(Flags.getAllPriFlags());
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
            if (legacySources(Flags.getAllFlags()).contains(target)) continue;
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
