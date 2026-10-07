package cn.lunadeer.dominion.configuration;

import cn.lunadeer.dominion.api.dtos.flag.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CushionGroupMigrationTest {
    @Test void freshDefaultsIncludeExistingAndDedicatedGroups() {
        for (String id : List.of("building", "decoration", "cushion")) {
            var group = FlagGroups.defaultPrivilegeGroups().stream().filter(g -> g.getId().equals(id)).findFirst().orElseThrow();
            assertTrue(group.containsFlag(Flags.CUSHION_PLACE));
            assertTrue(group.containsFlag(Flags.CUSHION_BREAK));
        }
        for (String id : List.of("entity-protection", "cushion")) {
            var group = FlagGroups.defaultEnvironmentGroups().stream().filter(g -> g.getId().equals(id)).findFirst().orElseThrow();
            assertTrue(group.containsFlag(Flags.CUSHION_MOB_DAMAGE));
            assertTrue(group.containsFlag(Flags.CUSHION_ENVIRONMENT_BREAK));
        }
    }

    @Test void upgradeAppendsWithoutResettingCustomizedGroupsOrExistingCushionGroup() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("groups.privilege.building.flags", List.of("place", "addon_custom"));
        yaml.set("groups.privilege.building.material", "DIAMOND");
        yaml.set("groups.environment.entity-protection.flags", List.of("armor_stand_mob_damage"));
        yaml.set("groups.privilege.cushion.flags", List.of("riding", "cushion_break"));
        yaml.set("groups.privilege.cushion.dialog-ui-icon", "");
        FlagConfiguration.migrateCushionGroups(yaml, 5);
        assertEquals(List.of("place", "addon_custom", "cushion_place", "cushion_break"),
                yaml.getStringList("groups.privilege.building.flags"));
        assertEquals("DIAMOND", yaml.getString("groups.privilege.building.material"));
        assertEquals(List.of("riding", "cushion_break", "cushion_place"), yaml.getStringList("groups.privilege.cushion.flags"));
        assertEquals("", yaml.getString("groups.privilege.cushion.dialog-ui-icon"));
        assertEquals(List.of("cushion_mob_damage", "cushion_environment_break"), yaml.getStringList("groups.environment.cushion.flags"));
        assertEquals(List.of("armor_stand_mob_damage", "cushion_mob_damage", "cushion_environment_break"),
                yaml.getStringList("groups.environment.entity-protection.flags"));
        assertFalse(yaml.contains("groups.privilege.decoration"), "Do not recreate a group the administrator deleted");
        String once = yaml.saveToString();
        FlagConfiguration.migrateCushionGroups(yaml, 5);
        assertEquals(once, yaml.saveToString());
    }

    @Test void reloadDoesNotUndoAnAdministratorsPostUpgradeEdits() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("groups.privilege.cushion.flags", List.of("cushion_break"));
        String before = yaml.saveToString();
        FlagConfiguration.migrateCushionGroups(yaml, 6);
        assertEquals(before, yaml.saveToString());
    }

    @Test void newFlagsHaveDistinctNamesAndNoLegacyInheritance() {
        for (Flag flag : List.of(Flags.CUSHION_PLACE, Flags.CUSHION_BREAK,
                Flags.CUSHION_MOB_DAMAGE, Flags.CUSHION_ENVIRONMENT_BREAK)) {
            assertTrue(flag.getFlagName().startsWith("cushion_"));
            assertNull(Flags.getLegacySource(flag));
        }
        assertNull(Flags.getPreFlag("place_cushion"));
        assertNull(Flags.getPreFlag("cushion_projectile_break"));
    }
}
