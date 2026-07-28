package cn.lunadeer.dominion.configuration;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldWideFlagMigrationTest {

    @TempDir
    Path tempDir;

    @Test
    void migratesLegacyValuesAndDoesNotOverwriteThemAgain() throws Exception {
        File file = tempDir.resolve("migration-test.yml").toFile();
        YamlConfiguration legacy = new YamlConfiguration();
        legacy.set("enabled", true);
        legacy.set(Flags.ANIMAL_SPAWN.getConfigurationNameKey(), false);
        legacy.set(Flags.MONSTER_SPAWN.getConfigurationNameKey(), false);
        legacy.set(Flags.VILLAGER_SPAWN.getConfigurationNameKey(), false);
        legacy.set(Flags.ENDER_MAN.getConfigurationNameKey(), true);
        legacy.set(Flags.PLACE.getConfigurationNameKey(), true);
        legacy.save(file);

        WorldWide.loadWorld(file);
        YamlConfiguration migrated = YamlConfiguration.loadConfiguration(file);
        assertEquals(2, migrated.getInt("flag-schema-version"));
        assertTrue(migrated.getBoolean(Flags.ANIMAL_SPAWN_EGG.getConfigurationNameKey()));
        assertTrue(migrated.getBoolean(Flags.MONSTER_SPAWN_EGG.getConfigurationNameKey()));
        assertFalse(migrated.getBoolean(Flags.VILLAGER_SPAWN_EGG.getConfigurationNameKey()));
        assertTrue(migrated.getBoolean(Flags.ENDER_MAN_SPAWN.getConfigurationNameKey()));
        assertTrue(migrated.getBoolean(Flags.PLACE_LIQUID.getConfigurationNameKey()));

        String firstMigration = Files.readString(file.toPath());
        WorldWide.loadWorld(file);
        assertEquals(firstMigration, Files.readString(file.toPath()));
    }
}
