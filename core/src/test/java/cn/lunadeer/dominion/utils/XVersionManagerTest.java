package cn.lunadeer.dominion.utils;

import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class XVersionManagerTest {
    @Test
    void dispatches263WithoutChangingPreviousBackends() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        try (var logger = mockStatic(XLogger.class)) {
            for (String version : new String[]{"26.3", "26.3-R0.1-SNAPSHOT", "26.3.build.159-beta"}) {
                when(server.getBukkitVersion()).thenReturn(version);
                assertEquals(XVersionManager.ImplementationVersion.v26_3, XVersionManager.GetVersion(plugin));
            }
            when(server.getBukkitVersion()).thenReturn("26.2");
            assertEquals(XVersionManager.ImplementationVersion.v26_2, XVersionManager.GetVersion(plugin));
            when(server.getBukkitVersion()).thenReturn("26.1.2");
            assertEquals(XVersionManager.ImplementationVersion.v26, XVersionManager.GetVersion(plugin));
        }
    }

    @Test
    void orders263Before262ForListenerBoundsAndFallback() {
        var current = XVersionManager.ImplementationVersion.v26_3;
        assertEquals(XVersionManager.ImplementationVersion.v26_2, current.getPrevious());
        assertTrue(current.compareWith(XVersionManager.ImplementationVersion.v26_2) > 0);
        assertTrue(current.compareWith(XVersionManager.ImplementationVersion.v1_21_11) > 0);
    }
}
