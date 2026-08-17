package cn.lunadeer.dominion.configuration;

import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.Flag;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.utils.XLogger;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class WorldWide {
    private static final int FLAG_SCHEMA_VERSION = 4;
    private static final String WORLD_WIDE_FILE_EXTENSION = ".yml";

    private static class WorldConfig {
        private boolean enabled = false;
        private final Map<PriFlag, Boolean> guestPrivilegeFlags = new HashMap<>();
        private final Map<EnvFlag, Boolean> environmentFlags = new HashMap<>();
    }

    private static final Map<String, WorldConfig> worlds = new HashMap<>();

    public static boolean isWorldWideEnabled(World world) {
        if (!worlds.containsKey(world.getName())) {
            return worlds.get("default").enabled;
        }
        return worlds.containsKey(world.getName()) && worlds.get(world.getName()).enabled;
    }

    public static @Nullable Map<EnvFlag, Boolean> getEnvironmentFlagValue(World world) {
        if (!worlds.containsKey(world.getName())) {
            // If the world is not loaded, return null
            return worlds.get("default").environmentFlags;
        }
        return worlds.get(world.getName()).environmentFlags;
    }

    public static boolean getEnvFlagValue(World world, @NotNull EnvFlag flag) {
        if (!worlds.containsKey(world.getName())) {
            // If the world is not loaded, return the default value of the flag
            return worlds.get("default").environmentFlags.getOrDefault(flag, flag.getDefaultValue());
        }
        return worlds.get(world.getName()).environmentFlags.getOrDefault(flag, flag.getDefaultValue());
    }

    public static @Nullable Map<PriFlag, Boolean> getGuestPrivilegeFlagValue(World world) {
        if (!worlds.containsKey(world.getName())) {
            // If the world is not loaded, return null
            return worlds.get("default").guestPrivilegeFlags;
        }
        return worlds.get(world.getName()).guestPrivilegeFlags;
    }

    public static boolean getGuestFlagValue(World world, @NotNull PriFlag flag) {
        if (!worlds.containsKey(world.getName())) {
            // If the world is not loaded, return the default value of the flag
            return worlds.get("default").guestPrivilegeFlags.getOrDefault(flag, flag.getDefaultValue());
        }
        return worlds.get(world.getName()).guestPrivilegeFlags.getOrDefault(flag, flag.getDefaultValue());
    }

    protected static void loadWorld(File file) throws IOException {
        if (!isWorldWideFile(file.toPath())) return;
        loadWorld(file, stripExtension(file.getName()));
    }

    private static void loadWorld(File file, String worldName) throws IOException {

        WorldConfig world = new WorldConfig();
        if (worlds.containsKey(worldName)) world = worlds.get(worldName);

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        world.enabled = config.getBoolean("enabled", false);
        int schemaVersion = config.getInt("flag-schema-version", 1);
        boolean changed = false;

        for (Flag flag : Flags.getAllFlags()) {
            if (flag.getFlagName().equals(Flags.ADMIN.getFlagName())) continue; // not handle admin flag for world-wide config

            if (!config.contains(flag.getConfigurationNameKey())) {
                boolean value = migratedValue(config, flag, schemaVersion < FLAG_SCHEMA_VERSION);
                if (schemaVersion < FLAG_SCHEMA_VERSION && Flags.preserveAllowedSpawnEggValue(flag)) value = true;
                config.set(flag.getConfigurationNameKey(), value);
                changed = true;
            }
            if (flag instanceof PriFlag priFlag) {
                world.guestPrivilegeFlags.put(priFlag, config.getBoolean(flag.getConfigurationNameKey(), flag.getDefaultValue()));
            } else if (flag instanceof EnvFlag envFlag) {
                world.environmentFlags.put(envFlag, config.getBoolean(flag.getConfigurationNameKey(), flag.getDefaultValue()));
            }
        }

        if (schemaVersion < FLAG_SCHEMA_VERSION) {
            config.set("flag-schema-version", FLAG_SCHEMA_VERSION);
            changed = true;
        }
        if (changed) config.save(file);
        worlds.put(worldName, world);
    }

    private static boolean migratedValue(YamlConfiguration config, Flag flag, boolean migrateSplitFlags) {
        if (!migrateSplitFlags) return flag.getDefaultValue();
        List<Flag> sources = Flags.getLegacySources(flag);
        if (sources.isEmpty()) return flag.getDefaultValue();
        boolean value = true;
        for (Flag source : sources) {
            value &= config.getBoolean(source.getConfigurationNameKey(), source.getDefaultValue());
        }
        return value;
    }

    static void loadWorldFiles(File rootPath) throws IOException {
        Path root = rootPath.toPath().toAbsolutePath().normalize();
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(WorldWide::isWorldWideFile)
                    .forEach(file -> {
                        try {
                            loadWorld(file.toFile(), getWorldName(root, file));
                        } catch (IOException e) {
                            XLogger.error(e);
                        }
                    });
        }
    }

    private static boolean isWorldWideFile(Path path) {
        if (!Files.isRegularFile(path)) return false;
        Path fileName = path.getFileName();
        if (fileName == null) return false;
        String name = fileName.toString();
        return name.length() > WORLD_WIDE_FILE_EXTENSION.length()
                && name.endsWith(WORLD_WIDE_FILE_EXTENSION);
    }

    private static String getWorldName(Path rootPath, Path file) {
        String relativePath = rootPath.relativize(file.toAbsolutePath().normalize()).toString();
        return stripExtension(relativePath).replace(File.separatorChar, '/');
    }

    private static String stripExtension(String fileName) {
        return fileName.substring(0, fileName.length() - WORLD_WIDE_FILE_EXTENSION.length());
    }

    protected static void saveWorld(File worldWideRootPath, String worldName) throws IOException {
        WorldConfig world = new WorldConfig();
        if (worlds.containsKey(worldName)) world = worlds.get(worldName);

        File worldWideFile = new File(worldWideRootPath, worldName + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(worldWideFile);

        if (config.get("enabled") == null) {
            config.setInlineComments("enabled", List.of("Enable or disable world-wide dominion for this world"));
        }
        config.set("enabled", world.enabled);
        config.set("flag-schema-version", FLAG_SCHEMA_VERSION);

        for (Flag flag : Flags.getAllFlags()) {
            if (flag.getFlagName().equals(Flags.ADMIN.getFlagName())) continue;

            if (config.get(flag.getConfigurationNameKey()) == null) {
                config.setInlineComments(flag.getConfigurationNameKey(), List.of(flag.getDisplayName() + " - " + flag.getDescription()));
            }
            config.set(flag.getConfigurationNameKey(), flag.getDefaultValue());
        }

        config.save(worldWideFile);
    }

    public static void load(CommandSender sender, JavaPlugin plugin) throws IOException {
        File rootPath = new File(plugin.getDataFolder(), "world-wide");
        if (!rootPath.exists()) {
            // create the root directory if it does not exist
            if (!rootPath.mkdirs()) {
                throw new RuntimeException("Failed to create world-wide dominion directory: " + rootPath.getAbsolutePath());
            }
        }
        loadWorldFiles(rootPath);
        
        // ensure to have a default world-wide setting to fallback
        if (worlds.size() <= 0 || !worlds.containsKey("default")) {
            worlds.put("default", new WorldConfig());
            saveWorld(rootPath, "default");
        }
    }

}
