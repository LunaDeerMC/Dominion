package cn.lunadeer.dominion.configuration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CrystalPlacementGroupMigrationTest {
    @ParameterizedTest
    @ValueSource(ints = {5, 6})
    void upgradeAppendsToBuildingWithoutResettingCustomEntriesOrIcons(int schemaVersion) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("groups.privilege.building.flags", List.of("place", "addon_custom"));
        yaml.set("groups.privilege.building.material", "DIAMOND");
        yaml.set("groups.privilege.building.dialog-ui-icon", "");
        yaml.set("groups.privilege.custom.flags", List.of("riding"));

        FlagConfiguration.migrateCrystalPlacementGroup(yaml, schemaVersion);

        assertEquals(List.of("place", "addon_custom", "place_ender_crystal"),
                yaml.getStringList("groups.privilege.building.flags"));
        assertEquals("DIAMOND", yaml.getString("groups.privilege.building.material"));
        assertEquals("", yaml.getString("groups.privilege.building.dialog-ui-icon"));
        assertEquals(List.of("riding"), yaml.getStringList("groups.privilege.custom.flags"));
        String once = yaml.saveToString();
        FlagConfiguration.migrateCrystalPlacementGroup(yaml, schemaVersion);
        assertEquals(once, yaml.saveToString());
    }

    @Test
    void doesNotRecreateDeletedGroups() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("groups.privilege.custom.flags", List.of("place"));
        String before = yaml.saveToString();

        FlagConfiguration.migrateCrystalPlacementGroup(yaml, 6);

        assertFalse(yaml.contains("groups.privilege.building"));
        assertEquals(before, yaml.saveToString());
    }

    @ParameterizedTest
    @ValueSource(ints = {7, 8})
    void reloadKeepsPostUpgradeGroupEdits(int schemaVersion) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("groups.privilege.building.flags", List.of("place"));
        String before = yaml.saveToString();

        FlagConfiguration.migrateCrystalPlacementGroup(yaml, schemaVersion);

        assertEquals(before, yaml.saveToString());
    }
}
