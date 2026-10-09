package cn.lunadeer.dominion.configuration;

import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.cache.CacheManager;
import cn.lunadeer.dominion.misc.Others;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorldWideLegacyFlagTest {
    @TempDir
    Path tempDir;

    @Test
    void legacyReadsAndChecksFollowTheCurrentWorldPermissions() throws Exception {
        var file = tempDir.resolve("legacy-api-world.yml").toFile();
        YamlConfiguration config = new YamlConfiguration();
        config.set("enabled", true);
        config.set("flag-schema-version", 5);
        config.set(Flags.CHEST.getConfigurationNameKey(), true);
        config.set(Flags.BURN_BLOCK.getConfigurationNameKey(), false);
        config.save(file);
        WorldWide.loadWorld(file);
        World world = mock(World.class);
        when(world.getName()).thenReturn("legacy-api-world");
        Location location = new Location(world, 0, 0, 0);
        Player player = mock(Player.class);
        CacheManager previous = CacheManager.instance;
        boolean chestEnabled = Flags.CHEST.getEnable();
        boolean burnEnabled = Flags.BURN_BLOCK.getEnable();
        try {
            CacheManager.instance = mock(CacheManager.class);
            Flags.CHEST.setEnable(true);
            Flags.BURN_BLOCK.setEnable(true);
            Map<PriFlag, Boolean> view = WorldWide.getGuestPrivilegeFlagValue(world);
            assertEquals(true, view.get(Flags.CONTAINER));
            assertTrue(WorldWide.getGuestFlagValue(world, Flags.CONTAINER));
            assertTrue(Others.checkPrivilegeFlagSilence(location, Flags.CONTAINER, player, null));
            view.put(Flags.CHEST, false);
            assertEquals(false, view.get(Flags.CONTAINER));
            assertFalse(Others.checkPrivilegeFlagSilence(location, Flags.CONTAINER, player, null));
            assertFalse(Others.checkEnvironmentFlag(location, Flags.BURN, null));
            WorldWide.getEnvironmentFlagValue(world).put(Flags.BURN_BLOCK, true);
            assertTrue(Others.checkEnvironmentFlag(location, Flags.BURN, null));
        } finally {
            CacheManager.instance = previous;
            Flags.CHEST.setEnable(chestEnabled);
            Flags.BURN_BLOCK.setEnable(burnEnabled);
        }
        YamlConfiguration saved = YamlConfiguration.loadConfiguration(file);
        assertFalse(saved.contains(Flags.CONTAINER.getConfigurationNameKey()));
        assertFalse(saved.contains(Flags.BURN.getConfigurationNameKey()));
    }
}
