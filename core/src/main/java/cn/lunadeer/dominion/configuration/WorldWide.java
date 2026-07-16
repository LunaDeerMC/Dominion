package cn.lunadeer.dominion.configuration;

import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.Flag;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.handler.WorldLoadHandler;
import cn.lunadeer.dominion.utils.XLogger;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldWide {

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
        String worldName = file.getName().replace(".yml", "");

        WorldConfig world = new WorldConfig();
        if (worlds.containsKey(worldName)) world = worlds.get(worldName);

        if (!file.exists()) return;    // if no file exisit skip loading and keep default

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        world.enabled = config.getBoolean("enabled", false);

        for (Flag flag : Flags.getAllFlags()) {
            if (flag.getFlagName().equals(Flags.ADMIN.getFlagName())) continue; // not handle admin flag for world-wide config
            
            if (flag instanceof PriFlag priFlag) {
                world.guestPrivilegeFlags.put(priFlag, config.getBoolean(flag.getConfigurationNameKey(), flag.getDefaultValue()));
            } else if (flag instanceof EnvFlag envFlag) {
                world.environmentFlags.put(envFlag, config.getBoolean(flag.getConfigurationNameKey(), flag.getDefaultValue()));
            }
        }

        worlds.put(worldName, world);
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
        File[] files = rootPath.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            try {
                loadWorld(file);
            } catch (IOException e) {
                XLogger.error(e);
            }
        }
        
        // ensure to have a default world-wide setting to fallback
        if (worlds.size() <= 0 || !worlds.containsKey("default")) {
            worlds.put("default", new WorldConfig());
            saveWorld(rootPath, "default");
        }
    }

}
