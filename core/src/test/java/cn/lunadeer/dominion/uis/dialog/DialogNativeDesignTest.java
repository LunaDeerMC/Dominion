package cn.lunadeer.dominion.uis.dialog;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        assertTrue(sources.contains("STYLE.compactItemWidth(), group.getIcon()"));
        assertTrue(sources.contains("flag.getIcon()"));
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

    @Test
    void dialogIconLayoutUsesVanillaAtlasSpritesByDefault() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                new File("../languages/dialog-ui/layout.yml"));
        assertEquals(1, yaml.getInt("schema-version"));
        assertEquals("minecraft:items/item/paper", yaml.getString("default-icon"));
        for (String key : new String[]{
                "buttons.back.icon", "buttons.close.icon", "buttons.search.icon",
                "buttons.primary.icon", "buttons.member.icon", "buttons.flag-group.icon",
                "buttons.enabled.icon", "buttons.disabled.icon",
                "menus.main.items.dominions.icon",
                "menus.dominion-list.items.remote-content.icon"
        }) {
            assertTrue(yaml.isString(key), key);
            assertTrue(yaml.getString(key).isBlank()
                            || yaml.getString(key).startsWith("minecraft:"),
                    key);
        }
    }

    @Test
    void emptyConfiguredIconDoesNotFallBackToAnotherIcon() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("default-icon", "minecraft:items/item/paper");
        yaml.set("buttons.default.icon", "minecraft:items/item/paper");
        yaml.set("buttons.empty.icon", "");

        assertEquals("minecraft:items/item/paper",
                DialogUiText.resolveIcon(yaml, null, "missing"));
        assertEquals("minecraft:items/item/paper",
                DialogUiText.resolveIcon(yaml, null, "default"));
        assertNull(DialogUiText.resolveIcon(yaml, null, "empty"));
    }

    @Test
    void playerRelatedListsAttachNativePlayerHeadIcons() {
        String memberList = read(Path.of(
                "src/main/java/cn/lunadeer/dominion/uis/dialog/pages/dominion/dashboard/permissions/members/MemberListPage.java"));
        assertTrue(memberList.contains("playerHead(member.getPlayer())"));

        String pickerList = read(Path.of(
                "src/main/java/cn/lunadeer/dominion/uis/dialog/pages/picker/PickerListPage.java"));
        assertTrue(pickerList.contains("entry.playerHead()"));
        assertTrue(pickerList.contains("playerHead(memberPlayer)"));
        assertTrue(pickerList.contains("playerHead(candidate)"));
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
