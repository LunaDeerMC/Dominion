package cn.lunadeer.dominion.handler;

import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.Flag;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.events.FlagRegisterEvent;
import cn.lunadeer.dominion.utils.XLogger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class FlagRegisterHandlerTest {
    private FlagRegisterHandler listener;
    private PluginManager manager;

    @BeforeEach
    void setUp() {
        JavaPlugin dominion = mock(JavaPlugin.class);
        Server server = mock(Server.class);
        manager = mock(PluginManager.class);
        when(dominion.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(manager);
        listener = new FlagRegisterHandler(dominion);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void logsUnknownSourceWithoutChangingTheFlagOrCancellation(boolean cancelled) {
        PriFlag flag = privilege("handler_null_source");
        FlagRegisterEvent event = new FlagRegisterEvent(null, flag);
        event.setCancelled(cancelled);

        try (MockedStatic<XLogger> logger = mockStatic(XLogger.class)) {
            assertDoesNotThrow(() -> listener.onFlagRegister(event));

            logger.verify(() -> XLogger.debug(
                    "External flag registered: Custom (handler_null_source) - From: unknown"));
        }
        assertSame(flag, event.getFlag());
        assertEquals(cancelled, event.isCancelled());
    }

    @Test
    void retainsTheRealPluginNameWhenPresent() {
        JavaPlugin slimefun = mock(JavaPlugin.class);
        when(slimefun.getName()).thenReturn("Slimefun");
        PriFlag flag = privilege("handler_named_source");
        FlagRegisterEvent event = new FlagRegisterEvent(slimefun, flag);

        try (MockedStatic<XLogger> logger = mockStatic(XLogger.class)) {
            listener.onFlagRegister(event);

            logger.verify(() -> XLogger.debug(
                    "External flag registered: Custom (handler_named_source) - From: Slimefun"));
        }
        assertSame(flag, event.getFlag());
        assertFalse(event.isCancelled());
    }

    @Test
    @SuppressWarnings("unchecked")
    void nullSourcePrivilegeAndEnvironmentFlagsRegisterThroughTheRealListener() throws ReflectiveOperationException {
        Field registryField = Flags.class.getDeclaredField("registered_flags");
        registryField.setAccessible(true);
        Map<String, Flag> registry = (Map<String, Flag>) registryField.get(null);
        Field revisionField = Flags.class.getDeclaredField("revision");
        revisionField.setAccessible(true);
        AtomicLong revision = (AtomicLong) revisionField.get(null);
        Map<String, Flag> originalRegistry;
        long originalRevision;
        synchronized (Flags.class) {
            originalRegistry = new LinkedHashMap<>(registry);
            originalRevision = revision.get();
        }

        PriFlag privilege = privilege("handler_registered_privilege");
        EnvFlag environment = new EnvFlag("handler_registered_environment", "Custom", "Custom",
                false, true, Material.PAPER);
        doAnswer(invocation -> {
            listener.onFlagRegister(invocation.getArgument(0));
            return null;
        }).when(manager).callEvent(any(FlagRegisterEvent.class));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<XLogger> logger = mockStatic(XLogger.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(manager);

            assertTrue(Flags.registerPriFlag(null, privilege));
            assertTrue(Flags.registerEnvFlag(null, environment));

            assertSame(privilege, Flags.getActivePriFlag(privilege.getFlagName()));
            assertSame(environment, Flags.getActiveEnvFlag(environment.getFlagName()));
            assertEquals(originalRevision + 2, Flags.getRevision());
            logger.verify(() -> XLogger.debug(
                    "External flag registered: Custom (handler_registered_privilege) - From: unknown"));
            logger.verify(() -> XLogger.debug(
                    "External flag registered: Custom (handler_registered_environment) - From: unknown"));
        } finally {
            synchronized (Flags.class) {
                registry.clear();
                registry.putAll(originalRegistry);
                revision.set(originalRevision);
            }
        }
    }

    private static PriFlag privilege(String name) {
        return new PriFlag(name, "Custom", "Custom", false, true, Material.PAPER);
    }
}
