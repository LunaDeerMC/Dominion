package cn.lunadeer.dominion.configuration;

import cn.lunadeer.dominion.api.dtos.flag.EnvFlagGroup;
import cn.lunadeer.dominion.api.dtos.flag.FlagGroups;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlagGroupLanguageTest {

    @AfterEach
    void restoreDefaults() {
        FlagGroups.replaceConfiguredGroups(
                FlagGroups.defaultEnvironmentGroups(),
                FlagGroups.defaultPrivilegeGroups()
        );
    }

    @Test
    void languageTextOverridesRuntimeDefaults() {
        EnvFlagGroup group = new EnvFlagGroup("weather", "Weather", "Default", Material.SUNFLOWER);
        FlagGroups.replaceConfiguredGroups(List.of(group), List.of());
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set(group.getDisplayNameKey(), "天气");
        yaml.set(group.getDescriptionKey(), "控制天气。");

        assertFalse(Language.loadFlagGroupTexts(yaml));
        assertEquals("天气", group.getDisplayName());
        assertEquals("控制天气。", group.getDescription());
    }

    @Test
    void missingLanguageTextIsAddedFromApiDefaults() {
        EnvFlagGroup group = new EnvFlagGroup("weather", "Weather", "Controls weather.", Material.SUNFLOWER);
        FlagGroups.replaceConfiguredGroups(List.of(group), List.of());
        YamlConfiguration yaml = new YamlConfiguration();

        assertTrue(Language.loadFlagGroupTexts(yaml));
        assertEquals("Weather", yaml.getString(group.getDisplayNameKey()));
        assertEquals("Controls weather.", yaml.getString(group.getDescriptionKey()));
    }
}
