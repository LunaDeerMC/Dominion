package cn.lunadeer.dominion.nms;

import org.bukkit.entity.Player;

/**
 * Receives the vanilla custom-click packet without using commands.
 */
public interface NMSDialogCallbackBridge {
    boolean install(Player player);

    void uninstall(Player player);

    boolean isInstalled(Player player);

    void shutdown();
}
