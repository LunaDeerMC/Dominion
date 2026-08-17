package cn.lunadeer.dominion.configuration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldWideLoadTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsRootAndNestedWorldWideFilesWithoutReadingDirectories() throws Exception {
        Path rootPath = Files.createDirectories(tempDir.resolve("world-wide"));
        writeConfig(rootPath.resolve("default.yml"), false);
        writeConfig(rootPath.resolve("acidisland_world.yml"), true);

        Path nestedConfig = rootPath.resolve("acidisland_world").resolve("bentobox.yml");
        Files.createDirectories(nestedConfig.getParent());
        writeConfig(nestedConfig, true);

        Files.writeString(rootPath.resolve("ignored-world.txt"), "enabled: true\n");

        WorldWide.loadWorldFiles(rootPath.toFile());

        assertTrue(isLoadedAndEnabled("acidisland_world"));
        assertTrue(isLoadedAndEnabled("acidisland_world/bentobox"));
        assertFalse(loadedWorlds().containsKey("ignored-world"));
        assertEquals(3, YamlConfiguration.loadConfiguration(nestedConfig.toFile())
                .getInt("flag-schema-version"));
    }

    private static void writeConfig(Path path, boolean enabled) throws IOException {
        YamlConfiguration config = new YamlConfiguration();
        config.set("enabled", enabled);
        config.save(path.toFile());
    }

    private static boolean isLoadedAndEnabled(String worldName) throws ReflectiveOperationException {
        Object worldConfig = loadedWorlds().get(worldName);
        if (worldConfig == null) return false;

        Field enabled = worldConfig.getClass().getDeclaredField("enabled");
        enabled.setAccessible(true);
        return enabled.getBoolean(worldConfig);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> loadedWorlds() throws ReflectiveOperationException {
        Field worlds = WorldWide.class.getDeclaredField("worlds");
        worlds.setAccessible(true);
        return (Map<String, ?>) worlds.get(null);
    }
}
