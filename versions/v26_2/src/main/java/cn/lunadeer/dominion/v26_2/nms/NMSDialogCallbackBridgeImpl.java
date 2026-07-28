package cn.lunadeer.dominion.v26_2.nms;

import cn.lunadeer.dominion.nms.NMSDialogCallbackBridge;
import org.bukkit.entity.Player;

/**
 * Dialog callback bridge for Minecraft 26.2.
 *
 * <p>26.2 keeps the 26.1 custom-click packet ABI, so the NMS-bound bridge is
 * shared with the v26 module.</p>
 */
public final class NMSDialogCallbackBridgeImpl implements NMSDialogCallbackBridge {
    private final NMSDialogCallbackBridge delegate = createDelegate();

    @Override
    public boolean install(Player player) {
        return delegate.install(player);
    }

    @Override
    public void uninstall(Player player) {
        delegate.uninstall(player);
    }

    @Override
    public boolean isInstalled(Player player) {
        return delegate.isInstalled(player);
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    private static NMSDialogCallbackBridge createDelegate() {
        try {
            Class<?> type = Class.forName("cn.lunadeer.dominion.v26.nms.NMSDialogCallbackBridgeImpl");
            return (NMSDialogCallbackBridge) type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to load the v26 dialog callback bridge", exception);
        }
    }
}
