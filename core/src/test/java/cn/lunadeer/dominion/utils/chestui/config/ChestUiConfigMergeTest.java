package cn.lunadeer.dominion.utils.chestui.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChestUiConfigMergeTest {
    @Test
    void addsMissingKeysAndKeepsExistingAndDeprecatedKeys() throws Exception {
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.loadFromString("menu:\n  title: Default\n  item:\n    name: Added\n");
        YamlConfiguration user = new YamlConfiguration();
        user.loadFromString("menu:\n  title: Custom\n  deprecated: Keep me\n");

        ChestUiConfig.mergeMissing(user, defaults);

        assertEquals("Custom", user.getString("menu.title"));
        assertEquals("Added", user.getString("menu.item.name"));
        assertEquals("Keep me", user.getString("menu.deprecated"));
    }

    @Test
    void missingLanguageFallsBackToEnglish(@TempDir Path root) throws Exception {
        Files.createDirectories(root.resolve("texts"));
        Files.createFile(root.resolve("texts/en_us.yml"));
        assertEquals(root.resolve("texts/en_us.yml").toFile(),
                ChestUiConfig.selectTextFile(root.toFile(), "zh_cn"));
        Files.createFile(root.resolve("texts/custom.yml"));
        assertEquals(root.resolve("texts/custom.yml").toFile(),
                ChestUiConfig.selectTextFile(root.toFile(), "CUSTOM"));
    }
}
