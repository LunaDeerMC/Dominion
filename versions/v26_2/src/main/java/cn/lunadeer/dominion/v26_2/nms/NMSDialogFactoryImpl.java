package cn.lunadeer.dominion.v26_2.nms;

import cn.lunadeer.dominion.nms.NMSDialogFactory;
import cn.lunadeer.dominion.utils.dialogui.DialogEncodingResult;
import cn.lunadeer.dominion.utils.dialogui.DialogSessionContext;
import cn.lunadeer.dominion.utils.dialogui.DialogSpec;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Dialog factory for Minecraft 26.2.
 *
 * <p>The dialog NMS records and packets remain compatible with the v26
 * implementation; this version entry point keeps runtime dispatch explicit.</p>
 */
public final class NMSDialogFactoryImpl implements NMSDialogFactory {
    private final NMSDialogFactory delegate = createDelegate();

    @Override
    public boolean isSupported() {
        return Bukkit.getMinecraftVersion().startsWith("26.2");
    }

    @Override
    public DialogEncodingResult validate(DialogSpec dialog) {
        if (!isSupported()) return DialogEncodingResult.unsupported("Dialog backend requires Minecraft 26.2");
        return delegate.validate(dialog);
    }

    @Override
    public DialogEncodingResult show(Player player, DialogSpec dialog, DialogSessionContext context) {
        if (!isSupported()) return DialogEncodingResult.unsupported("Dialog backend requires Minecraft 26.2");
        return delegate.show(player, dialog, context);
    }

    @Override
    public void close(Player player) {
        delegate.close(player);
    }

    private static NMSDialogFactory createDelegate() {
        try {
            Class<?> type = Class.forName("cn.lunadeer.dominion.v26.nms.NMSDialogFactoryImpl");
            return (NMSDialogFactory) type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to load the v26 dialog factory", exception);
        }
    }
}
