package cn.lunadeer.dominion.utils.dialogui;

import org.bukkit.entity.Player;

public final class DialogNavigator {
    private final DialogMenuUi ui;

    public DialogNavigator(DialogMenuUi ui) {
        this.ui = ui;
    }

    public void push(Player player, DialogRoute route) { ui.navigate(player, session -> session.push(route)); }
    public void replace(Player player, DialogRoute route) { ui.navigate(player, session -> session.replace(route)); }
    public void back(Player player) { ui.navigate(player, session -> { if (!session.back()) session.home(); }); }
    public void home(Player player) { ui.navigate(player, DialogMenuSession::home); }
    public void refresh(Player player) { ui.navigate(player, DialogMenuSession::touch); }
}
