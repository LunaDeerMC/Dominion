package cn.lunadeer.dominion.api.dtos.flag;

import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlagGroupsTest {

    @AfterEach
    void restoreDefaults() {
        FlagGroups.replaceConfiguredGroups(
                FlagGroups.defaultEnvironmentGroups(),
                FlagGroups.defaultPrivilegeGroups()
        );
    }

    @Test
    void preservesFlagOrderAndDeduplicatesMembership() {
        EnvFlagGroup group = new EnvFlagGroup(
                "ordered",
                "Ordered",
                "",
                Material.PAPER,
                List.of(Flags.MONSTER_SPAWN, Flags.ANIMAL_SPAWN, Flags.MONSTER_SPAWN)
        );

        assertEquals(List.of(Flags.MONSTER_SPAWN, Flags.ANIMAL_SPAWN), group.getFlags());
        assertFalse(group.addFlag(Flags.ANIMAL_SPAWN));
        assertThrows(UnsupportedOperationException.class, () -> group.getFlags().add(Flags.ANIMAL_MOVE));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void rejectsWrongFlagTypeAtRuntimeForRawApiCallers() {
        FlagGroup raw = new PriFlagGroup("privilege", "Privilege", "", Material.PAPER);
        assertThrows(IllegalArgumentException.class, () -> raw.addFlag(Flags.ANIMAL_MOVE));
    }

    @Test
    void supportsOverlappingGroupsAndBuildsUngroupedLastSet() {
        EnvFlagGroup first = new EnvFlagGroup(
                "first", "First", "", Material.PAPER, List.of(Flags.ANIMAL_SPAWN, Flags.ANIMAL_MOVE));
        EnvFlagGroup second = new EnvFlagGroup(
                "second", "Second", "", Material.PAPER, List.of(Flags.ANIMAL_MOVE));
        FlagGroups.replaceConfiguredGroups(List.of(first, second), List.of());

        assertEquals(List.of(first, second), FlagGroups.getEnvFlagGroups());
        assertTrue(first.containsFlag(Flags.ANIMAL_MOVE));
        assertTrue(second.containsFlag(Flags.ANIMAL_MOVE));
        assertFalse(FlagGroups.getUngroupedEnvFlags().containsFlag(Flags.ANIMAL_SPAWN));
        assertFalse(FlagGroups.getUngroupedEnvFlags().containsFlag(Flags.ANIMAL_MOVE));
        assertTrue(FlagGroups.getUngroupedEnvFlags().containsFlag(Flags.MONSTER_SPAWN));
    }

    @Test
    void groupMutationAdvancesRegistryRevision() {
        EnvFlagGroup group = new EnvFlagGroup("mutable", "Mutable", "", Material.PAPER);
        FlagGroups.replaceConfiguredGroups(List.of(group), List.of());
        long before = FlagGroups.getRevision();

        group.addFlag(Flags.ANIMAL_SPAWN);
        group.setDisplayName("Changed");

        assertEquals(before + 2, FlagGroups.getRevision());
    }

    @Test
    void environmentAndPrivilegeGroupsUseSeparateLanguageNamespaces() {
        EnvFlagGroup environment = new EnvFlagGroup("shared", "Environment", "", Material.PAPER);
        PriFlagGroup privilege = new PriFlagGroup("shared", "Privilege", "", Material.PAPER);

        assertEquals("flag-groups.environment.shared.display-name", environment.getDisplayNameKey());
        assertEquals("flag-groups.environment.shared.description", environment.getDescriptionKey());
        assertEquals("flag-groups.privilege.shared.display-name", privilege.getDisplayNameKey());
        assertEquals("flag-groups.privilege.shared.description", privilege.getDescriptionKey());
    }

    @Test
    void storageGroupContainsOneFlagPerSupportedContainerType() {
        PriFlagGroup storage = FlagGroups.defaultPrivilegeGroups().stream()
                .filter(group -> group.getId().equals("storage"))
                .findFirst()
                .orElseThrow();

        assertEquals(List.of(
                Flags.CHEST,
                Flags.BARREL,
                Flags.SHULKER_BOX,
                Flags.HOPPER,
                Flags.DROPPER,
                Flags.DISPENSER,
                Flags.FURNACE,
                Flags.BLAST_FURNACE,
                Flags.SMOKER,
                Flags.FLOWER_POT,
                Flags.COPPER_CHEST,
                Flags.SHELF,
                Flags.ITEM_FRAME_CONTENT
        ), storage.getFlags());
        assertFalse(storage.containsFlag(Flags.CONTAINER));
        assertFalse(Flags.getAllPriFlags().contains(Flags.CONTAINER));
    }

    @Test
    void oldAndNewConstructorsExposeDialogUiIconsWithoutChangingMaterials() {
        EnvFlag legacyFlag = new EnvFlag("legacy", "Legacy", "", false, true, Material.PAPER);
        EnvFlag explicitFlag = new EnvFlag("explicit", "Explicit", "", false, true,
                Material.PAPER, "custom:atlas/sprite");
        assertNull(legacyFlag.getIcon());
        assertEquals("custom:atlas/sprite", explicitFlag.getIcon());

        EnvFlagGroup legacyGroup = new EnvFlagGroup("legacy", "Legacy", "", Material.PAPER);
        EnvFlagGroup explicitGroup = new EnvFlagGroup("explicit", "Explicit", "", Material.PAPER,
                "custom:atlas/group", List.of());
        assertNull(legacyGroup.getIcon());
        assertEquals("custom:atlas/group", explicitGroup.getIcon());
        assertEquals("groups.environment.explicit.dialog-ui-icon",
                explicitGroup.getConfigurationDialogUiIconKey());
        explicitGroup.setIcon(null);
        assertNull(explicitGroup.getIcon());
        explicitGroup.setIcon("");
        assertNull(explicitGroup.getIcon());

        explicitFlag.setIcon(null);
        assertNull(explicitFlag.getIcon());
        explicitFlag.setIcon("");
        assertNull(explicitFlag.getIcon());
        explicitFlag.setMaterial("DIAMOND");
        assertNull(explicitFlag.getIcon());
        assertEquals(Material.DIAMOND, explicitFlag.getMaterial());
    }
}
