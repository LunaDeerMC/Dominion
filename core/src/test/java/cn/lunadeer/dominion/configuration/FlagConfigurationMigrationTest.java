package cn.lunadeer.dominion.configuration;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.FlagGroups;
import cn.lunadeer.dominion.api.dtos.flag.EnvFlagGroup;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void flagsConfigurationStoresGroupStructureMaterialAndDialogUiIcon() {
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
        assertTrue(yaml.isString(path + ".dialog-ui-icon"));
        assertNull(yaml.get(path + ".icon"));
        assertTrue(yaml.isList(path + ".flags"));
    }

    @Test
    void missingFlagDialogUiIconUsesDeclaredDefaultAndValidOverrideWins() {
        String materialKey = Flags.MONSTER_SPAWN.getConfigurationMaterialKey();
        String iconKey = Flags.MONSTER_SPAWN.getConfigurationDialogUiIconKey();
        MaterialState state = new MaterialState(Flags.MONSTER_SPAWN.getMaterial().name(),
                Flags.MONSTER_SPAWN.getIcon());
        try {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set(materialKey, "DIAMOND");
            FlagConfiguration.reconcileFlagDefinitions(yaml, false);

            assertEquals("minecraft:items/item/zombie_spawn_egg", yaml.getString(iconKey));
            assertEquals("minecraft:items/item/zombie_spawn_egg", Flags.MONSTER_SPAWN.getIcon());

            yaml.set(iconKey, "custom:atlas/sprite");
            FlagConfiguration.reconcileFlagDefinitions(yaml, false);
            assertEquals("custom:atlas/sprite", Flags.MONSTER_SPAWN.getIcon());

            yaml.set(iconKey, "not-a-sprite");
            FlagConfiguration.reconcileFlagDefinitions(yaml, false);
            assertEquals("not-a-sprite", yaml.getString(iconKey));
            assertEquals("minecraft:items/item/zombie_spawn_egg", Flags.MONSTER_SPAWN.getIcon());
        } finally {
            Flags.MONSTER_SPAWN.setMaterial(state.material());
            Flags.MONSTER_SPAWN.setIcon(state.icon());
        }
    }

    @Test
    void preservesConfiguredLegacyLookingIconWithoutMigration() {
        String iconKey = Flags.MONSTER_SPAWN.getConfigurationDialogUiIconKey();
        String oldIcon = Flags.MONSTER_SPAWN.getIcon();
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.set(iconKey, "minecraft:items/zombie_spawn_egg");

            FlagConfiguration.reconcileFlagDefinitions(yaml, false);

            assertEquals("minecraft:items/zombie_spawn_egg", yaml.getString(iconKey));
            assertEquals("minecraft:items/zombie_spawn_egg", Flags.MONSTER_SPAWN.getIcon());
        } finally {
            Flags.MONSTER_SPAWN.setIcon(oldIcon);
        }
    }

    @Test
    void emptyFlagDialogUiIconRemainsWithoutAnIcon() {
        String iconKey = Flags.MONSTER_SPAWN.getConfigurationDialogUiIconKey();
        String oldIcon = Flags.MONSTER_SPAWN.getIcon();
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.set(iconKey, "");

            FlagConfiguration.reconcileFlagDefinitions(yaml, false);

            assertNull(Flags.MONSTER_SPAWN.getIcon());
            assertEquals("", yaml.getString(iconKey));
        } finally {
            Flags.MONSTER_SPAWN.setIcon(oldIcon);
        }
    }

    @Test
    void missingGroupDialogUiIconUsesDeclaredDefaultAndValidOverrideWins() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("groups.environment.custom.material", "DIAMOND");
        yaml.set("groups.environment.custom.flags", java.util.List.of("monster_spawn"));

        invokeLoadConfiguredFlagGroups(yaml);
        try {
            EnvFlagGroup group = FlagGroups.getEnvFlagGroup("custom");
            assertNull(group.getIcon());

            yaml.set("groups.environment.custom.dialog-ui-icon", "custom:atlas/group");
            invokeLoadConfiguredFlagGroups(yaml);
            assertEquals("custom:atlas/group", FlagGroups.getEnvFlagGroup("custom").getIcon());
        } finally {
            FlagGroups.replaceConfiguredGroups(
                    FlagGroups.defaultEnvironmentGroups(),
                    FlagGroups.defaultPrivilegeGroups()
            );
        }
    }

    @Test
    void emptyGroupDialogUiIconRemainsWithoutAnIcon() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("groups.environment.custom.material", "DIAMOND");
        yaml.set("groups.environment.custom.dialog-ui-icon", "");
        yaml.set("groups.environment.custom.flags", java.util.List.of("monster_spawn"));

        invokeLoadConfiguredFlagGroups(yaml);
        try {
            assertNull(FlagGroups.getEnvFlagGroup("custom").getIcon());
        } finally {
            FlagGroups.replaceConfiguredGroups(
                    FlagGroups.defaultEnvironmentGroups(),
                    FlagGroups.defaultPrivilegeGroups()
            );
        }
    }

    @Test
    void preservesConfiguredGroupIconWithoutMigration() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("groups.environment.explosions.material", Material.TNT.name());
        yaml.set("groups.environment.explosions.dialog-ui-icon", "minecraft:items/item/tnt");
        yaml.set("groups.environment.explosions.flags", java.util.List.of("tnt_explode"));

        invokeLoadConfiguredFlagGroups(yaml);
        try {
            assertEquals("minecraft:items/item/tnt",
                    FlagGroups.getEnvFlagGroup("explosions").getIcon());
        } finally {
            FlagGroups.replaceConfiguredGroups(
                    FlagGroups.defaultEnvironmentGroups(),
                    FlagGroups.defaultPrivilegeGroups()
            );
        }
    }

    private void invokeLoadConfiguredFlagGroups(YamlConfiguration yaml) throws Exception {
        var method = FlagConfiguration.class.getDeclaredMethod("loadConfiguredFlagGroups", YamlConfiguration.class);
        method.setAccessible(true);
        method.invoke(null, yaml);
    }

    private record MaterialState(String material, String icon) {
    }
}
