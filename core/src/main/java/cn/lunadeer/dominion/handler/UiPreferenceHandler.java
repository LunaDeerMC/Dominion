package cn.lunadeer.dominion.handler;

import cn.lunadeer.dominion.uis.DominionUi;
import cn.lunadeer.dominion.utils.UiMode;
import org.bukkit.entity.Player;

/**
 * Keeps command parsing independent from the UI implementation package.
 */
public final class UiPreferenceHandler {
    private UiPreferenceHandler() {
    }

    public static void select(Player player, String requestedMode) {
        DominionUi.setPreferenceAndOpen(player, UiMode.parse(requestedMode, UiMode.DEFAULT));
    }
}
