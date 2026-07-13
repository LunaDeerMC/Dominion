package cn.lunadeer.dominion.utils.chestui;

import org.bukkit.entity.Player;

public final class MenuNavigator {
    private final ChestUiManager manager;

    MenuNavigator(ChestUiManager manager) { this.manager = manager; }
    public void push(Player player, MenuRoute route) { manager.navigate(player, session -> session.push(route)); }
    public void replace(Player player, MenuRoute route) { manager.navigate(player, session -> session.replace(route)); }
    public void back(Player player) { manager.navigate(player, session -> { if (!session.back()) session.home(); }); }
    public void home(Player player) { manager.navigate(player, MenuSession::home); }
    public void refresh(Player player) { manager.navigate(player, MenuSession::touch); }
}
