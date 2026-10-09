package cn.lunadeer.dominion.api.dtos.flag;

import cn.lunadeer.dominion.misc.Converts;
import cn.lunadeer.dominion.misc.DominionException;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LegacyFlagRegistryTest {
    @Test
    void oldNamesRemainReadableButCannotBeUsedAsCommandArguments() {
        assertSame(Flags.CONTAINER, Flags.getPreFlag("container"));
        assertSame(Flags.BURN, Flags.getEnvFlag("burn"));
        assertTrue(Flags.getAllPriFlags().contains(Flags.CONTAINER));
        assertFalse(Flags.getActivePriFlags().contains(Flags.CONTAINER));
        assertThrows(DominionException.class, () -> Converts.toPriFlag("container"));
        assertThrows(DominionException.class, () -> Converts.toEnvFlag("burn"));
        assertSame(Flags.CHEST, assertDoesNotThrow(() -> Converts.toPriFlag("chest")));
    }

    @Test
    void everyAliasResolvesToAnActiveFlagOfTheSameType() {
        Flags.getLegacyAliases().forEach((legacy, target) -> {
            assertSame(target, Flags.resolveReadFlag(legacy));
            assertSame(target, Flags.resolveReadFlag(target));
            assertEquals(legacy.getClass(), target.getClass());
            assertTrue(Flags.getActiveFlags().contains(target));
            assertFalse(Flags.getActiveFlags().contains(legacy));
            assertSame(legacy, Flags.getFlag(legacy.getFlagName()));
            assertNull(Flags.getActiveFlag(legacy.getFlagName()));
        });
    }

    @Test
    void oldMetadataFollowsCurrentTargetWithoutChangingMigrationDefaults() {
        boolean oldDefault = Flags.CHEST.getDefaultValue();
        boolean oldEnable = Flags.CHEST.getEnable();
        boolean legacyDefault = Flags.CONTAINER.getMigrationDefaultValue();
        boolean legacyEnable = Flags.CONTAINER.getMigrationEnable();
        try {
            Flags.CHEST.setDefaultValue(!legacyDefault);
            Flags.CHEST.setEnable(!legacyEnable);
            assertEquals(!legacyDefault, Flags.CONTAINER.getDefaultValue());
            assertEquals(!legacyEnable, Flags.CONTAINER.getEnable());
            assertEquals(legacyDefault, Flags.CONTAINER.getMigrationDefaultValue());
            assertEquals(legacyEnable, Flags.CONTAINER.getMigrationEnable());
            assertEquals(Flags.getAllPriFlagsEnable().contains(Flags.CHEST),
                    Flags.getAllPriFlagsEnable().contains(Flags.CONTAINER));
        } finally {
            Flags.CHEST.setDefaultValue(oldDefault);
            Flags.CHEST.setEnable(oldEnable);
        }
    }

    @Test
    void customRegistrationCannotReintroduceALegacyName() {
        PriFlag impostor = new PriFlag("container", "Custom", "Custom", false, true, Material.CHEST);
        assertFalse(Flags.registerPriFlag(null, impostor));
        assertSame(Flags.CONTAINER, Flags.getPreFlag("container"));
    }
}
