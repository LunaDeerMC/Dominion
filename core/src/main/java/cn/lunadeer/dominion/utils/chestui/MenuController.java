package cn.lunadeer.dominion.utils.chestui;

import org.bukkit.entity.Player;

/** Renders application-owned routes without coupling the chest framework to business pages. */
@FunctionalInterface
public interface MenuController {
    MenuView render(Player player, MenuSession session) throws Exception;
}
