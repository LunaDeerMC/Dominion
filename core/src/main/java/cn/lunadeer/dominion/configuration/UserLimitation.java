package cn.lunadeer.dominion.configuration;

import cn.lunadeer.dominion.Dominion;
import cn.lunadeer.dominion.utils.XLogger;
import cn.lunadeer.dominion.utils.configuration.ConfigurationPart;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages per-player limitation overrides.
 * <p>
 * This class loads player-specific limitation settings from limitations/users.yml.
 * Players configured in this file will have their limitations overridden,
 * taking priority over group-based limitations.
 */
public class UserLimitation {

    public static class UserLimitationText extends ConfigurationPart {
        public String loadingUserLimitations = "Loading user limitations...";
        public String loadUserLimitationFailed = "Failed to load user limitation for {0}, reason: {1}";
        public String loadUserLimitationsSuccess = "Successfully loaded {0} user limitations: {1}.";
        public String savingDefaultUserLimitation = "Creating default users.yml file...";
    }

    private static final Map<String, Limitation> userLimitations = new HashMap<>();
    private static File usersFile;
    private static YamlConfiguration yaml;

    /**
     * Load user limitations from the users.yml file.
     * This should be called after the main limitations are loaded.
     */
    public static void load() {
        XLogger.info(Language.userLimitationText.loadingUserLimitations);
        userLimitations.clear();

        File limitationsFolder = new File(Dominion.instance.getDataFolder(), "limitations");
        if (!limitationsFolder.exists()) {
            limitationsFolder.mkdirs();
        }

        usersFile = new File(limitationsFolder, "users.yml");
        if (!usersFile.exists()) {
            try {
                XLogger.info(Language.userLimitationText.savingDefaultUserLimitation);
                createDefaultUsersFile();
            } catch (IOException e) {
                XLogger.warn(Language.configurationText.saveLimitationFail, e.getMessage());
                return;
            }
        }

        yaml = YamlConfiguration.loadConfiguration(usersFile);
        Set<String> playerNames = yaml.getKeys(false);

        // Skip comment keys
        playerNames.removeIf(key -> key.startsWith("#") || key.equals("_comment"));

        for (String playerName : playerNames) {
            try {
                Limitation limitation = parsePlayerLimitation(playerName);
                if (limitation != null) {
                    userLimitations.put(playerName.toLowerCase(), limitation);
                }
            } catch (Exception e) {
                XLogger.warn(Language.userLimitationText.loadUserLimitationFailed, playerName, e.getMessage());
            }
        }

        XLogger.info(Language.userLimitationText.loadUserLimitationsSuccess,
                userLimitations.size(),
                String.join(", ", userLimitations.keySet()));
    }

    /**
     * Create the default users.yml file with example configuration.
     */
    private static void createDefaultUsersFile() throws IOException {
        usersFile.createNewFile();
        YamlConfiguration defaultYaml = new YamlConfiguration();

        defaultYaml.options().setHeader(Arrays.asList(
                "User-specific limitation overrides.",
                "Players configured here will have their limitations overridden,",
                "taking priority over group-based limitations.",
                "",
                "Format:",
                "",
                "  player_name:",
                "    amount-all-over-the-world: 10",
                "    priority: 100",
                "    economy:",
                "      enable: true",
                "      price-per-block: 5.0",
                "      square-only: false",
                "      refund-rate: 0.9",
                "    teleportation:",
                "      enable: true",
                "      cooldown: 5",
                "      delay: 3",
                "    world-limitations:",
                "      world:",
                "        amount: 3",
                "        max-sub-dominion-depth: 2",
                "        size-max-x: 256",
                "      world_nether:",
                "        amount: 1",
                "",
                "All settings are optional. If not specified, the default limitation will be used.",
                "Player names are case-insensitive.",
                "",
                "Available world-limitation settings:",
                "  amount, max-sub-dominion-depth, no-higher-than, no-lower-than,",
                "  size-max-x, size-max-y, size-max-z, size-min-x, size-min-y, size-min-z,",
                "  auto-include-vertical"
        ));

        defaultYaml.options().width(250);
        defaultYaml.save(usersFile);
    }

    /**
     * Parse a player's limitation from the YAML configuration.
     *
     * @param playerName The player's name
     * @return The parsed Limitation object, or null if parsing fails
     */
    private static @Nullable Limitation parsePlayerLimitation(@NotNull String playerName) throws Exception {
        ConfigurationSection playerSection = yaml.getConfigurationSection(playerName);
        if (playerSection == null) {
            return null;
        }

        // Get the base limitation
        Limitation baseLimitation = Configuration.limitations.get("default");
        if (baseLimitation == null) {
            baseLimitation = new Limitation();
        }

        // Create a new limitation instance for this player
        Limitation playerLimitation = new Limitation();

        // Set priority to maximum by default
        playerLimitation.priority = playerSection.getInt("priority", Integer.MAX_VALUE);

        // Override amount-all-over-the-world if specified
        if (playerSection.contains("amount-all-over-the-world")) {
            playerLimitation.amountAllOverTheWorld = playerSection.getInt("amount-all-over-the-world");
        } else {
            playerLimitation.amountAllOverTheWorld = baseLimitation.amountAllOverTheWorld;
        }

        // Override economy settings if specified
        if (playerSection.contains("economy")) {
            ConfigurationSection economySection = playerSection.getConfigurationSection("economy");
            if (economySection != null) {
                playerLimitation.economy = new Limitation.Economy();
                playerLimitation.economy.enable = economySection.getBoolean("enable", baseLimitation.economy.enable);
                playerLimitation.economy.pricePerBlock = economySection.getDouble("price-per-block", baseLimitation.economy.pricePerBlock);
                playerLimitation.economy.squareOnly = economySection.getBoolean("square-only", baseLimitation.economy.squareOnly);
                playerLimitation.economy.refundRate = economySection.getDouble("refund-rate", baseLimitation.economy.refundRate);
            } else {
                playerLimitation.economy = baseLimitation.economy;
            }
        } else {
            playerLimitation.economy = baseLimitation.economy;
        }

        // Override teleportation settings if specified
        if (playerSection.contains("teleportation")) {
            ConfigurationSection tpSection = playerSection.getConfigurationSection("teleportation");
            if (tpSection != null) {
                playerLimitation.teleportation = new Limitation.Teleportation();
                playerLimitation.teleportation.enable = tpSection.getBoolean("enable", baseLimitation.teleportation.enable);
                playerLimitation.teleportation.cooldown = tpSection.getInt("cooldown", baseLimitation.teleportation.cooldown);
                playerLimitation.teleportation.delay = tpSection.getInt("delay", baseLimitation.teleportation.delay);
            } else {
                playerLimitation.teleportation = baseLimitation.teleportation;
            }
        } else {
            playerLimitation.teleportation = baseLimitation.teleportation;
        }

        // Copy default world limitations first
        playerLimitation.worldLimitations = new HashMap<>(baseLimitation.worldLimitations);

        // Override world-limitations if specified
        if (playerSection.contains("world-limitations")) {
            ConfigurationSection worldLimitationsSection = playerSection.getConfigurationSection("world-limitations");
            if (worldLimitationsSection != null) {
                for (String worldName : worldLimitationsSection.getKeys(false)) {
                    ConfigurationSection worldSection = worldLimitationsSection.getConfigurationSection(worldName);
                    if (worldSection != null) {
                        // Get base world settings
                        Limitation.WorldLimitationSetting baseWorldSettings =
                                baseLimitation.worldLimitations.getOrDefault(worldName,
                                        baseLimitation.worldLimitations.get("default"));

                        Limitation.WorldLimitationSetting worldSettings = new Limitation.WorldLimitationSetting();

                        // Override each setting if specified, otherwise use base
                        worldSettings.amount = worldSection.getInt("amount", baseWorldSettings.amount);
                        worldSettings.maxSubDominionDepth = worldSection.getInt("max-sub-dominion-depth", baseWorldSettings.maxSubDominionDepth);
                        worldSettings.noHigherThan = worldSection.getInt("no-higher-than", baseWorldSettings.noHigherThan);
                        worldSettings.noLowerThan = worldSection.getInt("no-lower-than", baseWorldSettings.noLowerThan);
                        worldSettings.sizeMaxX = worldSection.getInt("size-max-x", baseWorldSettings.sizeMaxX);
                        worldSettings.sizeMaxY = worldSection.getInt("size-max-y", baseWorldSettings.sizeMaxY);
                        worldSettings.sizeMaxZ = worldSection.getInt("size-max-z", baseWorldSettings.sizeMaxZ);
                        worldSettings.sizeMinX = worldSection.getInt("size-min-x", baseWorldSettings.sizeMinX);
                        worldSettings.sizeMinY = worldSection.getInt("size-min-y", baseWorldSettings.sizeMinY);
                        worldSettings.sizeMinZ = worldSection.getInt("size-min-z", baseWorldSettings.sizeMinZ);
                        worldSettings.autoIncludeVertical = worldSection.getBoolean("auto-include-vertical", baseWorldSettings.autoIncludeVertical);

                        playerLimitation.worldLimitations.put(worldName, worldSettings);
                    }
                }
            }
        }

        return playerLimitation;
    }

    /**
     * Get the limitation for a specific player by name.
     *
     * @param playerName The player's name (case-insensitive)
     * @return The player's specific limitation, or null if not configured
     */
    public static @Nullable Limitation getPlayerLimitation(@NotNull String playerName) {
        return userLimitations.get(playerName.toLowerCase());
    }

    /**
     * Check if a player has a specific limitation configured.
     *
     * @param playerName The player's name (case-insensitive)
     * @return true if the player has a specific limitation configured
     */
    public static boolean hasPlayerLimitation(@NotNull String playerName) {
        return userLimitations.containsKey(playerName.toLowerCase());
    }

    /**
     * Get all configured player limitations.
     *
     * @return An unmodifiable map of player names to their limitations
     */
    public static Map<String, Limitation> getAllPlayerLimitations() {
        return Collections.unmodifiableMap(userLimitations);
    }

    /**
     * Reload user limitations from the file.
     */
    public static void reload() {
        load();
    }
}