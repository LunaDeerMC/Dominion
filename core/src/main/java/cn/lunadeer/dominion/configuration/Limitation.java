package cn.lunadeer.dominion.configuration;

import cn.lunadeer.dominion.utils.XLogger;
import cn.lunadeer.dominion.utils.configuration.*;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static cn.lunadeer.dominion.misc.Converts.toWorld;

@Headers({
        "This file controls how player can using the dominion feature.",
        "For more information, please visit:",
        "https://dominion.lunadeer.cn/en/notes/doc/owner/config-ref/limitations/"
})
public class Limitation extends ConfigurationFile {
    public static class LimitationText extends ConfigurationPart {
        public String loadingWorldSettings = "Loading world-settings...";
        public String loadingWorldSetting = "Loading world-settings of {0}...";
        public String loadWorldSettingFailed = "Failed to world-settings of {0} reason: {1}";
        public String loadWorldSettingsSuccess = "Successfully loaded {0} world-settings: {1}.";
    }


    @Comments("Do not modify this value.")
    public static int version = 2;  // <<<<<< When you change the configuration, you should increment this value.

    @Comments({
            "The priority of group when player has multiple groups.",
            "The group with higher number will be used first."
    })
    public int priority = 0;

    @Comments({
            "The settings of the economy support.",
            "You need to install vault and a economy plugin to use this feature."
    })
    public Economy economy = new Economy();

    public static class Economy extends ConfigurationPart {
        public boolean enable = false;
        @Comments("The price per block to claim dominion.")
        public double pricePerBlock = 10.0;
        @Comments("Only count the square blocks, ignore the height.")
        public boolean squareOnly = false;
        @Comments("The refund rate when player contract or delete dominion.")
        public double refundRate = 0.85;
    }

    @Comments("The settings of the teleportation feature.")
    public Teleportation teleportation = new Teleportation();

    public static class Teleportation extends ConfigurationPart {
        @Comments("Enable the teleportation feature.")
        public boolean enable = true;
        @Comments("The cooldown time of teleportation in seconds. 0 means no cooldown.")
        public int cooldown = 10;
        @Comments("Delay time before teleportation in seconds. 0 means no delay.")
        public int delay = 5;
    }

    @Comments({
            "How many dominions a player can create all over the world.",
            "Set -1 means no limitation."
    })
    public int amountAllOverTheWorld = 10;

    @HandleManually // See comments at loadWorldLimitationSettings() method
    public Map<String, WorldLimitationSetting> worldLimitations = new HashMap<>();
    @HandleManually
    private final String worldLimitationsKey = "world-limitations";

    public static class WorldLimitationSetting extends ConfigurationPart {
        @Comments({
                "The maximum amount of dominions a player can create in this world.",
                "Set -1 means no limitation (but still limited by amount-all-over-the-world)."
        })
        public int amount = 5;
        @Comments({
                "The depth limit of sub dominion.",
                "Set to -1 means no limitation, 0 means no sub dominion.",
        })
        public int maxSubDominionDepth = 3;
        @Comments({
                "Dominion's top no higher than this value.",
                "Should be larger than no-lower-than."
        })
        public int noHigherThan = 320;
        @Comments({
                "Dominion's bottom no lower than this value.",
                "Should be smaller than no-higher-than."
        })
        public int noLowerThan = -64;
        @Comments({
                "The maximum size of dominion in X axis (East-West).",
                "Set -1 means no limitation. Should be larger than size-min-x and 0."
        })
        public int sizeMaxX = 128;
        @Comments({
                "The maximum size of dominion in Y axis (Height).",
                "Set -1 means no limitation. Should be larger than size-min-y and 0."
        })
        public int sizeMaxY = 64;
        @Comments({
                "The maximum size of dominion in Z axis (North-South).",
                "Set -1 means no limitation. Should be larger than size-min-z and 0."
        })
        public int sizeMaxZ = 128;
        @Comments({
                "The minimum size of dominion in X axis (East-West).",
                "Should be smaller than size-max-x and larger than 0."
        })
        public int sizeMinX = 4;
        @Comments({
                "The minimum size of dominion in Y axis (Height).",
                "Should be smaller than size-max-y and larger than 0."
        })
        public int sizeMinY = 4;
        @Comments({
                "The minimum size of dominion in Z axis (North-South).",
                "Should be smaller than size-max-z and larger than 0."
        })
        public int sizeMinZ = 4;
        @Comments("Weather to include all vertical blocks when calculating the size.")
        public boolean autoIncludeVertical = false;
    }

    @PreProcess
    public void checkWorldLimitationSettings() {
        worldLimitations.put("default", new WorldLimitationSetting());
        if (!getYaml().contains(worldLimitationsKey)) {
            getYaml().createSection(worldLimitationsKey);
            getYaml().setComments(worldLimitationsKey, List.of(
                    "The settings of the limitations for each world.",
                    "The default settings will be used if the world is not listed here.",
                    "Do not delete the default."
            ));
        }
    }

    @PostProcess(priority = 1)
    public void loadWorldLimitationSettings() {
        XLogger.info(Language.limitationText.loadingWorldSettings);
        ConfigurationSection section = getYaml().getConfigurationSection(worldLimitationsKey);
        if (section == null) {
            return;
        }
        Set<String> keys = section.getKeys(false);
        for (String key : keys) {
            XLogger.info(Language.limitationText.loadingWorldSetting, key);
            try {
                WorldLimitationSetting settings = new WorldLimitationSetting();
                ConfigurationManager.readConfigurationPart(getYaml(), settings, worldLimitationsKey + "." + key);
                worldLimitations.put(key, settings);
            } catch (Exception e) {
                XLogger.error(Language.limitationText.loadWorldSettingFailed, key, e.getMessage());
            }
        }
        XLogger.info(Language.limitationText.loadWorldSettingsSuccess, worldLimitations.size(), String.join(", ", worldLimitations.keySet()));
    }

    @PostProcess(priority = 2)
    public void checkLimitationParams() {
        if (economy.pricePerBlock < 0) {
            economy.pricePerBlock = 0;
        }

        if (economy.refundRate < 0) {
            economy.refundRate = 0;
        } else if (economy.refundRate > 1) {
            economy.refundRate = 1;
        }

        if (amountAllOverTheWorld < 0) {
            amountAllOverTheWorld = -1;
        }

        if (worldLimitations.get("default") == null) {
            worldLimitations.put("default", new WorldLimitationSetting());
        }

        for (WorldLimitationSetting setting : worldLimitations.values()) {
            if (setting.amount < 0) {
                setting.amount = -1;
            }

            if (setting.amount > amountAllOverTheWorld && amountAllOverTheWorld != -1) {
                setting.amount = amountAllOverTheWorld;
            }

            if (setting.maxSubDominionDepth < 0) {
                setting.maxSubDominionDepth = -1;
            }

            if (setting.noHigherThan <= setting.noLowerThan) {
                setting.noHigherThan = setting.noLowerThan + 1;
            }

            if (setting.sizeMaxX < 0) {
                setting.sizeMaxX = -1;
            }
            if (setting.sizeMaxY < 0) {
                setting.sizeMaxY = -1;
            }
            if (setting.sizeMaxZ < 0) {
                setting.sizeMaxZ = -1;
            }

            if (setting.sizeMinX <= 0) {
                setting.sizeMinX = 1;
            }
            if (setting.sizeMaxX < setting.sizeMinX && setting.sizeMaxX != -1) {
                setting.sizeMaxX = setting.sizeMinX + 1;
            }
            if (setting.sizeMinY <= 0) {
                setting.sizeMinY = 1;
            }
            if (setting.sizeMaxY < setting.sizeMinY && setting.sizeMaxY != -1) {
                setting.sizeMaxY = setting.sizeMinY + 1;
            }
            if (setting.sizeMinZ <= 0) {
                setting.sizeMinZ = 1;
            }
            if (setting.sizeMaxZ < setting.sizeMinZ && setting.sizeMaxZ != -1) {
                setting.sizeMaxZ = setting.sizeMinZ + 1;
            }

        }
    }

    @PostProcess(priority = 3)
    public void saveWorldLimitationSettings() {
        for (Map.Entry<String, WorldLimitationSetting> entry : worldLimitations.entrySet()) {
            try {
                String prefix = worldLimitationsKey + "." + entry.getKey();
                ConfigurationManager.writeConfigurationPart(getYaml(), entry.getValue(), prefix, !Objects.equals(entry.getKey(), "default"));
            } catch (Exception e) {
                XLogger.warn("Failed to save world limitation settings for world {0}", entry.getKey());
            }
        }
    }

    public @NotNull Limitation.WorldLimitationSetting getWorldSettings(@NotNull UUID worldUUID) {
        return getWorldSettings(toWorld(worldUUID).getName());
    }

    public @NotNull Limitation.WorldLimitationSetting getWorldSettings(@NotNull World world) {
        return getWorldSettings(world.getName());
    }

    public @NotNull Limitation.WorldLimitationSetting getWorldSettings(@Nullable String worldName) {
        return Objects.requireNonNullElse(worldLimitations.get(worldName), worldLimitations.get("default"));
    }
}
