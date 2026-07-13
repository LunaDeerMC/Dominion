package cn.lunadeer.dominion.utils.chestui;

import org.bukkit.inventory.Inventory;
import java.util.Map;

public record MenuView(Inventory inventory, Map<Integer, ClickHandler> handlers) {}
