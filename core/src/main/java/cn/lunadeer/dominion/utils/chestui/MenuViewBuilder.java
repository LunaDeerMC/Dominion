package cn.lunadeer.dominion.utils.chestui;

import cn.lunadeer.dominion.utils.chestui.config.ChestUiConfig;
import cn.lunadeer.dominion.utils.chestui.config.ItemAppearance;
import cn.lunadeer.dominion.utils.chestui.config.MenuDefinition;
import cn.lunadeer.dominion.utils.LegacyToMiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.*;

public final class MenuViewBuilder {
    private final Player player;
    private final ChestUiConfig config;
    private final String menuId;
    private final MenuDefinition definition;
    private final Inventory inventory;
    private final Map<Integer, ClickHandler> handlers = new HashMap<>();

    public MenuViewBuilder(Player player, MenuSession session, ChestUiConfig config, String menuId,
                    Map<String, ?> titlePlaceholders) {
        this.player = player;
        this.config = config;
        this.menuId = menuId;
        definition = config.menu(menuId);
        String title = TextRenderer.render(player, config.text("menus." + menuId + ".title"), titlePlaceholders);
        MenuHolder holder = new MenuHolder(player.getUniqueId(), session.revision());
        inventory = Bukkit.createInventory(holder, definition.rows() * 9, LegacyToMiniMessage.parse(title));
        holder.inventory(inventory);
        fillDecoration();
    }

    public List<Integer> slots(String element) { return definition.slots(element); }

    public ItemAppearance appearance(String element) { return definition.appearance(element); }

    public void item(String element, Map<String, ?> placeholders, UUID headOwner, ClickHandler handler) {
        List<Integer> slots = definition.slots(element);
        if (slots.isEmpty()) return;
        String path = "menus." + menuId + ".items." + element;
        for (int slot : slots) {
            inventory.setItem(slot, ItemFactory.create(player, definition.appearance(element),
                    config.text(path + ".name"), config.textList(path + ".lore"), placeholders, headOwner));
            if (handler != null) handlers.put(slot, handler);
        }
    }

    public void itemAt(int slot, String element, String textElement, Map<String, ?> placeholders,
                UUID headOwner, ClickHandler handler) {
        itemAt(slot, element, textElement, placeholders, headOwner, null, handler);
    }

    public void itemAt(int slot, String element, String textElement, Map<String, ?> placeholders,
                UUID headOwner, ItemAppearance appearance,
                ClickHandler handler) {
        if (slot < 0 || slot >= inventory.getSize()) return;
        String path = "menus." + menuId + ".items." + textElement;
        inventory.setItem(slot, ItemFactory.create(player, appearance == null ? definition.appearance(element) : appearance,
                config.text(path + ".name"), config.textList(path + ".lore"), placeholders, headOwner));
        if (handler != null) handlers.put(slot, handler);
    }

    public MenuView build() { return new MenuView(inventory, Map.copyOf(handlers)); }

    private void fillDecoration() {
        String path = "menus." + menuId + ".items.filler";
        for (int slot : definition.slots("filler")) {
            inventory.setItem(slot, ItemFactory.create(player, definition.appearance("filler"),
                    config.text(path + ".name"), config.textList(path + ".lore"), Map.of(), null));
        }
    }
}
