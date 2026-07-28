package cn.lunadeer.dominion.configuration;

import cn.lunadeer.dominion.api.dtos.flag.Flag;
import cn.lunadeer.dominion.api.dtos.flag.FlagGroup;
import cn.lunadeer.dominion.api.dtos.flag.FlagGroups;
import cn.lunadeer.dominion.api.dtos.flag.Flags;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FlagLanguageCoverageTest {

    @Test
    void everyRuntimeLanguageContainsEveryFlag() {
        for (String code : new String[]{"en_us", "zh_cn", "zh_tw", "jp_jp"}) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                    new File("../languages/" + code + ".yml"));
            for (Flag flag : Flags.getAllFlags()) {
                assertTrue(yaml.isString(flag.getDisplayNameKey()),
                        code + ":" + flag.getDisplayNameKey());
                assertTrue(yaml.isString(flag.getDescriptionKey()),
                        code + ":" + flag.getDescriptionKey());
            }
            for (FlagGroup<?> group : defaultFlagGroups()) {
                assertTrue(yaml.isString(group.getDisplayNameKey()),
                        code + ":" + group.getDisplayNameKey());
                assertTrue(yaml.isString(group.getDescriptionKey()),
                        code + ":" + group.getDescriptionKey());
            }
        }
    }

    @Test
    void everyChestUiLanguageContainsGroupInteractionText() {
        for (String code : new String[]{"en_us", "zh_cn", "zh_tw", "jp_jp"}) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                    new File("../languages/chest-ui/texts/" + code + ".yml"));
            for (String key : new String[]{
                    "titles.flag-group",
                    "labels.ungrouped",
                    "labels.ungrouped-description",
                    "common.all-enabled",
                    "common.all-disabled",
                    "common.mixed",
                    "menus.flag-group-list.items.content.name"
            }) {
                assertTrue(yaml.contains(key), code + ":" + key);
            }
        }
    }

    private static java.util.List<FlagGroup<?>> defaultFlagGroups() {
        java.util.List<FlagGroup<?>> groups = new java.util.ArrayList<>();
        groups.addAll(FlagGroups.defaultEnvironmentGroups());
        groups.addAll(FlagGroups.defaultPrivilegeGroups());
        return groups;
    }
}
