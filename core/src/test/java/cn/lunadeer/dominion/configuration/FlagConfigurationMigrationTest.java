package cn.lunadeer.dominion.configuration;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.FlagGroups;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlagConfigurationMigrationTest {

    @Test
    void splitDefinitionInheritsLegacyDefaultAndEnableOnlyOnce() {
        boolean oldDefault = Flags.MONSTER_SPAWN_EGG.getDefaultValue();
        boolean oldEnable = Flags.MONSTER_SPAWN_EGG.getEnable();
        try {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set(Flags.MONSTER_SPAWN.getConfigurationDefaultKey(), false);
            yaml.set(Flags.MONSTER_SPAWN.getConfigurationEnableKey(), false);

            FlagConfiguration.reconcileFlagDefinitions(yaml, true);
            assertFalse(yaml.getBoolean(Flags.MONSTER_SPAWN_EGG.getConfigurationDefaultKey()));
            assertFalse(yaml.getBoolean(Flags.MONSTER_SPAWN_EGG.getConfigurationEnableKey()));

            yaml.set(Flags.MONSTER_SPAWN_EGG.getConfigurationDefaultKey(), true);
            yaml.set(Flags.MONSTER_SPAWN_EGG.getConfigurationEnableKey(), true);
            FlagConfiguration.reconcileFlagDefinitions(yaml, true);
            assertTrue(Flags.MONSTER_SPAWN_EGG.getDefaultValue());
            assertTrue(Flags.MONSTER_SPAWN_EGG.getEnable());
        } finally {
            Flags.MONSTER_SPAWN_EGG.setDefaultValue(oldDefault);
            Flags.MONSTER_SPAWN_EGG.setEnable(oldEnable);
        }
    }

    @Test
    void flagsConfigurationStoresOnlyGroupStructureAndMaterial() {
        YamlConfiguration yaml = new YamlConfiguration();
        FlagGroups.replaceConfiguredGroups(
                FlagGroups.defaultEnvironmentGroups(),
                FlagGroups.defaultPrivilegeGroups()
        );

        FlagConfiguration.writeConfiguredFlagGroups(yaml);

        String path = "groups.environment.creature-spawning";
        assertNull(yaml.get(path + ".display-name"));
        assertNull(yaml.get(path + ".description"));
        assertTrue(yaml.isString(path + ".material"));
        assertTrue(yaml.isList(path + ".flags"));
    }
}
