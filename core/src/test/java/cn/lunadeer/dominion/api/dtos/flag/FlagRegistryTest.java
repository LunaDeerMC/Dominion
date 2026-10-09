package cn.lunadeer.dominion.api.dtos.flag;

import cn.lunadeer.dominion.events.FlagRegisterEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FlagRegistryTest {
    private Map<String, Flag> registry;
    private Map<String, Flag> originalRegistry;
    private AtomicLong revision;
    private long originalRevision;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void saveRegistry() throws ReflectiveOperationException {
        Field flagsField = Flags.class.getDeclaredField("registered_flags");
        flagsField.setAccessible(true);
        registry = (Map<String, Flag>) flagsField.get(null);
        originalRegistry = new LinkedHashMap<>(registry);
        Field revisionField = Flags.class.getDeclaredField("revision");
        revisionField.setAccessible(true);
        revision = (AtomicLong) revisionField.get(null);
        originalRevision = revision.get();
    }

    @AfterEach
    void restoreRegistry() {
        synchronized (Flags.class) {
            registry.clear();
            registry.putAll(originalRegistry);
            revision.set(originalRevision);
        }
    }

    @Test
    void returnedListsCannotDesynchronizeRegistryLookups() {
        List<List<? extends Flag>> snapshots = List.of(
                Flags.getAllFlags(), Flags.getAllEnvFlags(), Flags.getAllPriFlags(),
                Flags.getActiveFlags(), Flags.getActiveEnvFlags(), Flags.getActivePriFlags(),
                Flags.getAllFlagsEnable(), Flags.getAllEnvFlagsEnable(), Flags.getAllPriFlagsEnable(),
                Flags.getActiveFlagsEnable(), Flags.getActiveEnvFlagsEnable(), Flags.getActivePriFlagsEnable());
        for (List<? extends Flag> snapshot : snapshots) {
            assertThrows(UnsupportedOperationException.class, snapshot::clear);
        }
        assertEquals(List.copyOf(originalRegistry.values()), Flags.getAllFlags());
        assertSame(Flags.CHEST, Flags.getActivePriFlag("chest"));
        assertSame(Flags.CONTAINER, Flags.getPreFlag("container"));
        assertNull(Flags.getActivePriFlag("container"));
    }

    @Test
    void registrationUsesEventReplacementAndAllSnapshotsShareOneRegistry() {
        PriFlag proposed = privilege("registry_proposed");
        PriFlag replacement = privilege("registry_replacement");
        EnvFlag environment = environment("registry_environment");
        PluginManager manager = mock(PluginManager.class);
        doAnswer(invocation -> {
            FlagRegisterEvent event = invocation.getArgument(0);
            if (event.getFlag() == proposed) event.setFlag(replacement);
            return null;
        }).when(manager).callEvent(any(FlagRegisterEvent.class));
        List<Flag> oldSnapshot = Flags.getAllFlags();

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
            assertTrue(Flags.registerPriFlag(null, proposed));
            assertTrue(Flags.registerEnvFlag(null, environment));
        }

        assertNull(Flags.getFlag(proposed.getFlagName()));
        assertSame(replacement, Flags.getPreFlag(replacement.getFlagName()));
        assertSame(replacement, Flags.getActivePriFlag(replacement.getFlagName()));
        assertSame(environment, Flags.getEnvFlag(environment.getFlagName()));
        assertSame(environment, Flags.getActiveEnvFlag(environment.getFlagName()));
        assertTrue(Flags.getAllPriFlags().contains(replacement));
        assertTrue(Flags.getActivePriFlagsEnable().contains(replacement));
        assertTrue(Flags.getAllEnvFlags().contains(environment));
        assertTrue(Flags.getActiveEnvFlagsEnable().contains(environment));
        assertFalse(oldSnapshot.contains(replacement));
        assertFalse(oldSnapshot.contains(environment));
        List<Flag> newSnapshot = Flags.getAllFlags();
        assertEquals(List.of(replacement, environment), newSnapshot.subList(oldSnapshot.size(), newSnapshot.size()));
        assertEquals(originalRevision + 2, Flags.getRevision());
        replacement.setEnable(false);
        assertFalse(Flags.getAllPriFlagsEnable().contains(replacement));
        assertFalse(Flags.getActivePriFlagsEnable().contains(replacement));
    }

    @Test
    void existingNamesAndInvalidProposalsAreRejectedBeforeEvents() {
        PluginManager manager = mock(PluginManager.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
            assertFalse(Flags.registerPriFlag(null, privilege("container")));
            assertFalse(Flags.registerEnvFlag(null, environment("container")));
            assertFalse(Flags.registerPriFlag(null, privilege("chest")));
            assertFalse(Flags.registerEnvFlag(null, environment("chest")));
            assertFalse(Flags.registerPriFlag(null, privilege(" ")));
            assertFalse(Flags.registerPriFlag(null, null));
        }
        verifyNoInteractions(manager);
        assertEquals(originalRevision, Flags.getRevision());
        assertEquals(List.copyOf(originalRegistry.values()), Flags.getAllFlags());
    }

    @Test
    void finalEventFlagIsValidatedBeforeChangingRegistry() {
        PriFlag proposed = privilege("registry_invalid_replacement");
        AtomicReference<Flag> replacement = new AtomicReference<>();
        PluginManager manager = mock(PluginManager.class);
        doAnswer(invocation -> {
            FlagRegisterEvent event = invocation.getArgument(0);
            event.setFlag(replacement.get());
            return null;
        }).when(manager).callEvent(any(FlagRegisterEvent.class));
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
            for (Flag invalid : Arrays.asList(null, Flags.CONTAINER, Flags.CHEST,
                    privilege("container"), privilege(" "), environment("registry_wrong_type"))) {
                replacement.set(invalid);
                assertFalse(Flags.registerPriFlag(null, proposed));
            }
        }
        assertEquals(originalRevision, Flags.getRevision());
        assertEquals(List.copyOf(originalRegistry.values()), Flags.getAllFlags());
    }

    @Test
    void cancelledRegistrationDoesNotChangeRegistry() {
        PluginManager manager = mock(PluginManager.class);
        doAnswer(invocation -> {
            FlagRegisterEvent event = invocation.getArgument(0);
            event.setCancelled(true);
            return null;
        }).when(manager).callEvent(any(FlagRegisterEvent.class));
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
            assertFalse(Flags.registerPriFlag(null, privilege("registry_cancelled")));
        }
        assertEquals(originalRevision, Flags.getRevision());
        assertNull(Flags.getFlag("registry_cancelled"));
    }

    @Test
    void activeCustomMigrationMetadataHonorsOverriddenGetters() {
        PriFlag custom = new PriFlag("registry_custom_metadata", "Custom", "Custom", false, false, Material.PAPER) {
            @Override
            public Boolean getDefaultValue() {
                return true;
            }

            @Override
            public Boolean getEnable() {
                return true;
            }
        };
        assertTrue(custom.getMigrationDefaultValue());
        assertTrue(custom.getMigrationEnable());
    }

    @Test
    void customGettersAndEventsRunOutsideTheRegistryLock() {
        PriFlag custom = new PriFlag("registry_lock_order", "Custom", "Custom", false, true, Material.PAPER) {
            @Override
            public String getFlagName() {
                assertFalse(Thread.holdsLock(Flags.class), "Custom names may consult other registries");
                return super.getFlagName();
            }

            @Override
            public Boolean getEnable() {
                assertFalse(Thread.holdsLock(Flags.class), "Avoid Flags/FlagGroups lock inversion");
                FlagGroups.getPriFlagGroup("storage");
                return true;
            }
        };
        PluginManager manager = mock(PluginManager.class);
        doAnswer(invocation -> {
            assertFalse(Thread.holdsLock(Flags.class));
            return null;
        }).when(manager).callEvent(any(FlagRegisterEvent.class));
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
            assertTrue(Flags.registerPriFlag(null, custom));
        }
        assertTrue(Flags.getAllPriFlagsEnable().contains(custom));
        assertTrue(Flags.getActivePriFlagsEnable().contains(custom));
    }

    private static PriFlag privilege(String name) {
        return new PriFlag(name, "Custom", "Custom", false, true, Material.PAPER);
    }

    private static EnvFlag environment(String name) {
        return new EnvFlag(name, "Custom", "Custom", false, true, Material.PAPER);
    }
}
