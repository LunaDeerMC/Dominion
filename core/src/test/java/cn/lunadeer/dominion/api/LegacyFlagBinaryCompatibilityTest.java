package cn.lunadeer.dominion.api;

import cn.lunadeer.dominion.DominionInterface;
import cn.lunadeer.dominion.api.dtos.CuboidDTO;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.doos.DominionDOO;
import cn.lunadeer.dominion.storage.repository.DominionRepository;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class LegacyFlagBinaryCompatibilityTest {
    @Test
    void precompiledConsumerReadsCurrentValuesWithoutRecompilation() throws Exception {
        URL fixture = getClass().getResource("/compat/legacy-flags/legacy-flag-reader.jar");
        assertNotNull(fixture, "Keep the precompiled old-API consumer as a test resource");
        CacheManager previousCache = CacheManager.instance;
        DominionAPI previousApi = DominionAPI.getInstance();
        boolean previousChestEnabled = Flags.CHEST.getEnable();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{fixture}, getClass().getClassLoader());
             var repository = mockStatic(DominionRepository.class)) {
            CacheManager.instance = mock(CacheManager.class);
            Flags.CHEST.setEnable(true);
            new DominionInterface();
            DominionDOO dominion = new DominionDOO(UUID.randomUUID(), "legacy-consumer",
                    UUID.randomUUID(), CuboidDTO.ZERO, -1);
            Player player = mock(Player.class);
            when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            Location location = new Location(null, 0, 0, 0);
            when(CacheManager.instance.getDominion(location)).thenReturn(dominion);

            // Only this old consumer comes from the JAR; all API classes resolve to the current runtime.
            Class<?> readerClass = loader.loadClass("dominion.compat.legacy.LegacyFlagReader");
            assertSame(loader, readerClass.getClassLoader());
            assertSame(Flags.class, loader.loadClass(Flags.class.getName()));
            Object reader = readerClass.getConstructor(DominionDTO.class).newInstance(dominion);
            Method reads = readerClass.getMethod("assertReads", boolean.class);
            Method checks = readerClass.getMethod("assertChecks", Location.class, Player.class, boolean.class);

            // The old binary retains the very same Map across both changes to the replacement permission.
            for (boolean allowed : new boolean[]{false, true, false}) {
                dominion.setGuestFlagValue(Flags.CHEST, allowed);
                invoke(reads, reader, allowed);
                invoke(checks, reader, location, player, allowed);
            }
        } finally {
            Flags.CHEST.setEnable(previousChestEnabled);
            CacheManager.instance = previousCache;
            DominionAPI.instance = previousApi;
        }
    }

    private static void invoke(Method method, Object target, Object... arguments) throws Exception {
        try {
            method.invoke(target, arguments);
        } catch (InvocationTargetException exception) {
            if (exception.getCause() instanceof Error error) throw error;
            if (exception.getCause() instanceof Exception cause) throw cause;
            throw exception;
        }
    }
}
