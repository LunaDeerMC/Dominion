package cn.lunadeer.dominion.utils.chestui.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class MenuDefinitionTest {
    @Test
    void parsesGridSymbolsAndRepeatedSlots() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("""
                menu:
                  required-elements: [content]
                  layout: [\"A**      \", \"         \"]
                  symbols: { A: back, '*': content }
                  items:
                    content: { material: PLAYER_HEAD, amount: 2, glow: true, item-flags: [HIDE_ATTRIBUTES], head-source: viewer }
                """);
        MenuDefinition menu = MenuDefinition.read(yaml.getConfigurationSection("menu"));
        assertEquals(2, menu.rows());
        assertEquals(0, menu.firstSlot("back"));
        assertEquals(java.util.List.of(1, 2), menu.slots("content"));
        assertEquals(2, menu.appearance("content").amount());
        assertEquals("viewer", menu.appearance("content").headSource());
    }

    @Test
    void rejectsWrongWidthUnmappedAndMissingRequiredElements() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> read("layout: [\"short\"]\nsymbols: { s: content }"));
        assertThrows(IllegalArgumentException.class, () -> read("layout: [\"?        \"]\nsymbols: { A: content }"));
        assertThrows(IllegalArgumentException.class, () -> read("required-elements: [content]\nlayout: [\"A        \"]\nsymbols: { A: back }"));
    }

    private static MenuDefinition read(String source) throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("menu:\n" + source.indent(2));
        return MenuDefinition.read(yaml.getConfigurationSection("menu"));
    }

    @Test
    void bundledMenusAreValidAndDefaultListFooterMatchesContract() {
        YamlConfiguration layout = YamlConfiguration.loadConfiguration(
                new File("../languages/chest-ui/layout.yml"));
        YamlConfiguration text = YamlConfiguration.loadConfiguration(
                new File("../languages/chest-ui/texts/en_us.yml"));
        var menus = layout.getConfigurationSection("menus");
        assertNotNull(menus);
        for (String id : menus.getKeys(false)) {
            MenuDefinition menu = MenuDefinition.read(menus.getConfigurationSection(id));
            assertFalse(menu.elements().isEmpty(), id);
            for (String element : menu.elements()) {
                assertTrue(text.contains("menus." + id + ".items." + element + ".name"), id + ":" + element);
            }
        }
        for (String id : java.util.List.of("dominion-list", "flag-list", "member-list", "group-list",
                "template-list", "picker-list", "title-list")) {
            MenuDefinition menu = MenuDefinition.read(menus.getConfigurationSection(id));
            assertEquals(45, menu.firstSlot("back"), id);
            assertEquals(48, menu.firstSlot("previous"), id);
            assertEquals(49, menu.firstSlot("status"), id);
            assertEquals(50, menu.firstSlot("next"), id);
            assertEquals(53, menu.firstSlot("close"), id);
        }
    }
}
