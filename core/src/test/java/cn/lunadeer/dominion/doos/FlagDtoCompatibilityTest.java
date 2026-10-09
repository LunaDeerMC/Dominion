package cn.lunadeer.dominion.doos;

import cn.lunadeer.dominion.api.dtos.CuboidDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.storage.repository.TemplateRepository;
import cn.lunadeer.dominion.flags.FlagValues;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

@SuppressWarnings("deprecation")
class FlagDtoCompatibilityTest {
    @Test
    void dominionMapsAndGettersTrackCurrentTargetValues() {
        DominionDOO dominion = dominion();
        var guestFlags = dominion.getGuestPrivilegeFlagValue();
        var environmentFlags = dominion.getEnvironmentFlagValue();
        assertSame(guestFlags, dominion.getGuestPrivilegeFlagValue());
        assertSame(environmentFlags, dominion.getEnvironmentFlagValue());

        for (boolean allowed : new boolean[]{true, false}) {
            guestFlags.put(Flags.CHEST, allowed);
            environmentFlags.put(Flags.BURN_BLOCK, allowed);

            assertEquals(allowed, dominion.getGuestFlagValue(Flags.CONTAINER));
            assertEquals(allowed, guestFlags.get(Flags.CONTAINER));
            assertEquals(allowed, dominion.getEnvFlagValue(Flags.BURN));
            assertEquals(allowed, environmentFlags.get(Flags.BURN));
        }
    }

    @Test
    void memberAndGroupMapsAndGettersTrackCurrentTargetValues() throws Exception {
        MemberDOO member = new MemberDOO(UUID.randomUUID(), dominion());
        GroupDOO group = group(Map.of(Flags.CHEST, false));
        var memberFlags = member.getFlagsValue();
        var groupFlags = group.getFlagsValue();
        assertSame(memberFlags, member.getFlagsValue());
        assertSame(groupFlags, group.getFlagsValue());

        for (boolean allowed : new boolean[]{true, false}) {
            memberFlags.put(Flags.CHEST, allowed);
            groupFlags.put(Flags.CHEST, allowed);

            assertEquals(allowed, member.getFlagValue(Flags.CONTAINER));
            assertEquals(allowed, memberFlags.get(Flags.CONTAINER));
            assertEquals(allowed, group.getFlagValue(Flags.CONTAINER));
            assertEquals(allowed, groupFlags.get(Flags.CONTAINER));
        }
    }

    @Test
    void templateMapRemainsAnImmutableSnapshotWithLegacyKeys() throws Exception {
        TemplateDOO template = template(Map.of(Flags.CHEST, false));
        var snapshot = template.getFlagsValue();
        assertEquals(false, snapshot.get(Flags.CONTAINER));

        try (MockedStatic<TemplateRepository> repository = mockStatic(TemplateRepository.class)) {
            template.setFlagValue(Flags.CHEST, true);
            repository.verify(() -> TemplateRepository.updateFlag(1, Flags.CHEST, true));
        }

        assertTrue(template.getFlagValue(Flags.CONTAINER));
        assertEquals(true, template.getFlagsValue().get(Flags.CONTAINER));
        assertEquals(false, snapshot.get(Flags.CONTAINER));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.put(Flags.CHEST, true));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.put(Flags.CONTAINER, true));
    }

    @Test
    void defaultFallbackUsesTheTargetFlag() throws Exception {
        DominionDOO emptyDominion = DominionDOO.rootDominion();
        MemberDOO member = new MemberDOO(UUID.randomUUID(), emptyDominion);
        GroupDOO group = group(Map.of());
        TemplateDOO template = template(Map.of());

        assertFalse(emptyDominion.getGuestFlagValue(Flags.CONTAINER));
        assertFalse(emptyDominion.getEnvFlagValue(Flags.BURN));
        assertEquals(Flags.CHEST.getDefaultValue(), member.getFlagValue(Flags.CONTAINER));
        assertEquals(Flags.CHEST.getDefaultValue(), group.getFlagValue(Flags.CONTAINER));
        assertEquals(Flags.CHEST.getDefaultValue(), template.getFlagValue(Flags.CONTAINER));
    }

    @Test
    void legacySettersRejectWritesBeforeChangingValuesOrAccessingStorage() throws Exception {
        DominionDOO dominion = dominion();
        dominion.getGuestPrivilegeFlagValue().put(Flags.CHEST, false);
        dominion.getEnvironmentFlagValue().put(Flags.BURN_BLOCK, false);
        MemberDOO member = new MemberDOO(UUID.randomUUID(), dominion);
        GroupDOO group = group(Map.of(Flags.CHEST, false));
        TemplateDOO template = template(Map.of(Flags.CHEST, false));

        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> dominion.setGuestFlagValue(Flags.CONTAINER, true)).getMessage().contains("chest"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> dominion.setEnvFlagValue(Flags.BURN, true)).getMessage().contains("burn_block"));
        assertThrows(IllegalArgumentException.class, () -> member.setFlagValue(Flags.CONTAINER, true));
        assertThrows(IllegalArgumentException.class, () -> group.setFlagValue(Flags.CONTAINER, true));
        assertThrows(IllegalArgumentException.class, () -> template.setFlagValue(Flags.CONTAINER, true));

        assertFalse(dominion.getGuestFlagValue(Flags.CHEST));
        assertFalse(dominion.getEnvFlagValue(Flags.BURN_BLOCK));
        assertFalse(member.getFlagValue(Flags.CHEST));
        assertFalse(group.getFlagValue(Flags.CHEST));
        assertFalse(template.getFlagValue(Flags.CHEST));
    }

    @Test
    void copiesDoNotKeepIndependentLegacyValuesForPersistence() throws Exception {
        DominionDOO dominion = dominion();
        dominion.getGuestPrivilegeFlagValue().put(Flags.CHEST, false);
        assertTrue(dominion.getGuestPrivilegeFlagValue().containsKey(Flags.CONTAINER));

        MemberDOO member = new MemberDOO(UUID.randomUUID(), dominion);
        GroupDOO group = group(Map.of(Flags.CHEST, false, Flags.CONTAINER, true));
        TemplateDOO template = template(Map.of(Flags.CHEST, false, Flags.CONTAINER, true));

        for (Object dto : new Object[]{member, group, template}) {
            Field field = dto.getClass().getDeclaredField("flags");
            field.setAccessible(true);
            Map<?, ?> storedFlags = ((FlagValues<?>) field.get(dto)).activeValues();
            assertFalse(storedFlags.containsKey(Flags.CONTAINER));
            assertEquals(false, storedFlags.get(Flags.CHEST));
        }
        assertFalse(member.getFlagValue(Flags.CONTAINER));
        assertFalse(group.getFlagValue(Flags.CONTAINER));
        assertFalse(template.getFlagValue(Flags.CONTAINER));
    }

    private static DominionDOO dominion() {
        return new DominionDOO(UUID.randomUUID(), "test", UUID.randomUUID(), CuboidDTO.ZERO, -1);
    }

    private static GroupDOO group(Map<PriFlag, Boolean> flags) throws Exception {
        Constructor<GroupDOO> constructor = GroupDOO.class.getDeclaredConstructor(
                Integer.class, Integer.class, String.class, Map.class, String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(1, 1, "test", flags, "test");
    }

    private static TemplateDOO template(Map<PriFlag, Boolean> flags) throws Exception {
        Constructor<TemplateDOO> constructor = TemplateDOO.class.getDeclaredConstructor(
                Integer.class, UUID.class, String.class, Map.class);
        constructor.setAccessible(true);
        return constructor.newInstance(1, UUID.randomUUID(), "test", flags);
    }
}
