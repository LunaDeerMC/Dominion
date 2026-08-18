package cn.lunadeer.dominion.api.dtos.flag;

import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
                Flags.COPPER_CHEST,
                Flags.SHELF,
                Flags.BOOKSHELF
        ), storage.getFlags());
        assertFalse(storage.containsFlag(Flags.CONTAINER));
        PriFlagGroup workstations = privilegeGroup("workstations");
        PriFlagGroup decoration = FlagGroups.defaultPrivilegeGroups().stream()
                .filter(group -> group.getId().equals("decoration"))
                .findFirst()
                .orElseThrow();
        PriFlagGroup building = FlagGroups.defaultPrivilegeGroups().stream()
                .filter(group -> group.getId().equals("building"))
                .findFirst()
                .orElseThrow();
        assertTrue(decoration.containsFlag(Flags.FLOWER_POT));
        assertTrue(building.containsFlag(Flags.PLACE_FLOWER_POT_CONTENT));
        assertTrue(decoration.containsFlag(Flags.PLACE_FLOWER_POT_CONTENT));
        assertTrue(building.containsFlag(Flags.ARMOR_STAND_PROJECTILE_BREAK));
        assertTrue(decoration.containsFlag(Flags.ARMOR_STAND_PROJECTILE_BREAK));
        assertTrue(workstations.containsFlag(Flags.LECTERN));
        assertTrue(decoration.containsFlag(Flags.SHELF));
        assertFalse(Flags.getAllPriFlags().contains(Flags.CONTAINER));
    }

    @Test
    void defaultEnvironmentGroupsExposeIntentionalDiscoveryOverlaps() {
        assertEquals(List.of(
                Flags.ENDER_MAN_PICKUP_BLOCK,
                Flags.ENDER_MAN_PLACE_BLOCK,
                Flags.WITHER_BREAK_BLOCK,
                Flags.DRAGON_BREAK_BLOCK,
                Flags.MOB_TRAMPLE
        ), environmentGroup("mob-griefing").getFlags());
        assertEquals(List.of(Flags.FLOW_IN_WATER, Flags.FLOW_IN_LAVA),
                environmentGroup("fluid-flow").getFlags());
        assertEquals(List.of(
                Flags.ICE_MELT,
                Flags.ICE_FORM_NATURAL,
                Flags.ICE_FORM_FROST_WALKER,
                Flags.SNOW_ACCUMULATION,
                Flags.SNOW_MELT
        ), environmentGroup("ice-and-snow").getFlags());
        assertEquals(List.of(Flags.TRAMPLE, Flags.MOB_TRAMPLE),
                environmentGroup("farmland").getFlags());
        assertEquals(List.of(
                Flags.TNT_EXPLODE,
                Flags.CREEPER_EXPLODE,
                Flags.WITHER_EXPLODE,
                Flags.WITHER_SKULL_EXPLODE,
                Flags.ENDER_CRYSTAL_EXPLODE,
                Flags.FIREBALL_EXPLODE,
                Flags.BED_EXPLODE,
                Flags.ANCHOR_EXPLODE
        ), environmentGroup("explosion-block-damage").getFlags());

        List<EnvFlag> explosionEntityFlags = List.of(
                Flags.TNT_DAMAGE_ENTITY,
                Flags.CREEPER_DAMAGE_ENTITY,
                Flags.WITHER_SKULL_DAMAGE_ENTITY,
                Flags.ENDER_CRYSTAL_DAMAGE_ENTITY,
                Flags.FIREBALL_DAMAGE_ENTITY,
                Flags.TNT_DAMAGE_ARMOR_STAND,
                Flags.TNT_DAMAGE_HANGING_ENTITY,
                Flags.CREEPER_DAMAGE_ARMOR_STAND,
                Flags.CREEPER_DAMAGE_HANGING_ENTITY,
                Flags.WITHER_SKULL_DAMAGE_ARMOR_STAND,
                Flags.WITHER_SKULL_DAMAGE_HANGING_ENTITY,
                Flags.ENDER_CRYSTAL_DAMAGE_ARMOR_STAND,
                Flags.ENDER_CRYSTAL_DAMAGE_HANGING_ENTITY,
                Flags.FIREBALL_DAMAGE_ARMOR_STAND,
                Flags.FIREBALL_DAMAGE_HANGING_ENTITY
        );
        assertEquals(explosionEntityFlags, environmentGroup("explosion-entity-damage").getFlags());
        EnvFlagGroup explosions = environmentGroup("explosions");
        EnvFlagGroup entityProtection = environmentGroup("entity-protection");
        explosionEntityFlags.forEach(flag -> assertTrue(explosions.containsFlag(flag)));
        explosionEntityFlags.subList(5, explosionEntityFlags.size())
                .forEach(flag -> assertTrue(entityProtection.containsFlag(flag)));
        assertTrue(environmentGroup("creature-behavior").containsFlag(Flags.MOB_TRAMPLE));
        assertTrue(environmentGroup("natural-changes").containsFlag(Flags.MOB_TRAMPLE));
        assertTrue(environmentGroup("farmland").containsFlag(Flags.MOB_TRAMPLE));
    }

    @Test
    void defaultGroupsCoverActiveFlagsAndExposeGranularSplits() {
        Set<EnvFlag> environmentFlags = new LinkedHashSet<>();
        FlagGroups.defaultEnvironmentGroups().forEach(group -> environmentFlags.addAll(group.getFlags()));
        Set<PriFlag> privilegeFlags = new LinkedHashSet<>();
        FlagGroups.defaultPrivilegeGroups().forEach(group -> privilegeFlags.addAll(group.getFlags()));

        assertEquals(Set.copyOf(Flags.getAllEnvFlags()), environmentFlags);
        assertEquals(Set.copyOf(Flags.getAllPriFlags()), privilegeFlags);
        assertFalse(environmentFlags.contains(Flags.ENDER_MAN));
        assertFalse(environmentFlags.contains(Flags.BLOCK_EXPLODE));
        assertFalse(privilegeFlags.contains(Flags.SHOOT));
        assertTrue(FlagGroups.defaultPrivilegeGroups().stream()
                .filter(group -> group.getId().equals("food"))
                .findFirst().orElseThrow().containsFlag(Flags.CAKE));
        assertTrue(FlagGroups.defaultPrivilegeGroups().stream()
                .filter(group -> group.getId().equals("trading"))
                .findFirst().orElseThrow().containsFlag(Flags.TRADE));
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

    private static EnvFlagGroup environmentGroup(String id) {
        return FlagGroups.defaultEnvironmentGroups().stream()
                .filter(group -> group.getId().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static PriFlagGroup privilegeGroup(String id) {
        return FlagGroups.defaultPrivilegeGroups().stream()
                .filter(group -> group.getId().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
