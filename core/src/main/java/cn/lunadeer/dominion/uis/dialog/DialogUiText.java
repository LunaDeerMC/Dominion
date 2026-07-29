package cn.lunadeer.dominion.uis.dialog;

import cn.lunadeer.dominion.configuration.Language.LanguageCode;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.List;

/**
 * Independent text bundle for the version-independent Dialog UI.
 */
public final class DialogUiText {
    private static final String ROOT = "languages/dialog-ui/texts/";
    private static final int CURRENT_SCHEMA = 2;
    private final JavaPlugin plugin;
    private volatile YamlConfiguration text = new YamlConfiguration();

    public DialogUiText(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public synchronized void load(String language) throws IOException {
        for (LanguageCode code : LanguageCode.values()) {
            String resource = ROOT + code.name() + ".yml";
            File file = new File(plugin.getDataFolder(), resource);
            if (!file.exists()) plugin.saveResource(resource, false);
        }

        File selected = new File(plugin.getDataFolder(), ROOT + language.toLowerCase(Locale.ROOT) + ".yml");
        if (!selected.isFile()) selected = new File(plugin.getDataFolder(), ROOT + "en_us.yml");
        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(selected);
        String selectedLanguage = selected.getName().substring(0, selected.getName().length() - 4);
        YamlConfiguration selectedDefaults = loadResource(ROOT + selectedLanguage + ".yml");
        if (loaded.getInt("schema-version", 1) < CURRENT_SCHEMA) {
            copyKey(loaded, selectedDefaults, "menus.main.items.ui.name");
            copyKey(loaded, selectedDefaults, "menus.main.items.ui.lore");
        }
        mergeMissing(loaded, selectedDefaults);
        mergeMissing(loaded, loadResource(ROOT + "en_us.yml"));
        loaded.set("schema-version", CURRENT_SCHEMA);
        loaded.save(selected);
        text = loaded;
    }

    public String text(String key) {
        return text.getString(key, key);
    }

    public boolean contains(String key) {
        return text.contains(key);
    }

    public List<String> textList(String key) {
        Object value = text.get(key);
        if (value instanceof List<?>) return text.getStringList(key);
        String single = text.getString(key);
        return single == null ? List.of() : List.of(single);
    }

    private static void mergeMissing(YamlConfiguration target, YamlConfiguration defaults) {
        for (String key : defaults.getKeys(true)) {
            if (!target.contains(key)) target.set(key, defaults.get(key));
        }
    }

    private static void copyKey(YamlConfiguration target, YamlConfiguration source, String key) {
        if (source.contains(key)) target.set(key, source.get(key));
    }

    private YamlConfiguration loadResource(String path) throws IOException {
        InputStream stream = plugin.getResource(path);
        if (stream == null) throw new FileNotFoundException(path);
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        }
    }
}
