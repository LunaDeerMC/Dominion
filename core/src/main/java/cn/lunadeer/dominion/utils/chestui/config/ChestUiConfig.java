package cn.lunadeer.dominion.utils.chestui.config;

import cn.lunadeer.dominion.configuration.Language.LanguageCode;
import cn.lunadeer.dominion.utils.XLogger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class ChestUiConfig {
    private static final String LAYOUT_RESOURCE = "languages/chest-ui/layout.yml";
    private static final String ENGLISH_RESOURCE = "languages/chest-ui/texts/en_us.yml";
    private final JavaPlugin plugin;
    private record Snapshot(Map<String, MenuDefinition> menus, YamlConfiguration text) {}
    private record LoadResult(YamlConfiguration yaml, boolean parsed) {}
    private volatile Snapshot snapshot = new Snapshot(Map.of(), new YamlConfiguration());

    public ChestUiConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public synchronized void load(String language) throws IOException {
        backupLegacyDirectory();
        File root = new File(plugin.getDataFolder(), "languages/chest-ui");
        File layoutFile = new File(root, "layout.yml");
        
        saveResourceIfMissing(LAYOUT_RESOURCE, layoutFile);
        for (LanguageCode code : LanguageCode.values()) {
            File textFile = new File(root, "texts/" + code.name() + ".yml");
            saveResourceIfMissing("languages/chest-ui/texts/" + code.name() + ".yml", textFile);
        }

        YamlConfiguration defaultLayout = loadResource(LAYOUT_RESOURCE);
        LoadResult layoutResult = loadUserOrDefault(layoutFile, defaultLayout);
        YamlConfiguration userLayout = layoutResult.yaml();
        mergeMissing(userLayout, defaultLayout);
        if (layoutResult.parsed()) userLayout.save(layoutFile);

        File selectedFile = selectTextFile(root, language);
        YamlConfiguration defaultText = loadResource(ENGLISH_RESOURCE);
        LoadResult textResult = loadUserOrDefault(selectedFile, defaultText);
        YamlConfiguration userText = textResult.yaml();
        mergeMissing(userText, defaultText);
        if (textResult.parsed()) userText.save(selectedFile);

        Map<String, MenuDefinition> loaded = new HashMap<>();
        ConfigurationSection defaults = defaultLayout.getConfigurationSection("menus");
        ConfigurationSection users = userLayout.getConfigurationSection("menus");
        if (defaults == null) throw new IOException("Default chest UI layout has no menus section");
        for (String id : defaults.getKeys(false)) {
            try {
                loaded.put(id, MenuDefinition.read(users == null ? null : users.getConfigurationSection(id)));
            } catch (Exception e) {
                XLogger.warn("Invalid chest UI menu {0}, using built-in defaults: {1}", id, e.getMessage());
                loaded.put(id, MenuDefinition.read(defaults.getConfigurationSection(id)));
            }
        }
        snapshot = new Snapshot(Map.copyOf(loaded), userText);
    }

    public MenuDefinition menu(String id) {
        MenuDefinition definition = snapshot.menus().get(id);
        if (definition == null) throw new IllegalArgumentException("Unknown chest UI menu: " + id);
        return definition;
    }

    public String text(String path) {
        return snapshot.text().getString(path, path);
    }

    public List<String> textList(String path) {
        YamlConfiguration text = snapshot.text();
        Object value = text.get(path);
        if (value instanceof List<?>) return text.getStringList(path);
        String single = text.getString(path);
        return single == null ? List.of() : List.of(single);
    }

    private void backupLegacyDirectory() {
        File legacy = new File(plugin.getDataFolder(), "languages/cui");
        if (!legacy.isDirectory()) return;
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        File backup = new File(legacy.getParentFile(), "cui-backup-" + timestamp);
        int suffix = 1;
        while (backup.exists()) backup = new File(legacy.getParentFile(), "cui-backup-" + timestamp + "-" + suffix++);
        try {
            Files.move(legacy.toPath(), backup.toPath(), StandardCopyOption.ATOMIC_MOVE);
            XLogger.warn("Dominion chest UI has been rebuilt. Legacy CUI configuration was moved to {0}; layout and text must be configured again.", backup.getPath());
        } catch (Exception atomicFailed) {
            try {
                Files.move(legacy.toPath(), backup.toPath());
                XLogger.warn("Dominion chest UI has been rebuilt. Legacy CUI configuration was moved to {0}; layout and text must be configured again.", backup.getPath());
            } catch (Exception e) {
                XLogger.warn("Unable to back up legacy CUI directory: {0}", e.getMessage());
            }
        }
    }

    private void saveResourceIfMissing(String resource, File target) throws IOException {
        if (target.exists()) return;
        try {
            // Keep extraction consistent with the regular language files: the resource path is
            // also its path below plugins/Dominion.
            plugin.saveResource(resource, false);
        } catch (IllegalArgumentException e) {
            throw new IOException("Unable to extract bundled resource " + resource, e);
        }
    }

    private YamlConfiguration loadResource(String resource) throws IOException {
        InputStream input = plugin.getResource(resource);
        if (input == null) throw new FileNotFoundException(resource);
        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        }
    }

    private LoadResult loadUserOrDefault(File file, YamlConfiguration defaults) {
        try {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.load(file);
            return new LoadResult(yaml, true);
        } catch (Exception e) {
            XLogger.warn("Unable to parse chest UI file {0}; using built-in defaults without overwriting it: {1}", file.getPath(), e.getMessage());
            return new LoadResult(defaults, false);
        }
    }

    static void mergeMissing(ConfigurationSection target, ConfigurationSection defaults) {
        for (String key : defaults.getKeys(false)) {
            Object defaultValue = defaults.get(key);
            if (!target.contains(key)) {
                target.set(key, defaultValue);
            } else if (defaultValue instanceof ConfigurationSection defaultSection) {
                ConfigurationSection targetSection = target.getConfigurationSection(key);
                if (targetSection != null) mergeMissing(targetSection, defaultSection);
            }
        }
    }

    static File selectTextFile(File root, String language) {
        File selected = new File(root, "texts/" + language.toLowerCase(Locale.ROOT) + ".yml");
        return selected.isFile() ? selected : new File(root, "texts/en_us.yml");
    }
}
