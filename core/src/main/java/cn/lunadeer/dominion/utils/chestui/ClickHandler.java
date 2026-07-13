package cn.lunadeer.dominion.utils.chestui;

import org.bukkit.event.inventory.ClickType;

@FunctionalInterface
public interface ClickHandler {
    void handle(ClickType clickType);
}
