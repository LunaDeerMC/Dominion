package cn.lunadeer.dominion.uis.dialog;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogNativeDesignTest {
    @Test
    void dominionMenusUseNativeDialogSemanticsOnly() throws Exception {
        Path root = Path.of("src/main/java/cn/lunadeer/dominion/uis/dialog");
        String sources;
        try (var files = Files.walk(root)) {
            sources = files.filter(path -> path.toString().endsWith(".java"))
                    .map(this::read)
                    .reduce("", String::concat);
        }
        for (String forbidden : new String[]{
                "WAIT_FOR_RESPONSE",
                "DialogClick",
                "DialogViewBuilder",
                "DialogPageConfig",
                "utils.chestui",
                "uis.chest"
        }) {
            assertFalse(sources.contains(forbidden), "legacy dialog dependency: " + forbidden);
        }
        assertTrue(sources.contains("DialogSpec.AfterAction.CLOSE"));
        assertTrue(sources.contains("DialogSpec.AfterAction.NONE"));
        assertTrue(sources.contains("keepOpenAfterAction"));
        assertTrue(sources.contains("page.backExit("));
        assertTrue(sources.contains("exitAction = button(\"back\""));
        assertFalse(sources.contains("private void spacer()"),
                "list grids must not use the old width-expanding spacer hack");
        assertTrue(sources.contains("append(Component.text(\"  ›\""));
        assertTrue(sources.contains("DominionDialogPage completeListRow()"));
        assertTrue(sources.contains("listActionCount % 2 != 0"));
        assertTrue(sources.contains("DialogSpec buildList()"));
        assertFalse(sources.contains("information("),
                "list labels must not be rendered as no-op buttons");
        assertTrue(sources.contains("BooleanInput"));
        assertTrue(sources.contains("SingleOptionInput"));
    }

    @Test
    void everyDialogLanguageContainsNativeLayoutText() {
        for (String code : new String[]{"en_us", "zh_cn", "zh_tw", "jp_jp"}) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                    new File("../languages/dialog-ui/texts/" + code + ".yml"));
            assertEquals(2, yaml.getInt("schema-version"), code);
            for (String key : new String[]{
                    "dialog.continue-in-background",
                    "input.resize-direction",
                    "input.resize-mode",
                    "input.resize-amount",
                    "input.select-title",
                    "labels.manage-dominion",
                    "labels.teleport-dominion",
                    "labels.edit-flag-group",
                    "labels.manage",
                    "labels.select",
                    "labels.teleport",
                    "descriptions.local-dominion-entry",
                    "descriptions.remote-dominion-entry",
                    "descriptions.flag-group",
                    "buttons.back.name",
                    "buttons.close.name",
                    "buttons.clear.name",
                    "buttons.refresh.name",
                    "buttons.save.name",
                    "buttons.apply.name"
            }) {
                assertTrue(yaml.isString(key), code + ':' + key);
            }
        }
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
