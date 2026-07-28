package cn.lunadeer.dominion.handler;

import cn.lunadeer.dominion.managers.MultiServerManager;

/**
 * Version- and renderer-independent data lookups used by graphical menus.
 */
public final class UiDataHandler {
    private UiDataHandler() {
    }

    public static String serverName(int serverId) {
        try {
            return MultiServerManager.instance.getServerName(serverId);
        } catch (Exception exception) {
            return String.valueOf(serverId);
        }
    }
}
