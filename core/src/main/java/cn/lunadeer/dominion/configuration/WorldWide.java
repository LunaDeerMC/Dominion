package cn.lunadeer.dominion.configuration;

import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.Flag;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
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
        return worlds.containsKey(world.getName()) && worlds.get(world.getName()).enabled;
    }

    public static @Nullable Map<EnvFlag, Boolean> getEnvironmentFlagValue(World world) {
        if (!worlds.containsKey(world.getName())) {
            // If the world is not loaded, return null
            return null;
        }
        return worlds.get(world.getName()).environmentFlags;
    }

    public static boolean getEnvFlagValue(World world, @NotNull EnvFlag flag) {
        if (!worlds.containsKey(world.getName())) {
            // If the world is not loaded, return the default value of the flag
            return flag.getDefaultValue();
        }
        return worlds.get(world.getName()).environmentFlags.getOrDefault(flag, flag.getDefaultValue());
    }

    public static @Nullable Map<PriFlag, Boolean> getGuestPrivilegeFlagValue(World world) {
        if (!worlds.containsKey(world.getName())) {
            // If the world is not loaded, return null
            return null;
        }
        return worlds.get(world.getName()).guestPrivilegeFlags;
    }

    public static boolean getGuestFlagValue(World world, @NotNull PriFlag flag) {
        if (!worlds.containsKey(world.getName())) {
            // If the world is not loaded, return the default value of the flag
            return flag.getDefaultValue();
        }
        return worlds.get(world.getName()).guestPrivilegeFlags.getOrDefault(flag, flag.getDefaultValue());
    }

    protected static void loadWorld(File worldWideRootPath, String worldName) throws IOException {
        if (!worlds.containsKey(worldName)) {
            worlds.put(worldName, new WorldConfig());
        }

        File worldWideFile = getFile(worldWideRootPath, worldName);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(worldWideFile);

        // enable configuration
        if (config.get("enabled") == null) {
            config.set("enabled", false);
            config.setInlineComments("enabled", List.of("Enable or disable world-wide dominion for this world"));
        }
        worlds.get(worldName).enabled = config.getBoolean("enabled", false);

        // flags configuration
        for (Flag flag : Flags.getAllFlags()) {
            if (flag.getFlagName().equals(Flags.ADMIN.getFlagName())) continue;
            if (config.get(flag.getConfigurationNameKey()) == null) {
                config.set(flag.getConfigurationNameKey(), flag.getDefaultValue());
            }
            config.setInlineComments(flag.getConfigurationNameKey(),
                    List.of(flag.getDisplayName() + " - " + flag.getDescription())
            );
            if (flag instanceof PriFlag priFlag) {
                worlds.get(worldName).guestPrivilegeFlags.put(priFlag, config.getBoolean(flag.getConfigurationNameKey(), flag.getDefaultValue()));
            } else if (flag instanceof EnvFlag envFlag) {
                worlds.get(worldName).environmentFlags.put(envFlag, config.getBoolean(flag.getConfigurationNameKey(), flag.getDefaultValue()));
            }
        }
        config.save(worldWideFile);
    }

    private static @NotNull File getFile(File worldWideRootPath, String worldName) {
        File worldWideFile = new File(worldWideRootPath, worldName + ".yml");
        if (!worldWideFile.exists()) {
            // create the world-wide dominion file if it does not exist
            try {
                if (!worldWideFile.createNewFile()) {
                    throw new RuntimeException("Failed to create world-wide dominion file: " + worldWideFile.getAbsolutePath());
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to create world-wide dominion file: " + worldWideFile.getAbsolutePath(), e);
            }
        }
        return worldWideFile;
    }

    public static void load(CommandSender sender, JavaPlugin plugin) throws IOException {
        File rootPath = new File(plugin.getDataFolder(), "world-wide");
        if (!rootPath.exists()) {
            // create the root directory if it does not exist
            if (!rootPath.mkdirs()) {
                throw new RuntimeException("Failed to create world-wide dominion directory: " + rootPath.getAbsolutePath());
            }
        }
        for (World world : plugin.getServer().getWorlds()) {
            String worldName = world.getName();
            loadWorld(rootPath, worldName);
        }
    }

}
