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
        legacy.set(Flags.BLOCK_EXPLODE.getConfigurationNameKey(), false);
        legacy.set(Flags.BURN.getConfigurationNameKey(), false);
        legacy.set(Flags.FLOW_IN_PROTECTION.getConfigurationNameKey(), false);
        legacy.set(Flags.ICE_FORM.getConfigurationNameKey(), false);
        legacy.set(Flags.ARMOR_STAND_EXPLOSION_DAMAGE.getConfigurationNameKey(), false);
        legacy.set(Flags.HANGING_ENTITY_EXPLOSION_DAMAGE.getConfigurationNameKey(), false);
        legacy.set(Flags.ARMOR_STAND_PLAYER_DAMAGE.getConfigurationNameKey(), false);
        legacy.set(Flags.HANGING_ENTITY_PLAYER_DAMAGE.getConfigurationNameKey(), false);
        legacy.set(Flags.PLACE.getConfigurationNameKey(), true);
        legacy.set(Flags.CONTAINER.getConfigurationNameKey(), false);
        legacy.set(Flags.HOPPER.getConfigurationNameKey(), true);
        legacy.save(file);

        WorldWide.loadWorld(file);
        YamlConfiguration migrated = YamlConfiguration.loadConfiguration(file);
        assertEquals(5, migrated.getInt("flag-schema-version"));
        assertTrue(migrated.getBoolean(Flags.ANIMAL_SPAWN_EGG.getConfigurationNameKey()));
        assertTrue(migrated.getBoolean(Flags.MONSTER_SPAWN_EGG.getConfigurationNameKey()));
        assertFalse(migrated.getBoolean(Flags.VILLAGER_SPAWN_EGG.getConfigurationNameKey()));
        assertTrue(migrated.getBoolean(Flags.ENDER_MAN_SPAWN.getConfigurationNameKey()));
        assertTrue(migrated.getBoolean(Flags.ENDER_MAN_PICKUP_BLOCK.getConfigurationNameKey()));
        assertTrue(migrated.getBoolean(Flags.ENDER_MAN_PLACE_BLOCK.getConfigurationNameKey()));
        assertFalse(migrated.getBoolean(Flags.BED_EXPLODE.getConfigurationNameKey()));
        assertFalse(migrated.getBoolean(Flags.ANCHOR_EXPLODE.getConfigurationNameKey()));
        assertFalse(migrated.getBoolean(Flags.BURN_BLOCK.getConfigurationNameKey()));
        assertFalse(migrated.getBoolean(Flags.BURN_ENTITY_FIRE.getConfigurationNameKey()));
        assertFalse(migrated.getBoolean(Flags.BURN_ENTITY_LAVA.getConfigurationNameKey()));
        assertFalse(migrated.getBoolean(Flags.FLOW_IN_WATER.getConfigurationNameKey()));
        assertFalse(migrated.getBoolean(Flags.FLOW_IN_LAVA.getConfigurationNameKey()));
        assertFalse(migrated.getBoolean(Flags.ICE_FORM_NATURAL.getConfigurationNameKey()));
        assertFalse(migrated.getBoolean(Flags.ICE_FORM_FROST_WALKER.getConfigurationNameKey()));
        assertFalse(migrated.getBoolean(Flags.TNT_DAMAGE_ARMOR_STAND.getConfigurationNameKey()));
        assertFalse(migrated.getBoolean(Flags.TNT_DAMAGE_HANGING_ENTITY.getConfigurationNameKey()));
        assertFalse(migrated.getBoolean(Flags.ARMOR_STAND_DIRECT_BREAK.getConfigurationNameKey()));
        assertFalse(migrated.getBoolean(Flags.HANGING_ENTITY_PROJECTILE_BREAK.getConfigurationNameKey()));
        assertTrue(migrated.getBoolean(Flags.PLACE_LIQUID.getConfigurationNameKey()));
        assertFalse(migrated.getBoolean(Flags.CHEST.getConfigurationNameKey()));
        assertFalse(migrated.getBoolean(Flags.BARREL.getConfigurationNameKey()));
        assertTrue(migrated.getBoolean(Flags.HOPPER.getConfigurationNameKey()));
        assertTrue(migrated.getBoolean(Flags.DROPPER.getConfigurationNameKey()));

        String firstMigration = Files.readString(file.toPath());
        WorldWide.loadWorld(file);
        assertEquals(firstMigration, Files.readString(file.toPath()));
    }

    @Test
    void burnEntitySplitPrefersTheIntermediateLegacyColumn() throws Exception {
        File file = tempDir.resolve("burn-migration-test.yml").toFile();
        YamlConfiguration legacy = new YamlConfiguration();
        legacy.set("enabled", true);
        legacy.set("flag-schema-version", 4);
        legacy.set(Flags.BURN_ENTITY.getConfigurationNameKey(), true);
        legacy.set(Flags.BURN.getConfigurationNameKey(), false);
        legacy.save(file);

        WorldWide.loadWorld(file);
        YamlConfiguration migrated = YamlConfiguration.loadConfiguration(file);

        assertTrue(migrated.getBoolean(Flags.BURN_ENTITY_FIRE.getConfigurationNameKey()));
        assertTrue(migrated.getBoolean(Flags.BURN_ENTITY_LAVA.getConfigurationNameKey()));
    }
}
